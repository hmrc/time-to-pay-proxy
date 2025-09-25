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

package uk.gov.hmrc.timetopayproxy.connectors.util

import play.api.http.Status
import play.api.libs.json.{ JsError, JsSuccess, Reads }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, HttpResponse }
import uk.gov.hmrc.timetopayproxy.connectors.util.HttpReadsWithLoggingBuilder.ResponseContext
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger
import uk.gov.hmrc.timetopayproxy.models.error.ConnectorError

import scala.util.{ Failure, Success, Try }

final class HttpReadsWithLoggingBuilder[E >: ConnectorError, Result] private (
  matcher: PartialFunction[Int, (ResponseContext, RequestAwareLogger, HeaderCarrier) => Either[E, Result]]
) {
  def orSuccess[ReadableResult <: Result: Reads](incomingStatus: Int): HttpReadsWithLoggingBuilder[E, Result] =
    withMatcher(
      matcher.orElse { case `incomingStatus` =>
        (responseContext: ResponseContext, logger: RequestAwareLogger, hc: HeaderCarrier) =>
          Try(responseContext.response.json).map(_.validate[ReadableResult]) match {
            case Success(JsSuccess(deserialisedBody, _)) => Right(deserialisedBody)
            case Success(JsError(_)) =>
              createConnectorError(
                responseContext,
                newStatus = 503,
                simpleMessage = "JSON structure is not valid in received successful HTTP response.",
                logger
              )(hc)
            case Failure(_) =>
              createConnectorError(
                responseContext,
                newStatus = 503,
                simpleMessage = "HTTP body is not JSON in received successful HTTP response.",
                logger
              )(hc)
          }
      }
    )

  def orError[ReadableError <: E: Reads](incomingStatus: Int): HttpReadsWithLoggingBuilder[E, Result] =
    orErrorTransformed[ReadableError](incomingStatus = incomingStatus, transform = identity)

  def orErrorTransformed[ReadableError: Reads](
    incomingStatus: Int,
    transform: ReadableError => E
  ): HttpReadsWithLoggingBuilder[E, Result] =
    withMatcher(
      matcher.orElse { case `incomingStatus` =>
        (responseContext: ResponseContext, logger: RequestAwareLogger, hc: HeaderCarrier) =>
          Try(responseContext.response.json).map(_.validate[ReadableError]) match {
            case Success(JsSuccess(deserialisedBody, _)) =>
              logWarningAboutValidUnsuccessfulResponse(responseContext, logger)(hc)
              Left(transform(deserialisedBody))
            case Success(JsError(_)) =>
              createConnectorError(
                responseContext,
                newStatus = 503,
                simpleMessage = "JSON structure is not valid in received error HTTP response.",
                logger
              )(hc)
            case Failure(_) =>
              createConnectorError(
                responseContext,
                newStatus = 503,
                simpleMessage = "HTTP body is not JSON in received error HTTP response.",
                logger
              )(hc)
          }
      }
    )

  def httpReads(logger: RequestAwareLogger)(implicit hc: HeaderCarrier): HttpReads[Either[E, Result]] = {
    (method: String, url: String, response: HttpResponse) =>
      val responseContext = ResponseContext(method = method, url = url, response)
      matcher.lift(response.status) match {
        case Some(handler) =>
          handler(responseContext, logger, hc)
        case None =>
          createConnectorError(
            responseContext,
            // Unrecognised incoming HTTP statuses cannot be forwarded because:
            //   1. They will likely break our schema by returning undocumented status codes with mismatched JSON.
            //   2. Some status codes don't make sense to be forwarded except in very special situations, e.g. 403, 404.
            newStatus = 503,
            simpleMessage = "HTTP status is unexpected in received HTTP response.",
            logger
          )(hc)
      }
  }

  private def createConnectorError(
    responseContext: ResponseContext,
    newStatus: Int,
    simpleMessage: String,
    logger: RequestAwareLogger
  )(implicit hc: HeaderCarrier): Left[ConnectorError, Nothing] = {
    logger.error(
      s"""$simpleMessage
         |Response status to be returned: $newStatus
         |Request made for received HTTP response: ${responseContext.method} ${responseContext.url}
         |Received HTTP response status: ${responseContext.response.status}
         |${safeToLogResponseBodyDescription(responseContext.response)}""".stripMargin
    )

    Left(ConnectorError(statusCode = newStatus, message = simpleMessage))
  }

  private def logWarningAboutValidUnsuccessfulResponse(
    responseContext: ResponseContext,
    logger: RequestAwareLogger
  )(implicit hc: HeaderCarrier): Unit =
    logger.warn(
      s"""Valid and expected error response was found in received successful HTTP response.
         |Request made for received HTTP response: ${responseContext.method} ${responseContext.url}
         |Received HTTP response status: ${responseContext.response.status}
         |${safeToLogResponseBodyDescription(responseContext.response)}""".stripMargin
    )

  private def safeToLogResponseBodyDescription(response: HttpResponse): String =
    if (Status.isSuccessful(response.status)) {
      s"Received HTTP response body not logged for 2xx statuses."
    } else {
      s"Received HTTP response body: ${response.body}"
    }

  private def withMatcher(
    newMatcher: PartialFunction[Int, (ResponseContext, RequestAwareLogger, HeaderCarrier) => Either[E, Result]]
  ): HttpReadsWithLoggingBuilder[E, Result] =
    new HttpReadsWithLoggingBuilder(newMatcher)
}

object HttpReadsWithLoggingBuilder {

  /** Includes a default handler for unrecognised/unimplemented/invalid status codes, which returns a 503. */
  def empty[E >: ConnectorError, Result]: HttpReadsWithLoggingBuilder[E, Result] =
    new HttpReadsWithLoggingBuilder(PartialFunction.empty)

  private final case class ResponseContext(method: String, url: String, response: HttpResponse)
}
