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

sealed abstract class FrequencyLowercase(override val entryName: String) extends EnumEntry

object FrequencyLowercase extends Enum[FrequencyLowercase] with PlayJsonEnum[FrequencyLowercase] {
  val values: immutable.IndexedSeq[FrequencyLowercase] = findValues

  case object Single extends FrequencyLowercase("single")
  case object Weekly extends FrequencyLowercase("weekly")
  case object TwoWeekly extends FrequencyLowercase("2Weekly")
  case object FourWeekly extends FrequencyLowercase("4Weekly")
  case object Monthly extends FrequencyLowercase("monthly")
  case object Quarterly extends FrequencyLowercase("quarterly")
  case object SixMonthly extends FrequencyLowercase("6Monthly")
  case object Annually extends FrequencyLowercase("annually")
}
