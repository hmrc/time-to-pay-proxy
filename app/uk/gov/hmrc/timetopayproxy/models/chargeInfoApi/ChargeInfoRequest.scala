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

import scala.collection.immutable

final case class ChargeInfoRequest(
  channelIdentifier: ChargeInfoChannelIdentifier,
  identifications: List[Identification],
  regimeType: RegimeType
)

object ChargeInfoRequest {
  implicit val format: OFormat[ChargeInfoRequest] = Json.format[ChargeInfoRequest]
}

final case class ChargeInfoChannelIdentifier(channelIdentifier: String) extends AnyVal

object ChargeInfoChannelIdentifier {
  implicit val format: Format[ChargeInfoChannelIdentifier] = Json.valueFormat[ChargeInfoChannelIdentifier]
}

sealed abstract class RegimeType(override val entryName: String) extends EnumEntry

object RegimeType extends Enum[RegimeType] with PlayJsonEnum[RegimeType] {
  val values: immutable.IndexedSeq[RegimeType] = findValues.distinct

  type SA = SA.type
  case object SA extends RegimeType("SA")
}
