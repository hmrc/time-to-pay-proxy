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

import enumeratum.{ Enum, EnumEntry, PlayJsonEnum }

import scala.collection.immutable

sealed abstract class RegimeType(override val entryName: String) extends EnumEntry

object RegimeType extends Enum[RegimeType] with PlayJsonEnum[RegimeType] {
  val values: immutable.IndexedSeq[RegimeType] = findValues

  case object PAYE extends RegimeType("PAYE")
  case object VATC extends RegimeType("VATC")
  case object SIMP extends RegimeType("SIMP")
  case object SA extends RegimeType("SA")
}
