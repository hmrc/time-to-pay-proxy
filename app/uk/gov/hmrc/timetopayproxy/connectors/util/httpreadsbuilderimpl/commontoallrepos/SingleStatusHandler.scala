/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilderimpl.commontoallrepos

import play.api.libs.json.{ JsError, JsSuccess, Reads }

import scala.util.{ Failure, Success, Try }

/** For any status, takes a request and produces a result. */
/* ℹ️ Note about updating this file: ℹ️
 * This file must be kept consistent with every copy in the other debt-transformation repos.
 * The most complex features are required by time-to-pay, so that is the best place to test any refactoring.
 */
private[util] final case class SingleStatusHandler[+ServError, +Result](
  processor: (ResponseContext, Option[LoggingContext[ServError]]) => Either[HttpReadsBuilderError[ServError], Result]
)

private[util] object SingleStatusHandler {
  def fallbackErrorForUnknownStatusCode[ServError](
    sourceClass: Class[_]
  ): SingleStatusHandler[ServError, Nothing] =
    SingleStatusHandler { (responseContext, maybeLoggingContext) =>
      Left(
        HttpReadsBuilderError.Impl.GeneralErrorForUnsuccessfulStatusCode.createAndLogForUnexpectedStatusCode(
          sourceClass = sourceClass,
          responseContext,
          maybeLoggingContext
        )
      )
    }

  def assumingNoBody[ServError, Result](
    sourceClass: Class[_],
    value: Either[ServError, Result]
  ): SingleStatusHandler[ServError, Result] =
    SingleStatusHandler[ServError, Result] { (responseContext, maybeLoggingContext) =>
      if (responseContext.response.body.isEmpty) {
        if (value.isLeft) {
          maybeLoggingContext.foreach(_.logWarningAboutValidUnsuccessfulResponse(responseContext))
        }
        value.left.map((connectorError: ServError) =>
          HttpReadsBuilderError.Impl.PassthroughServiceError(error = connectorError)
        )
      } else {

        value match {
          case Right(_) =>
            // If the status code was mapped to a "success", we need to return a special error so we may avoid retries.
            val error: HttpReadsBuilderError.Impl.BodyNotEmptyErrorForSuccess =
              HttpReadsBuilderError.Impl.BodyNotEmptyErrorForSuccess(sourceClass = sourceClass, responseContext)

            val trimmedBodyLength: Int = responseContext.response.body.trim.length

            maybeLoggingContext.foreach(
              _.logError(
                responseContext,
                simpleMessage =
                  s"Body of successful upstream HTTP response is not empty. Trimmed body length: $trimmedBodyLength.",
                error
              )
            )
            Left(error)
          case Left(_) =>
            Left(
              HttpReadsBuilderError.Impl.GeneralErrorForUnsuccessfulStatusCode
                .createAndLogForNonEmptyBodyInErrorResponse(
                  sourceClass = sourceClass,
                  responseContext,
                  maybeLoggingContext
                )
            )
        }
      }
    }

  def assumingJsonSuccess[ServError, Result, ReadableResult: Reads](
    sourceClass: Class[_],
    transform: ReadableResult => Result
  ): SingleStatusHandler[ServError, Result] =
    SingleStatusHandler { (responseContext, maybeLoggingContext) =>
      Try(responseContext.response.json).map(_.validate[ReadableResult]) match {
        case Success(JsSuccess(deserialisedBody, _)) => Right(transform(deserialisedBody))
        case Success(JsError(errs)) =>
          val error: HttpReadsBuilderError.Impl.CentrallyImmplementedVariant =
            HttpReadsBuilderError.Impl.JsonNotValidErrorForSuccess(
              sourceClass = sourceClass,
              responseContext,
              errs = errs.toSeq.map(e => (e._1, e._2.toSeq))
            )
          maybeLoggingContext.foreach(
            _.logError(
              responseContext,
              simpleMessage = "JSON structure is not valid in received successful HTTP response.",
              error
            )
          )
          Left(error)
        case Failure(ex) =>
          val error = HttpReadsBuilderError.Impl.NotJsonErrorForSuccess(sourceClass = sourceClass, responseContext, ex)

          maybeLoggingContext.foreach(
            _.logError(
              responseContext,
              simpleMessage = "HTTP body is not JSON in received successful HTTP response.",
              error
            )
          )
          Left(error)
      }
    }

  def assumingJsonError[Result, ReadableError: Reads, ServError](
    sourceClass: Class[_],
    transform: ReadableError => ServError
  ): SingleStatusHandler[ServError, Result] =
    SingleStatusHandler { (responseContext, maybeLoggingContext: Option[LoggingContext[ServError]]) =>
      Try(responseContext.response.json).map(_.validate[ReadableError]) match {
        case Success(JsSuccess(deserialisedBody, _)) =>
          maybeLoggingContext.foreach(_.logWarningAboutValidUnsuccessfulResponse(responseContext))
          Left(HttpReadsBuilderError.Impl.PassthroughServiceError(transform(deserialisedBody)))
        case Success(JsError(errs)) =>
          Left(
            HttpReadsBuilderError.Impl.GeneralErrorForUnsuccessfulStatusCode
              .createAndLogForInvalidJsonStructureInErrorResponse(
                sourceClass = sourceClass,
                responseContext,
                maybeLoggingContext,
                errs.toSeq.map(e => (e._1, e._2.toSeq))
              )
          )
        case Failure(_) =>
          Left(
            HttpReadsBuilderError.Impl.GeneralErrorForUnsuccessfulStatusCode
              .createAndLogForNonJsonErrorInErrorResponse(
                sourceClass = sourceClass,
                responseContext,
                maybeLoggingContext
              )
          )
      }
    }
}
