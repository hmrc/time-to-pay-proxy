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

final case class PlanToCreatePlan(
  quoteId: QuoteId,
  quoteType: QuoteType,
  quoteDate: LocalDate,
  instalmentStartDate: LocalDate,
  instalmentAmount: Option[BigDecimal],
  paymentPlanType: PaymentPlanType,
  thirdPartyBank: Boolean,
  numberOfInstalments: Int,
  frequency: Option[FrequencyLowercase],
  duration: Option[Duration],
  initialPaymentMethod: Option[PaymentMethod],
  initialPaymentReference: Option[PaymentReference],
  initialPaymentDate: Option[LocalDate],
  initialPaymentAmount: Option[BigDecimal],
  totalDebtIncInt: BigDecimal,
  totalInterest: BigDecimal,
  interestAccrued: BigDecimal,
  planInterest: BigDecimal
) {
  require(!quoteId.value.trim().isEmpty(), "quoteId should not be empty")
  require(instalmentAmount.forall(_ > 0), "instalmentAmount should be a positive amount.")
  require(numberOfInstalments > 0, "numberOfInstalments should be positive.")
  require(initialPaymentAmount.forall(_ > 0), "initialPaymentAmount should be a positive amount.")
  require(totalDebtIncInt > 0, "totalDebtincInt should be a positive amount.")
  require(totalInterest >= 0, "totalInterest should be a positive amount.")
  require(interestAccrued >= 0, "interestAccrued should be a positive amount.")
  require(planInterest >= 0, "planInterest should be a positive amount.")
}

object PlanToCreatePlan {
  implicit val format: OFormat[PlanToCreatePlan] = Json.format[PlanToCreatePlan]
}

final case class PaymentInformation(paymentMethod: PaymentMethod, paymentReference: Option[PaymentReference]) {
  require(
    (paymentMethod != PaymentMethod.DirectDebit) || ((paymentMethod == PaymentMethod.DirectDebit) && paymentReference
      .forall(x => !x.value.trim().isEmpty()) && !paymentReference.isEmpty),
    "Direct Debit should always have payment reference"
  )

  require(
    paymentReference.isEmpty || paymentReference.forall(x => !x.value.trim().isEmpty()),
    "paymentReference should not be empty"
  )
}

object PaymentInformation {
  implicit val format: OFormat[PaymentInformation] = Json.format[PaymentInformation]
}
case class ChargeSource(value: String)

object ChargeSource {
  implicit val format: Format[ChargeSource] = Json.valueFormat[ChargeSource]
}

final case class ParentChargeReference(value: String) extends AnyVal

object ParentChargeReference {
  implicit val format: Format[ParentChargeReference] = Json.valueFormat[ParentChargeReference]
}

final case class ParentMainTrans(value: String)

object ParentMainTrans {
  implicit val format: Format[ParentMainTrans] = Json.valueFormat[ParentMainTrans]
}
final case class CreatePlanDebtItemCharge(
  debtItemChargeId: DebtItemChargeId,
  mainTrans: String,
  subTrans: String,
  originalDebtAmount: BigDecimal,
  interestStartDate: Option[LocalDate],
  paymentHistory: Seq[Payment],
  dueDate: Option[LocalDate],
  chargeSource: Option[ChargeSource],
  parentChargeReference: Option[ParentChargeReference],
  parentMainTrans: Option[ParentMainTrans]
) {
  require(!debtItemChargeId.value.trim().isEmpty(), "debtItemChargeId should not be empty")
  require(originalDebtAmount > 0, "originalDebtAmount should be a positive amount.")
}

object CreatePlanDebtItemCharge {
  implicit val format: OFormat[CreatePlanDebtItemCharge] = Json.format[CreatePlanDebtItemCharge]

}

final case class CreatePlanRequest(
  customerReference: CustomerReference,
  quoteReference: QuoteReference,
  channelIdentifier: ChannelIdentifier,
  plan: PlanToCreatePlan,
  debtItemCharges: Seq[CreatePlanDebtItemCharge],
  payments: Seq[PaymentInformation],
  customerPostCodes: Seq[CustomerPostCode],
  instalments: Seq[Instalment],
  regimeType: Option[OpLedRegimeType]
) {
  require(!customerReference.value.trim().isEmpty(), "customerReference should not be empty")
  require(!quoteReference.value.trim().isEmpty(), "quoteReference should not be empty")
}

object CreatePlanRequest {
  implicit val format: OFormat[CreatePlanRequest] = Json.format[CreatePlanRequest]
}
