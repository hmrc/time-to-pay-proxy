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
import uk.gov.hmrc.timetopayproxy.models.saopled.common.ProcessingDateTimeInstant
import uk.gov.hmrc.timetopayproxy.models.saopled.common.apistatus.ApiStatus

/** This is intended for both `200 OK` and `500 Internal Server Error`.
  * This is also an incoming error from the `time-to-pay` service.
  */
final case class TtpCancelInformativeResponse(
  apisCalled: List[ApiStatus],
  processingDateTime: ProcessingDateTimeInstant
) extends TtppSpecificError with TtppWriteableError {

  def toWriteableProxyError: TtppWriteableError = this

  def toErrorResult: Result = Results.Ok(Json.toJson(this))
}

object TtpCancelInformativeResponse {
  implicit val format: OFormat[TtpCancelInformativeResponse] = Json.format[TtpCancelInformativeResponse]
}
