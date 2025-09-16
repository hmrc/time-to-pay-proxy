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
                simpleMessage = "JSON structure is not valid in successful upstream response.",
                logger
              )(hc)
            case Failure(_) =>
              createConnectorError(
                responseContext,
                newStatus = 503,
                simpleMessage = "Successful upstream response body is not JSON.",
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
            case Success(JsSuccess(deserialisedBody, _)) => Left(transform(deserialisedBody))
            case Success(JsError(_)) =>
              createConnectorError(
                responseContext,
                newStatus = 503,
                simpleMessage = "JSON structure is not valid in error upstream response.",
                logger
              )(hc)
            case Failure(_) =>
              createConnectorError(
                responseContext,
                newStatus = 503,
                simpleMessage = "Error upstream response body is not JSON.",
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
            newStatus = response.status,
            simpleMessage = "Upstream response status is unexpected.",
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
    val incomingHttpBodyLine: String =
      if (Status.isSuccessful(responseContext.response.status)) {
        s"Incoming HTTP response body not logged for successful (2xx) statuses."
      } else {
        s"Incoming HTTP response body: ${responseContext.response.body}"
      }

    logger.error(
      s"""$simpleMessage
         |Response status being returned: $newStatus
         |Request made: ${responseContext.method} ${responseContext.url}
         |Response status received: ${responseContext.response.status}
         |$incomingHttpBodyLine""".stripMargin
    )

    Left(ConnectorError(statusCode = newStatus, message = simpleMessage))
  }

  private def withMatcher(
    newMatcher: PartialFunction[Int, (ResponseContext, RequestAwareLogger, HeaderCarrier) => Either[E, Result]]
  ): HttpReadsWithLoggingBuilder[E, Result] =
    new HttpReadsWithLoggingBuilder(newMatcher)
}

object HttpReadsWithLoggingBuilder {
  def apply[E >: ConnectorError, Result]: HttpReadsWithLoggingBuilder[E, Result] =
    new HttpReadsWithLoggingBuilder(PartialFunction.empty)

  private final case class ResponseContext(method: String, url: String, response: HttpResponse)
}
