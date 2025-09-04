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

package uk.gov.hmrc.timetopayproxy.models.saopledttp

import play.api.libs.json.{ Format, Json, OFormat }

final case class ApiName(value: String) extends AnyVal
final case class ApiStatusCode(value: String) extends AnyVal
final case class ApiErrorResponse(value: String) extends AnyVal

final case class ApiStatus(
  name: ApiName,
  statusCode: ApiStatusCode,
  processingDateTime: ProcessingDateTime,
  errorResponse: Option[ApiErrorResponse]
)

object ApiName {
  implicit val format: Format[ApiName] = Json.valueFormat[ApiName]
}

object ApiStatusCode {
  implicit val format: Format[ApiStatusCode] = Json.valueFormat[ApiStatusCode]
}

object ApiErrorResponse {
  implicit val format: Format[ApiErrorResponse] = Json.valueFormat[ApiErrorResponse]
}

object ApiStatus {
  implicit val format: OFormat[ApiStatus] = Json.format[ApiStatus]
}
