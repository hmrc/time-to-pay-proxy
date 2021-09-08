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

import play.api.libs.json.Json

final case class DebtItemId(value: String) extends AnyVal

object DebtItemId extends ValueTypeFormatter {
  implicit val format =
    valueTypeFormatter(DebtItemId.apply, DebtItemId.unapply)
}

final case class DebtItemChargeId(value: String) extends AnyVal

object DebtItemChargeId extends ValueTypeFormatter {
  implicit val format =
    valueTypeFormatter(DebtItemChargeId.apply, DebtItemChargeId.unapply)
}

final case class DebtItem(debtItemId: DebtItemId,
                          debtItemChargeId: DebtItemChargeId,
                          mainTrans: MainTransType,
                          subTrans: SubTransType,
                          originalDebtAmount: BigDecimal,
                          interestStartDate: LocalDate,
                          paymentHistory: Seq[Payment]) {
  require(!debtItemId.value.trim().isEmpty(), "debtItemId should not be empty")
  require(!debtItemChargeId.value.trim().isEmpty(), "debtItemChargeId should not be empty")
}

object DebtItem {
  implicit val format = Json.format[DebtItem]
}
