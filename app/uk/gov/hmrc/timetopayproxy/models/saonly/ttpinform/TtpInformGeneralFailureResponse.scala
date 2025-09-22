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

package uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.timetopayproxy.models.error.{ProxyEnvelopeError, TtppWriteableError}

/** This is both an incoming and an outgoing error response body. */
final case class TtpInformGeneralFailureResponse(code: Int, details: String)
    extends ProxyEnvelopeError with TtppWriteableError { // TODO DTD-3779: This should no longer be writable

  def toWriteableProxyError: TtppWriteableError = this

  def toErrorResult: Result = Results.Status(code)(Json.toJson(this))
}

object TtpInformGeneralFailureResponse {
  // TODO DTD-3779: Change this to a reader because the outgoing format will be TtppErrorResponse.
  implicit val format: OFormat[TtpInformGeneralFailureResponse] = Json.format[TtpInformGeneralFailureResponse]
}
