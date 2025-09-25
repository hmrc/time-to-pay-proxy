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

package uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel

import play.api.libs.json.{ Json, OFormat }
import uk.gov.hmrc.timetopayproxy.models.FrequencyLowercase
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.saonly.common.{ ArrangementAgreedDate, InitialPaymentDate, TtpEndDate }

final case class TtpCancelPaymentPlan(
  arrangementAgreedDate: ArrangementAgreedDate,
  ttpEndDate: TtpEndDate,
  frequency: FrequencyLowercase,
  cancellationDate: CancellationDate,
  initialPaymentDate: Option[InitialPaymentDate],
  initialPaymentAmount: Option[GbpPounds]
)

object TtpCancelPaymentPlan {
  implicit val format: OFormat[TtpCancelPaymentPlan] = Json.format[TtpCancelPaymentPlan]
}
