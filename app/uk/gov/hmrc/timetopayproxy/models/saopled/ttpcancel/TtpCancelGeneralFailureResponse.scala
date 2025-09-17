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

package uk.gov.hmrc.timetopayproxy.models.saopled.ttpcancel

import play.api.libs.json.{ Json, OFormat }
import play.api.mvc.{ Result, Results }
import uk.gov.hmrc.timetopayproxy.models.error.{ TtppSpecificError, TtppWriteableError }

/** This is both an incoming and an outgoing error response body. */
final case class TtpCancelGeneralFailureResponse(code: Int, details: String)
    extends TtppSpecificError with TtppWriteableError /* TODO DTD-3785: This should no longer be writeable. */ {

  def toWriteableProxyError: TtppWriteableError = this

  def toErrorResult: Result = Results.Status(code)(Json.toJson(this))
}

object TtpCancelGeneralFailureResponse {
  // TODO DTD-3785: Change this to a reader because the outgoing format will be TtppErrorResponse.
  implicit val format: OFormat[TtpCancelGeneralFailureResponse] = Json.format[TtpCancelGeneralFailureResponse]
}
