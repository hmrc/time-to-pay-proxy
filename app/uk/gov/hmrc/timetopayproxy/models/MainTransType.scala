/*
 * Copyright 2022 HM Revenue & Customs
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

sealed abstract class MainTransType(override val entryName: String) extends EnumEntry

object MainTransType extends Enum[MainTransType] with PlayJsonEnum[MainTransType] {
  val values: immutable.IndexedSeq[MainTransType] = findValues

  case object ChBDebt extends MainTransType("5330")
  case object ChBMigratedDebt extends MainTransType("5350")
  case object DrierIt extends MainTransType("1085")
  case object TPSSAFT extends MainTransType("1511")
  case object TPSSFailureToSubmit extends MainTransType("1515")
  case object TPSSPenalty extends MainTransType("1520")
  case object TPSSAccTaxAssessment extends MainTransType("1525")
  case object TPSSAFTAssessment extends MainTransType("1526")
  case object TPSSSchemaSanction extends MainTransType("1530")
  case object TPSSSchemaSanctionNT extends MainTransType("1531")
  case object TPSSLumpSum extends MainTransType("1535")
  case object TPSSLumpSumINT extends MainTransType("1536")
  case object TPSSUnreportedLiability extends MainTransType("1540")
  case object TPSSUnreportedLiabilityINT extends MainTransType("1541")
  case object TPSSContractSettlement extends MainTransType("1545")
  case object TPSSContractSettlementINT extends MainTransType("1546")
  case object TCOGFee extends MainTransType("2421")
  case object NIDistraintCosts extends MainTransType("1441")

  case object ApprenticeshipLevyCharge extends MainTransType("2006")
  case object ApprenticeshipLevyInterest extends MainTransType("2007")
  case object Class1ANICsCharge extends MainTransType("2060")
  case object EOYNonRTIManualCharge extends MainTransType("2090")
  case object EOYNonRTIManualChargeInt extends MainTransType("2095")
  case object EmploymentAllowance extends MainTransType("2130")
  case object InYearCISRCharge extends MainTransType("2040")
  case object InYearRTIChargeEPS extends MainTransType("2030")
  case object InYearRTIChargeFPS extends MainTransType("2000")
  case object IntEmploymentAllowance extends MainTransType("2135")
  case object InterestOnClass1A extends MainTransType("2065")
  case object InterestOnInYearCISR extends MainTransType("2045")
  case object PAYEEYU extends MainTransType("2100")
  case object PAYEEYUInterest extends MainTransType("2105")
  case object RTIInterest extends MainTransType("2005")

  case object VATReturnCharge extends MainTransType("4700")
  case object VATOfficersAssessment extends MainTransType("4730")
  case object VATErrorCorrection extends MainTransType("4731")
  case object VATProtectiveAssessment extends MainTransType("4733")
  case object VATBNPOfRegPost2010 extends MainTransType("4760")
  case object VATFTNMatChangePre2010 extends MainTransType("4763")
  case object VATFTNMatChangePost2010 extends MainTransType("4766")
  case object VATCivilEvasionPenalty extends MainTransType("4745")
  case object VATInaccuraciesInECSales extends MainTransType("4770")
  case object VATFailureToSubmitECSales extends MainTransType("4773")
  case object VATFTNEachPartner extends MainTransType("4776")
  case object VATOAInaccuraciesFrom2009 extends MainTransType("4780")
  case object VATInaccuracyAssessmentsPen extends MainTransType("4755")
  case object VATInaccuracyReturnReplaced extends MainTransType("4783")
  case object VATBNPOfRegPre2010 extends MainTransType("4786")
  case object VATWrongDoingPenalty extends MainTransType("4765")
  case object VATCarterPenalty extends MainTransType("4775")
  case object VATFTNRCSL extends MainTransType("4790")
  case object VATFailureToSubmitRCSL extends MainTransType("4793")
  case object VATMPPre2009 extends MainTransType("4796")
  case object VATMPRPre2009 extends MainTransType("4799")
  case object VATDefaultSurcharge extends MainTransType("4747")
  case object VATOADefaultInterest extends MainTransType("4705")
  case object VATECDefaultInterest extends MainTransType("4706")
  case object VATAADefaultInterest extends MainTransType("4707")
  case object VATPADefaultInterest extends MainTransType("4708")
  case object VATIndirectTaxRevenueRec extends MainTransType("4711")
  case object VATDefaultInterest extends MainTransType("4721")
  case object VATMigReturnCharge extends MainTransType("7700")
  case object VATMigOfficersAssessment extends MainTransType("7730")
  case object VATMigErrorCorrection extends MainTransType("7731")
  case object VATMigDefaultInterest extends MainTransType("7721")
  case object VATMigDefaultSurcharge extends MainTransType("7747")
  case object VATMigBNPOfRegPost2010 extends MainTransType("7760")
  case object VATMigFTNMatChgPost2010 extends MainTransType("7766")
  case object VATMigMiscellaneousPenalty extends MainTransType("7735")
  case object VATMigCivilEvasionPenalty extends MainTransType("7745")
  case object VATMigFTNEachPartner extends MainTransType("7776")
  case object VATMigInaccuracyAsstsPen extends MainTransType("7755")
  case object VATMigInaccReturnReplaced extends MainTransType("7783")
  case object VATMigBNPOfRegPre2010 extends MainTransType("7786")
  case object VATMigWrongDoingPenalty extends MainTransType("7765")
  case object VATMigratedCarterPenalty extends MainTransType("7775")
  case object VATMigMPPre2009 extends MainTransType("7796")
  case object VATMigMPRPre2009 extends MainTransType("7799")
  case object VATLateSubmissionPen extends MainTransType("4748")
  case object VATReturn1stLPP extends MainTransType("4703")
  case object VATReturn2ndLPP extends MainTransType("4704")
  case object VATOA1stLPP extends MainTransType("4741")
  case object VATOA2ndLPP extends MainTransType("4742")
  case object VATErrorCorrection1stLPP extends MainTransType("4743")
  case object VATErrorCorrection2ndLPP extends MainTransType("4744")
  case object VATPA1stLPP extends MainTransType("4761")
  case object VATPA2ndLPP extends MainTransType("4762")
  case object VATLSPInterest extends MainTransType("4749")
  case object VATReturnLPI extends MainTransType("4620")
  case object VATReturn1stLPPLPI extends MainTransType("4622")
  case object VATReturn2ndLPPLPI extends MainTransType("4624")
  case object VATIndirectTaxRevRecLPI extends MainTransType("4638")
  case object VATOfficersAssessmentLPI extends MainTransType("4658")
  case object VATOA1stLPPLPI extends MainTransType("4660")
  case object VATOA2ndLPPLPI extends MainTransType("4662")
  case object VATErrorCorrectionLPI extends MainTransType("4664")
  case object VATErrorCorrect1stLPPLPI extends MainTransType("4666")
  case object VATErrorCorrect2ndLPPLPI extends MainTransType("4668")
  case object VATProtectiveAssessmentLPI extends MainTransType("4676")
  case object VATPA1stLPPLPI extends MainTransType("4678")
  case object VATPA2ndLPPLPI extends MainTransType("4680")
  case object VATMiscellaneousPenaltyLPI extends MainTransType("4682")
  case object VATCivilEvasionPenaltyLPI extends MainTransType("4684")
  case object VATInaccuracyAssessPenLPI extends MainTransType("4687")
  case object VATBNPOfPostReg2010LPI extends MainTransType("4693")
  case object VATWrongDoingPenaltyLPI extends MainTransType("4695")
  case object VATFTNMatChgPost2010LPI extends MainTransType("4767")
  case object VATCarterPenaltyLPI extends MainTransType("4697")
  case object VATFTNEachPartnerLPI extends MainTransType("4777")
  case object VATOAInaccurFrom2009LPI extends MainTransType("4781")
  case object VATInaccRtnReplacedLPI extends MainTransType("4784")
  case object VATFTNRCSLLPI extends MainTransType("4791")
  case object VATFailureToSubmitRCSLLPI extends MainTransType("4794")
  case object VATRPIRecovery extends MainTransType("4797")
}
