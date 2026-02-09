/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.models.saonly.ttpfullamend

import cats.data.NonEmptyList
import play.api.libs.json.{ Format, Json, OFormat }
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.models.{ DebtItemChargeId, FrequencyLowercase }
import uk.gov.hmrc.timetopayproxy.utils.json.CatsNonEmptyListJson

case class NewPaymentPlan(
  arrangementAgreedDate: ArrangementAgreedDate,
  ttpEndDate: TtpEndDate,
  frequency: FrequencyLowercase,
  ddiReference: Option[DdiReference],
  initialPaymentDate: Option[InitialPaymentDate],
  initialPaymentAmount: Option[GbpPounds],
  debtItemCharges: NonEmptyList[NewDebtItemChargeReference]
)

object NewPaymentPlan {
  implicit val formats: OFormat[NewPaymentPlan] = Json.format[NewPaymentPlan]
  implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat[T]
}

case class NewDebtItemChargeReference(
  debtItemChargeId: DebtItemChargeId,
  chargeSource: ChargeSourceSAOnly,
  chargeAmendment: ChargeAmendment
)

object NewDebtItemChargeReference {
  implicit val format: OFormat[NewDebtItemChargeReference] = Json.format[NewDebtItemChargeReference]
}
