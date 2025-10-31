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

package uk.gov.hmrc.timetopayproxy.models.saonly.fullAmend

import cats.data.NonEmptyList
import play.api.libs.json.{ Format, Json, OFormat }
import uk.gov.hmrc.timetopayproxy.models.saonly.common.{ SaOnlyInstalment, TransitionedIndicator, TtpPaymentPlan }
import uk.gov.hmrc.timetopayproxy.models.{ ChannelIdentifier, Identification }
import uk.gov.hmrc.timetopayproxy.utils.json.CatsNonEmptyListJson

case class FullAmendRequest(
  identifications: NonEmptyList[Identification],
  paymentPlan: TtpPaymentPlan,
  instalments: NonEmptyList[SaOnlyInstalment],
  channelIdentifier: ChannelIdentifier,
  transitioned: TransitionedIndicator
)
object FullAmendRequest {
  implicit val format: OFormat[FullAmendRequest] = {
    implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat[T]

    Json.format[FullAmendRequest]
  }
}
