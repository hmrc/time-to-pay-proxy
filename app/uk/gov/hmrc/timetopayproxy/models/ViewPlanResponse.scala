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

import play.api.libs.json.Json

import java.time.LocalDate

final case class InstalmentPlan(
                                 dutyId: String,
                                 dueDate: LocalDate,
                                 amountDue: BigDecimal,
                                 balance: BigDecimal,
                                 interest: Double,
                                 interestRate: Double,
                                 instalmentNumber: Int
                               )

object InstalmentPlan {
  implicit val format = Json.format[InstalmentPlan]
}

final case class ViewPlanResponse(customerReference: String,
                                  pegaPlanId: String,
                                  quoteType: String,
                                  paymentMethod: String,
                                  paymentReference: String,
                                  instalments: Seq[InstalmentPlan],
                                  numberOfInstalments: String,
                                  totalDebtAmount: BigDecimal,
                                  totalInterest: Double)

object ViewPlanResponse {
  implicit val format = Json.format[ViewPlanResponse]
}