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

package uk.gov.hmrc.timetopayproxy.models

import play.api.libs.json.{ Json, OFormat }

import java.time.LocalDate

final case class InitialCollection(dueDate: LocalDate, amountDue: BigDecimal)

object InitialCollection {
  implicit val format: OFormat[InitialCollection] = Json.format[InitialCollection]
}

final case class RegularCollection(dueDate: LocalDate, amountDue: BigDecimal)

object RegularCollection {
  implicit val format: OFormat[RegularCollection] = Json.format[RegularCollection]
}

final case class Collections(initialCollection: Option[InitialCollection], regularCollections: List[RegularCollection])

object Collections {
  implicit val format: OFormat[Collections] = Json.format[Collections]
}
