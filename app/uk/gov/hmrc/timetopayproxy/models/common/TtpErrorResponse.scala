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

package uk.gov.hmrc.timetopayproxy.models.common

import play.api.libs.json.{ Json, OFormat }

final case class ErrorResponse(code: String, reason: String)
final case class TtpErrorResponse(failures: Seq[ErrorResponse])
final case class TtpErrorResponseAndCode(statusCode: Int, error: TtpErrorResponse)

object ErrorResponse {
  implicit val format: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}

object TtpErrorResponse {
  implicit val format: OFormat[TtpErrorResponse] = Json.format[TtpErrorResponse]
}

object TtpErrorResponseAndCode {
  implicit val format: OFormat[TtpErrorResponseAndCode] = Json.format[TtpErrorResponseAndCode]
}
