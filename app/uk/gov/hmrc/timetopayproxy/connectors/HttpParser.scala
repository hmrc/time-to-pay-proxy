/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.timetopayproxy.models.{ConnectorError, Error, TimeToPayError, TtppError}
import cats.syntax.either._
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope
trait HttpParser {


  implicit def httpReads[T](implicit headerCarrier: HeaderCarrier, rds: Reads[T]): HttpReads[Either[TtppError, T]] = (_, _, response) => {
    response.status match {
      case Status.OK =>
        response.json.validate[T].fold(
          error => ConnectorError(503, "Couldn't parse body from upstream").asLeft[T],
          Right(_)
        )
      case status =>
          response.json.validate[TimeToPayError].fold(
            _ => ConnectorError(503, "Couldn't parse body from upstream"),
            error => ConnectorError(status, error.failures.headOption.map(_.reason).getOrElse("An unknown error has occurred"))
          ).asLeft[T]
    }
  }
}