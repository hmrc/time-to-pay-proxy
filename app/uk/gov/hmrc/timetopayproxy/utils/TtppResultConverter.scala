/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json}
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.timetopayproxy.models.TimeToPayErrorResponse

object TtppResultConverter {

  implicit class ToResult[T](r: T) {
    def toResult(implicit format: Format[T]): Result = r match {
      case TimeToPayErrorResponse(status, _) =>
        Results.Status(status)(Json.toJson(r))
      case _ => Results.Ok(Json.toJson(r))
    }
  }

}
