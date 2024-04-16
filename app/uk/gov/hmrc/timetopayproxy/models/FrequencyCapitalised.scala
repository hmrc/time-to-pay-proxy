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

sealed abstract class FrequencyCapitalised(override val entryName: String) extends EnumEntry

object FrequencyCapitalised extends Enum[FrequencyCapitalised] with PlayJsonEnum[FrequencyCapitalised] {
  val values: immutable.IndexedSeq[FrequencyCapitalised] = findValues

  case object Single extends FrequencyCapitalised("Single")
  case object Weekly extends FrequencyCapitalised("Weekly")
  case object TwoWeekly extends FrequencyCapitalised("2Weekly")
  case object FourWeekly extends FrequencyCapitalised("4Weekly")
  case object Monthly extends FrequencyCapitalised("Monthly")
  case object Quarterly extends FrequencyCapitalised("Quarterly")
  case object SixMonthly extends FrequencyCapitalised("6Monthly")
  case object Annually extends FrequencyCapitalised("Annually")

  implicit class ToLowerCaseConversion(val value: FrequencyCapitalised) extends AnyVal {
    def toFrequencyLowercase: FrequencyLowercase =
      value match {
        case Single     => FrequencyLowercase.Single
        case Weekly     => FrequencyLowercase.Weekly
        case TwoWeekly  => FrequencyLowercase.TwoWeekly
        case FourWeekly => FrequencyLowercase.FourWeekly
        case Monthly    => FrequencyLowercase.Monthly
        case Quarterly  => FrequencyLowercase.Quarterly
        case SixMonthly => FrequencyLowercase.SixMonthly
        case Annually   => FrequencyLowercase.Annually
      }
  }
}
