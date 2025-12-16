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

package uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder.implcommontoallrepos

import play.api.libs.json.{ JsError, JsSuccess, Reads }

import scala.util.{ Failure, Success, Try }

/** For any status, takes a request and produces a result. */
/* ℹ️ Note about updating this file: ℹ️
 * This file must be kept consistent with every copy in the other debt-transformation repos.
 * The most complex features are required by time-to-pay, so that is the best place to test any refactoring.
 */
private[httpreadsbuilder] final case class SingleStatusHandler[+ServiceError, +Result](
  processor: (
    ResponseContext,
    Option[LoggingContext[ServiceError]]
  ) => Either[HttpReadsBuilderError[ServiceError], Result]
)

private[httpreadsbuilder] object SingleStatusHandler {
  def fallbackErrorForUnknownStatusCode[ServiceError](
    sourceClass: Class[_]
  ): SingleStatusHandler[ServiceError, Nothing] =
    SingleStatusHandler { (responseContext, maybeLoggingContext) =>
      val error = HttpReadsBuilderError.UnexpectedStatusCode(sourceClass = sourceClass, responseContext)

      maybeLoggingContext.foreach(_.logError(responseContext, error))
      Left(error)
    }

  def assumingNoBody[ServiceError, Result](
    sourceClass: Class[_],
    value: Either[ServiceError, Result]
  ): SingleStatusHandler[ServiceError, Result] =
    SingleStatusHandler[ServiceError, Result] { (responseContext, maybeLoggingContext) =>
      if (responseContext.response.body.isEmpty) {
        if (value.isLeft) {
          maybeLoggingContext.foreach(_.logWarningAboutValidUnsuccessfulResponse(responseContext))
        }
        value.left.map((connectorError: ServiceError) =>
          HttpReadsBuilderError.PassthroughServiceError(error = connectorError)
        )
      } else {

        value match {
          case Right(_) =>
            // If the status code was mapped to a "success", we need to return a special error so we may avoid retries.
            val error = HttpReadsBuilderError.ResponseBodyNotEmpty(
              sourceClass = sourceClass,
              responseContext,
              HttpReadsBuilderError.OriginallyMeantToBeRight
            )

            maybeLoggingContext.foreach(_.logError(responseContext, error))
            Left(error)
          case Left(_) =>
            val error = HttpReadsBuilderError.ResponseBodyNotEmpty(
              sourceClass = sourceClass,
              responseContext,
              HttpReadsBuilderError.OriginallyMeantToBeLeft
            )

            maybeLoggingContext.foreach(_.logError(responseContext, error))
            Left(error)
        }
      }
    }

  def assumingJsonSuccess[ServiceError, Result, ReadableResult: Reads](
    sourceClass: Class[_],
    transform: ReadableResult => Result
  ): SingleStatusHandler[ServiceError, Result] =
    SingleStatusHandler { (responseContext, maybeLoggingContext) =>
      Try(responseContext.response.json).map(_.validate[ReadableResult]) match {
        case Success(JsSuccess(deserialisedBody, _)) => Right(transform(deserialisedBody))
        case Success(JsError(errs)) =>
          val error = HttpReadsBuilderError.ResponseBodyInvalidJsonStructure(
            sourceClass = sourceClass,
            responseContext,
            HttpReadsBuilderError.OriginallyMeantToBeRight,
            errs = errs.toSeq.map(e => (e._1, e._2.toSeq))
          )

          maybeLoggingContext.foreach(_.logError(responseContext, error))
          Left(error)
        case Failure(ex) =>
          val error = HttpReadsBuilderError.ResponseBodyNotJson(
            sourceClass = sourceClass,
            responseContext,
            HttpReadsBuilderError.OriginallyMeantToBeRight,
            ex
          )

          maybeLoggingContext.foreach(_.logError(responseContext, error))
          Left(error)
      }
    }

  def assumingJsonError[Result, ReadableError: Reads, ServiceError](
    sourceClass: Class[_],
    transform: ReadableError => ServiceError
  ): SingleStatusHandler[ServiceError, Result] =
    SingleStatusHandler { (responseContext, maybeLoggingContext: Option[LoggingContext[ServiceError]]) =>
      Try(responseContext.response.json).map(_.validate[ReadableError]) match {
        case Success(JsSuccess(deserialisedBody, _)) =>
          maybeLoggingContext.foreach(_.logWarningAboutValidUnsuccessfulResponse(responseContext))
          Left(HttpReadsBuilderError.PassthroughServiceError(transform(deserialisedBody)))
        case Success(JsError(errs)) =>
          val error = HttpReadsBuilderError.ResponseBodyInvalidJsonStructure(
            sourceClass = sourceClass,
            responseContext,
            HttpReadsBuilderError.OriginallyMeantToBeLeft,
            errs.toSeq.map { e => (e._1, e._2.toSeq) }
          )

          maybeLoggingContext.foreach(_.logError(responseContext, error))
          Left(error)
        case Failure(throwable) =>
          val error = HttpReadsBuilderError.ResponseBodyNotJson(
            sourceClass = sourceClass,
            responseContext,
            HttpReadsBuilderError.OriginallyMeantToBeLeft,
            sensitiveException = throwable
          )

          maybeLoggingContext.foreach(_.logError(responseContext, error))
          Left(error)
      }
    }
}
