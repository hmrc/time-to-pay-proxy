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

package uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform

import cats.data.NonEmptyList
import play.api.libs.json.{ Format, Json, OFormat, OWrites, Reads }
import uk.gov.hmrc.timetopayproxy.config.FeatureSwitch
import uk.gov.hmrc.timetopayproxy.models.saonly.common.{ SaOnlyInstalment, SaOnlyPaymentPlan, SaOnlyPaymentPlanR2, TransitionedIndicator }
import uk.gov.hmrc.timetopayproxy.models.{ ChannelIdentifier, Identification }
import uk.gov.hmrc.timetopayproxy.utils.json.CatsNonEmptyListJson

sealed trait InformRequest

final case class TtpInformRequest(
  identifications: NonEmptyList[Identification],
  paymentPlan: SaOnlyPaymentPlan,
  instalments: NonEmptyList[SaOnlyInstalment],
  channelIdentifier: ChannelIdentifier,
  // Unlike the FullAmend request, CDCS requested this field to be optional due to delivery constraints.
  transitioned: Option[TransitionedIndicator]
) extends InformRequest

final case class TtpInformRequestR2(
  identifications: NonEmptyList[Identification],
  paymentPlan: SaOnlyPaymentPlanR2,
  instalments: NonEmptyList[SaOnlyInstalment],
  channelIdentifier: ChannelIdentifier,
  // Unlike the FullAmend request, CDCS requested this field to be optional due to delivery constraints.
  transitioned: Option[TransitionedIndicator]
) extends InformRequest

object InformRequest {
  private def reads(featureSwitch: FeatureSwitch): Reads[InformRequest] = Reads { jsValue =>
    if (featureSwitch.saRelease2Enabled.enabled) TtpInformRequestR2.r2Format.reads(jsValue)
    else TtpInformRequest.r1Format.reads(jsValue)
  }

  private val writes: OWrites[InformRequest] = OWrites {
    case r1: TtpInformRequest   => TtpInformRequest.r1Format.writes(r1)
    case r2: TtpInformRequestR2 => TtpInformRequestR2.r2Format.writes(r2)
  }

  def format(featureSwitch: FeatureSwitch): OFormat[InformRequest] = OFormat(reads(featureSwitch), writes)
}

object TtpInformRequest {
  private implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat
  val r1Format: OFormat[TtpInformRequest] = Json.format[TtpInformRequest]
}

object TtpInformRequestR2 {
  private implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat
  val r2Format: OFormat[TtpInformRequestR2] = Json.format[TtpInformRequestR2]
}
