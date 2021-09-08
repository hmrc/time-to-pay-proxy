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

final case class PlanToCreatePlan(quoteId: QuoteId,
                                  quoteType: QuoteType,
                                  quoteDate: LocalDate,
                                  instalmentStartDate: LocalDate,
                                  instalmentAmount: BigDecimal,
                                  paymentPlanType: PaymentPlanType,
                                  thirdPartyBank: Boolean,
                                  numberOfInstalments: Int,
                                  frequency: Frequency,
                                  duration: Duration,
                                  initialPaymentDate: LocalDate,
                                  initialPaymentAmount: BigDecimal,
                                  totalDebtincInt: BigDecimal,
                                  totalInterest: BigDecimal,
                                  interestAccrued: BigDecimal,
                                  planInterest: BigDecimal) {
  require(!quoteId.value.trim().isEmpty(), "quoteId should not be empty")
}

object PlanToCreatePlan {
  implicit val format = Json.format[PlanToCreatePlan]
}

final case class PaymentInformation(paymentMethod: PaymentMethod, paymentReference: PaymentReference) {
  require(!paymentReference.value.trim().isEmpty(), "paymentReference should not be empty")
}

object PaymentInformation {
  implicit val format = Json.format[PaymentInformation]
}

final case class CreatePlanRequest(customerReference: CustomerReference,
                                   quoteReference: QuoteReference,
                                   channelIdentifier: ChannelIdentifier,
                                   plan: PlanToCreatePlan,
                                   debtItems: Seq[DebtItem],
                                   payments: Seq[PaymentInformation],
                                   customerPostCodes: Seq[CustomerPostCode],
                                   instalments: Seq[Instalment]
                                  ) {
  require(!customerReference.value.trim().isEmpty(), "customerReference should not be empty")
  require(!quoteReference.value.trim().isEmpty(), "quoteReference should not be empty")
}

object CreatePlanRequest {
  implicit val format = Json.format[CreatePlanRequest]
}
