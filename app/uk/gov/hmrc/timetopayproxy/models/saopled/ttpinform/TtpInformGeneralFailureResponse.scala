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

package uk.gov.hmrc.timetopayproxy.models.saopled.ttpinform

import play.api.libs.json.{ JsObject, Json, OFormat }
import uk.gov.hmrc.timetopayproxy.models.error.{ InternalTtppError, TtppWriteableError }

/** This is both an incoming and an outgoing error response body. */
final case class TtpInformGeneralFailureResponse(code: Int, details: String)
    extends InternalTtppError with TtppWriteableError { // TODO DTD-3779: This should no longer be writable
  def toWriteableProxyError: TtppWriteableError = this
  def toJson: JsObject = Json.toJsObject(this)
}

object TtpInformGeneralFailureResponse {
  implicit val format: OFormat[TtpInformGeneralFailureResponse] = Json.format[TtpInformGeneralFailureResponse]
}
