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

import play.api.libs.json.{ Json, Writes }
import play.api.mvc.{ Result, Results }
import uk.gov.hmrc.timetopayproxy.models.error.{ TtppSpecificError, TtppWriteableError }

/** Outgoing error for `500 Internal Server Error`.
  *
  * TODO DTD-3785: Also mention that this is also an incoming error from the `time-to-pay` service.
  */
final case class TtpCancelInformativeError(
  // TODO DTD-3785: Make it standalone and implement the internalErrors field which won't exist in the 200 OK class.
  response: TtpCancelInformativeResponse
) extends TtppSpecificError with TtppWriteableError {

  def toWriteableProxyError: TtppWriteableError = this

  def toErrorResult: Result = Results.InternalServerError(Json.toJson(this.response))
}

object TtpCancelInformativeError {
  // TODO DTD-3785: This will need a reader.
  implicit val writes: Writes[TtpCancelInformativeError] =
    (error: TtpCancelInformativeError) => Json.toJsObject(error.response)
}
