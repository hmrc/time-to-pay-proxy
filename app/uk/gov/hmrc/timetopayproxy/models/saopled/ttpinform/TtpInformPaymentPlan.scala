package uk.gov.hmrc.timetopayproxy.models.saopled.ttpinform

import uk.gov.hmrc.timetopayproxy.models.FrequencyLowercase
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPoundsUnchecked
import uk.gov.hmrc.timetopayproxy.models.saopled.common.{ArrangementAgreedDate, InitialPaymentDate, TtpEndDate}

final case class TtpInformPaymentPlan(
  arrangementAgreedDate: ArrangementAgreedDate,
  ttpEndDate: TtpEndDate,
  frequency: FrequencyLowercase,
  initialPaymentDate: Option[InitialPaymentDate],
  initialPaymentAmount: Option[GbpPoundsUnchecked],
  ddiReference: Option[DdiReference]
)
