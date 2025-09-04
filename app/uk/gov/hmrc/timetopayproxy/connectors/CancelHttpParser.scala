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

package uk.gov.hmrc.timetopayproxy.connectors

import cats.syntax.either._
import play.api.http.Status
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.timetopayproxy.models.{ ConnectorError, TtppError }
import uk.gov.hmrc.timetopayproxy.models.saopledttp.{ CancelErrorResponse, CancelResponse, CancelResponseError }

import scala.util.{ Failure, Success, Try }

/** Custom HTTP parser for the cancel endpoint.
  * This is needed because the cancel endpoint has an unusual response pattern
  * that differs from other endpoints in the time-to-pay service:
  * The key difference is in 500 error structure
  */
object CancelHttpParser {

  implicit val httpReads: HttpReads[Either[TtppError, CancelResponse]] = (_, _, response) =>
    response.status match {
      case Status.OK =>
        response.json
          .validate[CancelResponse]
          .fold(
            _ => ConnectorError(503, "Couldn't parse body from upstream").asLeft[CancelResponse],
            Right(_)
          )

      case Status.INTERNAL_SERVER_ERROR =>
        response.json
          .validate[CancelResponse]
          .fold(
            _ => ConnectorError(503, "Couldn't parse body from upstream").asLeft[CancelResponse],
            cancelResponse => CancelResponseError(500, cancelResponse).asLeft[CancelResponse]
          )

      case Status.BAD_REQUEST =>
        Try(response.json) match {
          case Success(value) =>
            value
              .validate[CancelErrorResponse]
              .fold(
                _ => ConnectorError(503, "Couldn't parse body from upstream"),
                error => ConnectorError(error.code, error.details)
              )
              .asLeft[CancelResponse]
          case Failure(_) =>
            ConnectorError(Status.BAD_REQUEST, "Couldn't parse body from upstream").asLeft[CancelResponse]
        }

      case status =>
        // Any other status is unexpected
        ConnectorError(status, "Unexpected response from upstream").asLeft[CancelResponse]
    }
}
