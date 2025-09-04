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

package uk.gov.hmrc.timetopayproxy.utils

import play.api.libs.json.{ Json, Writes }
import uk.gov.hmrc.timetopayproxy.models.{ ConnectorError, TtppError, TtppErrorResponse, ValidationError }
import uk.gov.hmrc.timetopayproxy.models.saopledttp.{ CancelResponse, CancelResponseError }

sealed trait ErrorResponse
final case class StandardErrorResponse(ttppErrorResponse: TtppErrorResponse) extends ErrorResponse
final case class CancelErrorResponseWrapper(statusCode: Int, cancelResponse: CancelResponse) extends ErrorResponse

object TtppErrorHandler {

  implicit val standardErrorResponseWrites: Writes[StandardErrorResponse] = (o: StandardErrorResponse) =>
    Json.toJson(o.ttppErrorResponse)

  implicit val cancelErrorResponseWrapperWrites: Writes[CancelErrorResponseWrapper] = (o: CancelErrorResponseWrapper) =>
    Json.toJson(o.cancelResponse)

  implicit val errorResponseWrites: Writes[ErrorResponse] = {
    case std: StandardErrorResponse         => Json.toJson(std)
    case cancel: CancelErrorResponseWrapper => Json.toJson(cancel)
  }

  implicit class FromErrorToResponse(error: TtppError) {
    def toErrorResponse: ErrorResponse = error match {
      case ConnectorError(status, message) =>
        StandardErrorResponse(TtppErrorResponse(status, s"$message"))
      case ValidationError(message) =>
        StandardErrorResponse(TtppErrorResponse(400, s"$message"))
      case CancelResponseError(statusCode, cancelResponse) =>
        // For CancelResponseError, we create a special response that will be handled differently
        CancelErrorResponseWrapper(statusCode, cancelResponse)
      case e => StandardErrorResponse(TtppErrorResponse(500, s"${e.toString}"))
    }
  }

}
