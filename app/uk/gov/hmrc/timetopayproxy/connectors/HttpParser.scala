/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.http.Status
import play.api.libs.json.Reads
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.timetopayproxy.models.{ ConnectorError, IncomingApiError, TtppError }
import cats.syntax.either._

import scala.util.{ Failure, Success, Try }

trait HttpParser[IncomingError <: IncomingApiError] {
  implicit def httpReads[T](implicit
    successReads: Reads[T],
    errorReads: Reads[IncomingError]
  ): HttpReads[Either[TtppError, T]] = (_, _, response) =>
    response.status match {
      case Status.OK | Status.CREATED =>
        response.json
          .validate[T]
          .fold(
            _ => ConnectorError(503, "Couldn't parse body from upstream").asLeft[T],
            Right(_)
          )
      case status =>
        Try(response.json) match {
          case Success(value) =>
            value
              .validate[IncomingError]
              .fold(
                _ => ConnectorError(503, "Couldn't parse body from upstream"),
                error => error.toConnectorError(status = status)
              )
              .asLeft[T]
          case Failure(_) => ConnectorError(status, "Couldn't parse body from upstream").asLeft[T]
        }

    }
}
