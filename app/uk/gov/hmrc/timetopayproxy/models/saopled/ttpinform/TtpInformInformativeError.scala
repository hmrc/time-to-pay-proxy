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

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.timetopayproxy.models.error.{TtppSpecificError, TtppWriteableError}
import uk.gov.hmrc.timetopayproxy.models.saopled.common.ProcessingDateTimeInstant
import uk.gov.hmrc.timetopayproxy.models.saopled.common.apistatus.ApiStatus

/** Outgoing error for `500 Internal Server Error`. Also the incoming error from the `time-to-pay` service. */
final case class TtpInformInformativeError(
  // TODO DTD-3779: Implement the internalErrors field which won't exist in the 200 OK class.
  apisCalled: List[ApiStatus],
  processingDateTime: ProcessingDateTimeInstant
) extends TtppSpecificError with TtppWriteableError {

  def toWriteableProxyError: TtppWriteableError = this

  def toErrorResult: Result = Results.InternalServerError(Json.toJson(this))
}

object TtpInformInformativeError {
  implicit val format: OFormat[TtpInformInformativeError] = Json.format[TtpInformInformativeError]
}
