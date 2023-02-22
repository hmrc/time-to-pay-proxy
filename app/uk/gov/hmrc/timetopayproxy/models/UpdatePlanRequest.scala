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

import play.api.libs.json.Json
import enumeratum.{ Enum, EnumEntry, PlayJsonEnum }

import scala.collection.immutable

sealed abstract class CompleteReason(override val entryName: String) extends EnumEntry

object CompleteReason extends Enum[CompleteReason] with PlayJsonEnum[CompleteReason] {
  val values: immutable.IndexedSeq[CompleteReason] = findValues

  case object PaymentInFull extends CompleteReason("Payment in full")
  case object PaymentInFullCaps extends CompleteReason("Payment in Full")
  case object AmendmentOfChargesToNil extends CompleteReason("Amendment of charges to nil")
  case object AmendmentOfChargesToNilCaps extends CompleteReason("Amendment of Charges to Nil")
  case object Remission extends CompleteReason("Remission")
  case object PaidWithinTolerance extends CompleteReason("Paid within Tolerance")
}

final case class CancellationReason(value: String) extends AnyVal

object CancellationReason extends ValueTypeFormatter {
  implicit val format =
    valueTypeFormatter(CancellationReason.apply, CancellationReason.unapply)
}

sealed abstract class PaymentMethod(override val entryName: String) extends EnumEntry

object PaymentMethod extends Enum[PaymentMethod] with PlayJsonEnum[PaymentMethod] {
  val values: scala.collection.immutable.IndexedSeq[PaymentMethod] = findValues

  case object DirectDebit extends PaymentMethod("directDebit")
  case object Bacs extends PaymentMethod("BACS")
  case object Cheque extends PaymentMethod("cheque")
  case object CardPayment extends PaymentMethod("cardPayment")
  case object OnGoingAward extends PaymentMethod("Ongoing award")
}

final case class PaymentReference(value: String) extends AnyVal

object PaymentReference extends ValueTypeFormatter {
  implicit val format =
    valueTypeFormatter(PaymentReference.apply, PaymentReference.unapply)
}

final case class UpdateType(value: String) extends AnyVal

object UpdateType extends ValueTypeFormatter {
  implicit val format =
    valueTypeFormatter(UpdateType.apply, UpdateType.unapply)
}

final case class UpdatePlanRequest(
  customerReference: CustomerReference,
  planId: PlanId,
  updateType: UpdateType,
  planStatus: Option[PlanStatus],
  completeReason: Option[CompleteReason],
  cancellationReason: Option[CancellationReason],
  thirdPartyBank: Option[Boolean],
  payments: Option[List[PaymentInformation]]
) {
  require(
    !(updateType.value == "planStatus") || !planStatus.isEmpty,
    "Invalid UpdatePlanRequest payload: Payload has a missing field or an invalid format. Field name: planStatus."
  )
}

object UpdatePlanRequest {
  implicit val format = Json.format[UpdatePlanRequest]
}
