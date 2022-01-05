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

/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.Json

import scala.collection.immutable

sealed abstract class ChannelIdentifier(override val entryName: String) extends EnumEntry

object ChannelIdentifier extends Enum[ChannelIdentifier] with PlayJsonEnum[ChannelIdentifier] {
  val values: immutable.IndexedSeq[ChannelIdentifier] = findValues

  case object Advisor extends ChannelIdentifier("advisor")
  case object SelfService extends ChannelIdentifier("selfService")
}

final case class PlanToGenerateQuote(
                      quoteType: QuoteType,
                      quoteDate: LocalDate,
                      instalmentStartDate: LocalDate,
                      instalmentAmount: Option[BigDecimal],
                      frequency: Option[Frequency],
                      duration: Option[Duration],
                      initialPaymentAmount: Option[BigDecimal],
                      initialPaymentDate: Option[LocalDate],
                      paymentPlanType: PaymentPlanType
                     ) {
  require(instalmentAmount.forall(_ > 0), "instalmentAmount should be a positive amount.")
  require(initialPaymentAmount.forall(_ > 0), "initialPaymentAmount should be a positive amount.")
}

object PlanToGenerateQuote {
  implicit val format = Json.format[PlanToGenerateQuote]
}


final case class GenerateQuoteRequest(
                             customerReference: CustomerReference,
                             channelIdentifier: ChannelIdentifier,
                             plan: PlanToGenerateQuote,
                             customerPostCodes: List[CustomerPostCode],
                             debtItemCharges: List[DebtItemCharge]) {
  require(!customerReference.value.trim().isEmpty(), "customerReference should not be empty")
}


object GenerateQuoteRequest {
  implicit val format = Json.format[GenerateQuoteRequest]
}


