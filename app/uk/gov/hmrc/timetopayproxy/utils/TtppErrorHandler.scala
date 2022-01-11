/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.timetopayproxy.models.{ConnectorError, TtppError, TtppErrorResponse, ValidationError}

object TtppErrorHandler {

  implicit class FromErrorToResponse(error: TtppError) {
    def toErrorResponse:  TtppErrorResponse = error match {
      case ConnectorError(status, message) =>
        TtppErrorResponse(status, s"$message")
      case ValidationError(message) =>
        TtppErrorResponse(400, s"$message")
      case e => TtppErrorResponse(500, s"${e.toString}")
    }
  }

}
