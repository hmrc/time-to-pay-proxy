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

import play.api.libs.json.{ Format, Json, OFormat }

import java.time.LocalDate

final case class IsInterestBearingCharge(value: Boolean) extends AnyVal

object IsInterestBearingCharge extends ValueTypeFormatter {
  implicit val format: Format[IsInterestBearingCharge] =
    valueTypeFormatter(IsInterestBearingCharge.apply, IsInterestBearingCharge.unapply)
}

final case class UseChargeReference(value: Boolean) extends AnyVal

object UseChargeReference extends ValueTypeFormatter {
  implicit val format: Format[UseChargeReference] =
    valueTypeFormatter(UseChargeReference.apply, UseChargeReference.unapply)
}

final case class DebtItemChargeSelfServe(
  outstandingDebtAmount: BigDecimal,
  mainTrans: String,
  subTrans: String,
  debtItemChargeId: DebtItemChargeId,
  interestStartDate: Option[LocalDate],
  debtItemOriginalDueDate: LocalDate,
  isInterestBearingCharge: IsInterestBearingCharge,
  useChargeReference: UseChargeReference
)

object DebtItemChargeSelfServe {
  implicit val format: OFormat[DebtItemChargeSelfServe] = Json.format[DebtItemChargeSelfServe]
}
