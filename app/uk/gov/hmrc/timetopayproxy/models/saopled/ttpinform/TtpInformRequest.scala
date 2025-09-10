package uk.gov.hmrc.timetopayproxy.models.saopled.ttpinform

import cats.data.NonEmptyList
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.timetopayproxy.models.{ChannelIdentifier, Identification}
import uk.gov.hmrc.timetopayproxy.models.saopled.common.{SaOpLedInstalment, TransitionedIndicator}
import uk.gov.hmrc.timetopayproxy.utils.json.CatsNonEmptyListJson

final case class TtpInformRequest(
  identifications: NonEmptyList[Identification],
  paymentPlan: TtpInformPaymentPlan,
  instalments: NonEmptyList[SaOpLedInstalment],
  channelIdentifier: ChannelIdentifier,
  transitioned: Option[TransitionedIndicator]
)

object TtpInformRequest {
  implicit val format: OFormat[TtpInformRequest] = {
    implicit def nelFormat[T: Format]: Format[NonEmptyList[T]] = CatsNonEmptyListJson.nonEmptyListFormat

    Json.format[TtpInformRequest]
  }
}