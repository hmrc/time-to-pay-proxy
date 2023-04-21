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

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable

sealed abstract class SubTransType(override val entryName: String) extends EnumEntry

object SubTransType extends Enum[SubTransType] with PlayJsonEnum[SubTransType] {
  val values: immutable.IndexedSeq[SubTransType] = findValues

  case object ChBDebt extends SubTransType("7006")
  case object GuardiansGBDebt extends SubTransType("7010")
  case object GuardiansNIDebt extends SubTransType("7011")
  case object ChBMigratedDebt extends SubTransType("7012")
  case object GuardiansGBChBMigratedDebt extends SubTransType("7014")
  case object GuardiansNIChBMigratedDebt extends SubTransType("7013")
  case object IT extends SubTransType("1000")
  case object NICGB extends SubTransType("1020")
  case object NICNI extends SubTransType("1025")
  case object HIPG extends SubTransType("1180")
  case object INTIT extends SubTransType("2000")
  case object TGPEN extends SubTransType("1090")
  case object TakingControlFee extends SubTransType("1150")

  case object EOYNonRTIEyerNIC1GB extends SubTransType("1023")
  case object EOYNonRTIEyeeNIC1GB extends SubTransType("1026")
  case object InYearCl1ANIC extends SubTransType("1030")
  case object EOYNonRTISL extends SubTransType("1100")
  case object ApprenticeshipLevy extends SubTransType("1106")
  case object INTApprenticehipLevy extends SubTransType("1107")
  case object EOYNonRTICISDedSuffered extends SubTransType("1250")
  case object EOYNonRTINICHoliday extends SubTransType("1260")
  case object EOYNonRTISPP extends SubTransType("1270")
  case object EOYNonRTISAP extends SubTransType("1280")
  case object EOYNonRTIShPP extends SubTransType("1290")
  case object EOYNonRTISSP extends SubTransType("1300")
  case object EOYNonRTISMP extends SubTransType("1310")
  case object EOYNonRTINICComponSMP extends SubTransType("1320")
  case object EOYNonRTINICComponSPP extends SubTransType("1330")
  case object EOYNonRTINICComponSAP extends SubTransType("1340")
  case object EOYNonRTINICComponShPP extends SubTransType("1350")
  case object EmploymentAllowance extends SubTransType("1355")
  case object SPBP extends SubTransType("1390")
  case object SPBPandNIcomponSPBP extends SubTransType("1395")
  case object EOYNonRTINIC1GBINT extends SubTransType("2020")
  case object EOYNonRTIEYerNIC1GBINT extends SubTransType("2023")
  case object EOYNonRTIEYeeNIC1GBINT extends SubTransType("2026")
  case object InterestOnInyearCl1ANIC extends SubTransType("2030")
  case object EOYNonRTIINTSL extends SubTransType("2100")
  case object INTEMPAllowance extends SubTransType("2355")

  case object VATDebitCharge extends SubTransType("1174")
  case object VATInterest extends SubTransType("1175")

  case object OFP extends SubTransType("1091")
  case object IBFInterestOnly extends SubTransType("2091")

}
