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
  matcher: PartialFunction[Int, ResponseContext => Either[E, Result]],
  //  The logger is injected so it can be fully tested, because the risk of logging sensitive information here is higher.
  logger: RequestAwareLogger,
  hc: HeaderCarrier
) {
  def orSuccess[ReadableResult <: Result: Reads](incomingStatus: Int): HttpReadsWithLoggingBuilder[E, Result] =
    withMatcher(
      matcher.orElse { case `incomingStatus` =>
        (responseContext: ResponseContext) =>
          Try(responseContext.response.json).map(_.validate[ReadableResult]) match {
            case Success(JsSuccess(deserialisedBody, _)) => Right(deserialisedBody)
            case Success(JsError(_)) =>
              createConnectorError(
                responseContext,
                newStatus = 503,
                simpleMessage = "Couldn't parse body from upstream"
              )
            case Failure(_) =>
              createConnectorError(
                responseContext,
                newStatus = 503,
                simpleMessage = "Couldn't parse body from upstream"
              )
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
        (responseContext: ResponseContext) =>
          Try(responseContext.response.json).map(_.validate[ReadableError]) match {
            case Success(JsSuccess(deserialisedBody, _)) => Left(transform(deserialisedBody))
            case Success(JsError(_)) =>
              createConnectorError(
                responseContext,
                newStatus = 503,
                simpleMessage = "Couldn't parse body from upstream"
              )
            case Failure(_) =>
              createConnectorError(
                responseContext,
                newStatus = 503,
                simpleMessage = "Couldn't parse body from upstream"
              )
          }
      }
    )

  def httpReads: HttpReads[Either[E, Result]] = { (method: String, url: String, response: HttpResponse) =>
    matcher.lift(response.status) match {
      case Some(handler) => handler(ResponseContext(method = method, url = url, response))
      case None =>
        createConnectorError(
          ResponseContext(method = method, url = url, response),
          newStatus = response.status,
          simpleMessage = "Unexpected response from upstream"
        )
    }
  }

  private def createConnectorError(
    responseContext: ResponseContext,
    newStatus: Int,
    simpleMessage: String
  ): Left[ConnectorError, Nothing] = {
    val incomingHttpBodyLine: String =
      if (Status.isSuccessful(responseContext.response.status)) {
        s"Incoming HTTP response body not logged for successful statuses."
      } else {
        s"Incoming HTTP response body: ${responseContext.response.body}"
      }

    val logMessage = if (simpleMessage.endsWith(".")) simpleMessage else s"$simpleMessage."
    val errorMessage = if (simpleMessage.endsWith(".")) simpleMessage.dropRight(1) else simpleMessage

    logger.error(
      s"""$logMessage
         |Response status being returned: $newStatus
         |Request made: ${responseContext.method} ${responseContext.url}
         |Response status received: ${responseContext.response.status}
         |$incomingHttpBodyLine""".stripMargin
    )(hc)

    Left(ConnectorError(newStatus, errorMessage))
  }

  private def withMatcher(
    newMatcher: PartialFunction[Int, ResponseContext => Either[E, Result]]
  ): HttpReadsWithLoggingBuilder[E, Result] =
    new HttpReadsWithLoggingBuilder(newMatcher, logger, hc)
}

object HttpReadsWithLoggingBuilder {
  def apply[E >: ConnectorError, Result](logger: RequestAwareLogger)(implicit
    hc: HeaderCarrier
  ): HttpReadsWithLoggingBuilder[E, Result] =
    new HttpReadsWithLoggingBuilder(PartialFunction.empty, logger, hc)

  private final case class ResponseContext(method: String, url: String, response: HttpResponse)
}
