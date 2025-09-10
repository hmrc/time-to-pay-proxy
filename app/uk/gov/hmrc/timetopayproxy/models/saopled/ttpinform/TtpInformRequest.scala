package uk.gov.hmrc.timetopayproxy.models.saopled.ttpinform

import cats.data.NonEmptyList
import uk.gov.hmrc.timetopayproxy.models.{ChannelIdentifier, Identification}
import uk.gov.hmrc.timetopayproxy.models.saopled.common.{SaOpLedInstalment, TransitionedIndicator}

final case class TtpInformRequest(
  identifications: NonEmptyList[Identification],
  paymentPlan: TtpInformPaymentPlan,
  instalments: NonEmptyList[SaOpLedInstalment],
  channelIdentifier: ChannelIdentifier,
  transitioned: Option[TransitionedIndicator]
)
