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

package uk.gov.hmrc.timetopayproxy.models.chargeInfoApi

import enumeratum.{ Enum, EnumEntry, PlayJsonEnum }
import play.api.libs.json.{ Format, Json, OFormat }
import uk.gov.hmrc.timetopayproxy.models.Identification

import java.time.{ LocalDate, LocalDateTime }
import scala.collection.immutable

final case class ChargeInfoResponse(
  processingDateTime: LocalDateTime,
  identification: List[Identification],
  individualDetails: IndividualDetails,
  addresses: List[Address],
  chargeTypeAssessment: List[ChargeTypeAssessment]
)

object ChargeInfoResponse {
  implicit val format: OFormat[ChargeInfoResponse] = Json.format[ChargeInfoResponse]
}

final case class IndividualDetails(
  title: Option[Title],
  firstName: Option[FirstName],
  lastName: Option[LastName],
  dateOfBirth: Option[DateOfBirth],
  districtNumber: Option[DistrictNumber],
  customerType: CustomerType,
  transitionToCDCS: TransitionToCdcs
)

object IndividualDetails {
  implicit val format: OFormat[IndividualDetails] =
    Json.format[IndividualDetails]
}

final case class Title(value: String) extends AnyVal

object Title {
  implicit val format: Format[Title] = Json.valueFormat[Title]
}

final case class FirstName(value: String) extends AnyVal

object FirstName {
  implicit val format: Format[FirstName] = Json.valueFormat[FirstName]
}

final case class LastName(value: String) extends AnyVal

object LastName {
  implicit val format: Format[LastName] = Json.valueFormat[LastName]
}

final case class DateOfBirth(value: LocalDate) extends AnyVal

object DateOfBirth {
  implicit val format: Format[DateOfBirth] = Json.valueFormat[DateOfBirth]
}

final case class DistrictNumber(value: String) extends AnyVal

object DistrictNumber {
  implicit val format: Format[DistrictNumber] = Json.valueFormat[DistrictNumber]
}

sealed abstract class CustomerType(override val entryName: String) extends EnumEntry

object CustomerType extends Enum[CustomerType] with PlayJsonEnum[CustomerType] {

  val values: immutable.IndexedSeq[CustomerType] = findValues

  /** Newer customer setup. Doesn't require IDMS. Requires CDCS and ETMP, but will eventually fully migrate to ETMP. */
  case object ItsaMigtrated extends CustomerType("MTD(ITSA)")

  /** Customer was either transitioned to CDCS or they will be transitioned by our services.
    *
    * Not-so-old customer, requires newer CDCS instead of old IDMS.
    */
  case object ClassicSaTransitioned extends CustomerType("Classic SA - Transitioned")

  /** Old customer, requires old IDMS instead of newer CDCS. */
  case object ClassicSaNonTransitioned extends CustomerType("Classic SA - Non Transitioned")

}

final case class TransitionToCdcs(value: Boolean) extends AnyVal

object TransitionToCdcs {
  implicit val format: Format[TransitionToCdcs] = Json.valueFormat[TransitionToCdcs]
}

final case class Address(
  addressType: AddressType,
  addressLine1: AddressLine1,
  addressLine2: Option[AddressLine2],
  addressLine3: Option[AddressLine3],
  addressLine4: Option[AddressLine4],
  rls: Option[Rls],
  contactDetails: Option[ContactDetails],
  postCode: Option[ChargeInfoPostCode],
  postcodeHistory: List[PostCodeInfo]
)

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]
}

final case class AddressType(value: String) extends AnyVal

object AddressType {
  implicit val format: Format[AddressType] = Json.valueFormat[AddressType]
}

final case class AddressLine1(value: String) extends AnyVal

object AddressLine1 {
  implicit val format: Format[AddressLine1] = Json.valueFormat[AddressLine1]
}

final case class AddressLine2(value: String) extends AnyVal

object AddressLine2 {
  implicit val format: Format[AddressLine2] = Json.valueFormat[AddressLine2]
}

final case class AddressLine3(value: String) extends AnyVal

object AddressLine3 {
  implicit val format: Format[AddressLine3] = Json.valueFormat[AddressLine3]
}

final case class AddressLine4(value: String) extends AnyVal

object AddressLine4 {
  implicit val format: Format[AddressLine4] = Json.valueFormat[AddressLine4]
}

final case class Rls(value: Boolean) extends AnyVal

object Rls {
  implicit val format: Format[Rls] = Json.valueFormat[Rls]
}

final case class ContactDetails(
  telephoneNumber: Option[TelephoneNumber],
  fax: Option[Fax],
  mobile: Option[Mobile],
  emailAddress: Option[Email],
  emailSource: Option[EmailSource]
)

object ContactDetails {
  implicit val format: OFormat[ContactDetails] = Json.format[ContactDetails]
}

final case class TelephoneNumber(value: String) extends AnyVal

object TelephoneNumber {
  implicit val format: Format[TelephoneNumber] = Json.valueFormat[TelephoneNumber]
}

final case class Fax(value: String) extends AnyVal

object Fax {
  implicit val format: Format[Fax] = Json.valueFormat[Fax]
}

final case class Mobile(value: String) extends AnyVal

object Mobile {
  implicit val format: Format[Mobile] = Json.valueFormat[Mobile]
}

final case class Email(value: String) extends AnyVal

object Email {
  implicit val format: Format[Email] = Json.valueFormat[Email]
}

final case class EmailSource(value: String) extends AnyVal

object EmailSource {
  implicit val format: Format[EmailSource] = Json.valueFormat[EmailSource]
}

final case class AltFormat(value: Int) extends AnyVal

object AltFormat {
  implicit val format: Format[AltFormat] = Json.valueFormat[AltFormat]
}

final case class ChargeInfoPostCode(value: String) extends AnyVal

object ChargeInfoPostCode {
  implicit val format: Format[ChargeInfoPostCode] = Json.valueFormat[ChargeInfoPostCode]
}

final case class CountryCode(value: String) extends AnyVal

object CountryCode {
  implicit val format: Format[CountryCode] = Json.valueFormat[CountryCode]
}

final case class PostCodeInfo(addressPostcode: ChargeInfoPostCode, postcodeDate: LocalDate)

object PostCodeInfo {
  implicit val format: OFormat[PostCodeInfo] = Json.format[PostCodeInfo]
}

final case class ChargeTypeAssessment(
  debtTotalAmount: BigInt,
  chargeReference: ChargeReference,
  parentChargeReference: Option[ChargeInfoParentChargeReference],
  mainTrans: MainTrans,
  charges: List[Charge]
)

object ChargeTypeAssessment {
  implicit val format: OFormat[ChargeTypeAssessment] =
    Json.format[ChargeTypeAssessment]
}

final case class ChargeReference(value: String) extends AnyVal

object ChargeReference {
  implicit val format: Format[ChargeReference] = Json.valueFormat[ChargeReference]
}

final case class ChargeInfoParentChargeReference(value: String) extends AnyVal

object ChargeInfoParentChargeReference {
  implicit val format: Format[ChargeInfoParentChargeReference] = Json.valueFormat[ChargeInfoParentChargeReference]
}

final case class MainTrans(value: String) extends AnyVal

object MainTrans {
  implicit val format: Format[MainTrans] = Json.valueFormat[MainTrans]
}

final case class Charge(
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
  originalCreationDate: Option[OriginalCreationDate],
  tieBreaker: Option[TieBreaker],
  originalTieBreaker: Option[OriginalTieBreaker],
  saTaxYearEnd: Option[SaTaxYearEnd],
  creationDate: Option[CreationDate],
  originalChargeType: Option[OriginalChargeType]
)

object Charge {
  implicit val format: OFormat[Charge] = Json.format[Charge]
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
