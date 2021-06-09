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

package uk.gov.hmrc.timetopayproxy.models

import java.time.LocalDate

import play.api.libs.json.{Format, JsPath, Json}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

final case class DutyId(value: String) extends AnyVal

object DutyId extends ValueTypeFormatter {
  implicit val formatter = valueTypeFormatter[String, DutyId](
    DutyId.apply,
    DutyId.unapply
  )
}

final case class Subtrans(value: String) extends AnyVal

object Subtrans extends ValueTypeFormatter {

  implicit val formatter = valueTypeFormatter[String, Subtrans](
    Subtrans.apply,
    Subtrans.unapply
  )

}

final case class Duty(dutyId: DutyId,
                      subtrans: Subtrans,
                      originalDebtAmount: BigDecimal,
                      interestStartDate: LocalDate,
                      breathingSpaces: List[BreathingSpace])

object Duty {
  implicit val format = Json.format[Duty]
}
