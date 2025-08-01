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

package uk.gov.hmrc.timetopayproxy.models

import play.api.libs.json.{ Json, OFormat }

final case class TimeToPayInnerError(code: String, reason: String)

final case class TimeToPayError(failures: Seq[TimeToPayInnerError]) extends IncomingApiError {
  def toConnectorError(status: Int): ConnectorError =
    ConnectorError(
      status,
      failures.headOption.map(_.reason).getOrElse("An unknown error has occurred")
    )
}

object TimeToPayInnerError {
  implicit val format: OFormat[TimeToPayInnerError] = Json.format[TimeToPayInnerError]
}

object TimeToPayError {
  implicit val format: OFormat[TimeToPayError] = Json.format[TimeToPayError]
}
