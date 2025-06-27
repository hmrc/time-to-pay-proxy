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

import java.time.LocalDate
import play.api.libs.json.{ Format, Json, OFormat }

final case class DebtItemChargeId(value: String) extends AnyVal

object DebtItemChargeId extends ValueTypeFormatter {
  implicit val format: Format[DebtItemChargeId] =
    valueTypeFormatter(DebtItemChargeId.apply, DebtItemChargeId.unapply)
}

final case class DebtItemCharge(
  debtItemChargeId: DebtItemChargeId,
  mainTrans: String,
  subTrans: String,
  originalDebtAmount: BigDecimal,
  interestStartDate: Option[LocalDate],
  paymentHistory: Seq[Payment]
) {
  require(!debtItemChargeId.value.trim().isEmpty(), "debtItemChargeId should not be empty")
  require(originalDebtAmount > 0, "originalDebtAmount should be a positive amount.")
}

object DebtItemCharge {
  implicit val format: OFormat[DebtItemCharge] = Json.format[DebtItemCharge]
}

final case class QuoteDebtItemCharge(
  debtItemChargeId: DebtItemChargeId,
  mainTrans: String,
  subTrans: String,
  originalDebtAmount: BigDecimal,
  interestStartDate: Option[LocalDate],
  paymentHistory: Seq[Payment],
  dueDate: Option[LocalDate]
) {
  require(!debtItemChargeId.value.trim().isEmpty(), "debtItemChargeId should not be empty")
  require(originalDebtAmount > 0, "originalDebtAmount should be a positive amount.")
}

object QuoteDebtItemCharge {
  implicit val format: OFormat[QuoteDebtItemCharge] = Json.format[QuoteDebtItemCharge]
}

