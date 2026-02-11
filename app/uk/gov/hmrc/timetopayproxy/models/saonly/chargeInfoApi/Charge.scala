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

package uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi

import play.api.libs.json.{ Format, Json, OFormat }

import java.time.LocalDate

sealed trait Charge

final case class ChargeR1(
  taxPeriodFrom: TaxPeriodFrom,
  taxPeriodTo: TaxPeriodTo,
  chargeType: ChargeType,
  mainType: MainType,
  subTrans: SubTrans,
  outstandingAmount: OutstandingAmount,
  dueDate: DueDate,
  isInterestBearingCharge: Option[ChargeInfoIsInterestBearingCharge],
  interestStartDate: Option[InterestStartDate],
  accruedInterest: AccruedInterest,
  chargeSource: ChargeInfoChargeSource,
  parentMainTrans: Option[ChargeInfoParentMainTrans],
  tieBreaker: Option[TieBreaker],
  saTaxYearEnd: Option[SaTaxYearEnd],
  creationDate: Option[CreationDate],
  originalChargeType: Option[OriginalChargeType]
) extends Charge

object ChargeR1 {
  implicit val format: OFormat[ChargeR1] = Json.format[ChargeR1]
}

final case class ChargeR2(
  taxPeriodFrom: TaxPeriodFrom,
  taxPeriodTo: TaxPeriodTo,
  chargeType: ChargeType,
  mainType: MainType,
  subTrans: SubTrans,
  outstandingAmount: OutstandingAmount,
  dueDate: DueDate,
  isInterestBearingCharge: Option[ChargeInfoIsInterestBearingCharge],
  interestStartDate: Option[InterestStartDate],
  accruedInterest: AccruedInterest,
  chargeSource: ChargeInfoChargeSource,
  parentMainTrans: Option[ChargeInfoParentMainTrans],
  tieBreaker: Option[TieBreaker],
  saTaxYearEnd: Option[SaTaxYearEnd],
  creationDate: Option[CreationDate],
  originalChargeType: Option[OriginalChargeType],
  locks: Option[List[Lock]]
) extends Charge

object ChargeR2 {
  implicit val format: OFormat[ChargeR2] = Json.format[ChargeR2]
}

final case class TaxPeriodFrom(value: LocalDate) extends AnyVal

object TaxPeriodFrom {
  implicit val format: Format[TaxPeriodFrom] = Json.valueFormat[TaxPeriodFrom]
}

final case class TaxPeriodTo(value: LocalDate) extends AnyVal

object TaxPeriodTo {
  implicit val format: Format[TaxPeriodTo] = Json.valueFormat[TaxPeriodTo]
}

final case class ChargeType(value: String) extends AnyVal

object ChargeType {
  implicit val format: Format[ChargeType] = Json.valueFormat[ChargeType]
}

final case class MainType(value: String) extends AnyVal

object MainType {
  implicit val format: Format[MainType] = Json.valueFormat[MainType]
}

final case class SubTrans(value: String) extends AnyVal

object SubTrans {
  implicit val format: Format[SubTrans] = Json.valueFormat[SubTrans]
}

final case class OutstandingAmount(value: BigInt) extends AnyVal

object OutstandingAmount {
  implicit val format: Format[OutstandingAmount] = Json.valueFormat[OutstandingAmount]
}

final case class DueDate(dueDate: LocalDate) extends AnyVal

object DueDate {
  implicit val format: Format[DueDate] = Json.valueFormat[DueDate]
}

final case class ChargeInfoIsInterestBearingCharge(value: Boolean) extends AnyVal

object ChargeInfoIsInterestBearingCharge {
  implicit val format: Format[ChargeInfoIsInterestBearingCharge] = Json.valueFormat[ChargeInfoIsInterestBearingCharge]
}

final case class InterestStartDate(value: LocalDate) extends AnyVal

object InterestStartDate {
  implicit val format: Format[InterestStartDate] = Json.valueFormat[InterestStartDate]
}

final case class AccruedInterest(value: BigInt) extends AnyVal

object AccruedInterest {
  implicit val format: Format[AccruedInterest] = Json.valueFormat[AccruedInterest]
}

final case class ChargeInfoChargeSource(value: String) extends AnyVal

object ChargeInfoChargeSource {
  implicit val format: Format[ChargeInfoChargeSource] = Json.valueFormat[ChargeInfoChargeSource]
}

final case class ChargeInfoParentMainTrans(value: String) extends AnyVal

object ChargeInfoParentMainTrans {
  implicit val format: Format[ChargeInfoParentMainTrans] = Json.valueFormat[ChargeInfoParentMainTrans]
}

final case class OriginalCreationDate(value: LocalDate) extends AnyVal

object OriginalCreationDate {
  implicit val format: Format[OriginalCreationDate] = Json.valueFormat[OriginalCreationDate]
}

final case class TieBreaker(value: String) extends AnyVal

object TieBreaker {
  implicit val format: Format[TieBreaker] = Json.valueFormat[TieBreaker]
}

final case class OriginalTieBreaker(value: String) extends AnyVal

object OriginalTieBreaker {
  implicit val format: Format[OriginalTieBreaker] = Json.valueFormat[OriginalTieBreaker]
}

final case class SaTaxYearEnd(value: LocalDate) extends AnyVal

object SaTaxYearEnd {
  implicit val format: Format[SaTaxYearEnd] = Json.valueFormat[SaTaxYearEnd]
}

final case class CreationDate(value: LocalDate) extends AnyVal

object CreationDate {
  implicit val format: Format[CreationDate] = Json.valueFormat[CreationDate]
}

final case class OriginalChargeType(value: String) extends AnyVal

object OriginalChargeType {
  implicit val format: Format[OriginalChargeType] = Json.valueFormat[OriginalChargeType]
}

final case class Lock(lockType: String, lockReason: String)

object Lock {
  implicit val format: OFormat[Lock] = Json.format[Lock]
}
