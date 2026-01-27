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

sealed trait ChargeTypeAssessment

final case class ChargeTypeAssessmentR1(
  debtTotalAmount: BigInt,
  chargeReference: ChargeReference,
  parentChargeReference: Option[ChargeInfoParentChargeReference],
  mainTrans: MainTrans,
  charges: List[ChargeR1]
) extends ChargeTypeAssessment

object ChargeTypeAssessmentR1 {
  implicit val format: OFormat[ChargeTypeAssessmentR1] =
    Json.format[ChargeTypeAssessmentR1]
}

final case class ChargeTypeAssessmentR2(
  debtTotalAmount: BigInt,
  chargeReference: ChargeReference,
  parentChargeReference: Option[ChargeInfoParentChargeReference],
  mainTrans: MainTrans,
  charges: List[ChargeR2],
  isInsolvent: IsInsolvent
) extends ChargeTypeAssessment

object ChargeTypeAssessmentR2 {
  implicit val format: OFormat[ChargeTypeAssessmentR2] =
    Json.format[ChargeTypeAssessmentR2]
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

final case class IsInsolvent(value: Boolean) extends AnyVal

object IsInsolvent {
  implicit val format: Format[IsInsolvent] = Json.valueFormat[IsInsolvent]
}
