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

package uk.gov.hmrc.timetopayproxy.models

import play.api.libs.json.Json

import java.time.LocalDate

final case class ViewPlanResponsePlan(
  planId: PlanId,
  caseId: CaseId,
  quoteId: QuoteId,
  quoteDate: LocalDate,
  quoteType: QuoteType,
  paymentPlanType: PaymentPlanType,
  thirdPartyBank: Boolean,
  numberOfInstalments: Int,
  totalDebtIncInt: BigInt,
  totalInterest: BigDecimal,
  interestAccrued: BigInt,
  planInterest: BigDecimal
)

object ViewPlanResponsePlan {
  implicit val format = Json.format[ViewPlanResponsePlan]
}
