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

package uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi

import enumeratum.{ Enum, EnumEntry, PlayJsonEnum }
import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.timetopayproxy.config.FeatureSwitch
import uk.gov.hmrc.timetopayproxy.models.{ ChargeTypesExcluded, Identification }

import java.time.{ LocalDate, LocalDateTime }
import scala.collection.immutable

sealed trait ChargeInfoResponse

object ChargeInfoResponse {
  def reads(featureSwitch: FeatureSwitch): Reads[ChargeInfoResponse] = Reads { jsValue =>
    if (featureSwitch.saRelease2Enabled.enabled) ChargeInfoResponseR2.format.reads(jsValue)
    else ChargeInfoResponseR1.format.reads(jsValue)
  }

  implicit val writes: OWrites[ChargeInfoResponse] = OWrites {
    case r1: ChargeInfoResponseR1 => ChargeInfoResponseR1.format.writes(r1)
    case r2: ChargeInfoResponseR2 => ChargeInfoResponseR2.format.writes(r2)
  }

  def format(featureSwitch: FeatureSwitch): OFormat[ChargeInfoResponse] = OFormat.apply(reads(featureSwitch), writes)
}

final case class ChargeInfoResponseR1(
  processingDateTime: LocalDateTime,
  identification: List[Identification],
  individualDetails: IndividualDetails,
  addresses: List[Address],
  chargeTypeAssessment: List[ChargeTypeAssessmentR1]
) extends ChargeInfoResponse

object ChargeInfoResponseR1 {
  private val reads: Reads[ChargeInfoResponseR1] = Json.reads[ChargeInfoResponseR1]
  private val writes: OWrites[ChargeInfoResponseR1] =
    (
      (__ \ "processingDateTime").write[LocalDateTime] and
        (__ \ "identification").write[List[Identification]] and
        (__ \ "individualDetails").write[IndividualDetails] and
        (__ \ "addresses").write[List[Address]] and
        (__ \ "chargeTypeAssessment").write[List[ChargeTypeAssessmentR1]]
    )(chargeInfoR1 =>
      (
        chargeInfoR1.processingDateTime,
        chargeInfoR1.identification,
        chargeInfoR1.individualDetails,
        chargeInfoR1.addresses,
        chargeInfoR1.chargeTypeAssessment
      )
    )

  implicit val format: OFormat[ChargeInfoResponseR1] = OFormat[ChargeInfoResponseR1](reads, writes)
}

final case class ChargeInfoResponseR2(
  processingDateTime: LocalDateTime,
  identification: List[Identification],
  individualDetails: IndividualDetails,
  addresses: List[Address],
  customerSignals: Option[List[Signal]],
  chargeTypeAssessment: List[ChargeTypeAssessmentR2],
  chargeTypesExcluded: ChargeTypesExcluded
) extends ChargeInfoResponse

object ChargeInfoResponseR2 {
  implicit val format: OFormat[ChargeInfoResponseR2] = Json.format[ChargeInfoResponseR2]
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
