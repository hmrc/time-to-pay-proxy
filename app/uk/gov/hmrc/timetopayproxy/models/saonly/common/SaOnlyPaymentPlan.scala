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

package uk.gov.hmrc.timetopayproxy.models.saonly.common

import cats.data.NonEmptyList
import play.api.libs.json.{ Format, Json, OFormat }
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.{ FrequencyLowercase, InformDebtItemCharge }

final case class SaOnlyPaymentPlan(
  arrangementAgreedDate: ArrangementAgreedDate,
  ttpEndDate: TtpEndDate,
  frequency: FrequencyLowercase,
  initialPaymentDate: Option[InitialPaymentDate],
  initialPaymentAmount: Option[GbpPounds],
  ddiReference: Option[DdiReference]
)

object SaOnlyPaymentPlan {
  implicit val format: OFormat[SaOnlyPaymentPlan] = Json.format[SaOnlyPaymentPlan]
}

final case class SaOnlyPaymentPlanR2(
  arrangementAgreedDate: ArrangementAgreedDate,
  ttpEndDate: TtpEndDate,
  frequency: FrequencyLowercase,
  initialPaymentDate: Option[InitialPaymentDate],
  initialPaymentAmount: Option[GbpPounds],
  ddiReference: Option[DdiReference],
  debtItemCharges: NonEmptyList[InformDebtItemCharge]
)

object SaOnlyPaymentPlanR2 {
  implicit val informDebtItemChargeNELFormat: Format[NonEmptyList[InformDebtItemCharge]] =
    NonEmptyListFormat.nonEmptyListFormat[InformDebtItemCharge]

  implicit val format: OFormat[SaOnlyPaymentPlanR2] = Json.format[SaOnlyPaymentPlanR2]
}
