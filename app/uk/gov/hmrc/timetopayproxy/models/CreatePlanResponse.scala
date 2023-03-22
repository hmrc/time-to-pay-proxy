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
import play.api.libs.json.Json

final case class CaseId(value: String) extends AnyVal

object CaseId extends ValueTypeFormatter {
  implicit val format =
    valueTypeFormatter(CaseId.apply, CaseId.unapply)
}

sealed abstract class PlanStatus(override val entryName: String) extends EnumEntry

object PlanStatus extends Enum[PlanStatus] with PlayJsonEnum[PlanStatus] {
  val values: scala.collection.immutable.IndexedSeq[PlanStatus] = findValues

  case object Success extends PlanStatus("success")
  case object Failure extends PlanStatus("failure")
  case object TtpArrangementInProgress extends PlanStatus("TTP Arrangement - In Progress")
  case object ResolvedTTPAmended extends PlanStatus("Resolved - TTP Amended")
  case object InDefaultClericalReview extends PlanStatus("In Default - Clerical Review")
  case object PendingFirstReminder extends PlanStatus("Pending - First Reminder")
  case object InDefaultFirstReminder extends PlanStatus("In Default - First Reminder")
  case object PendingSecondReminder extends PlanStatus("Pending - Second Reminder")
  case object InDefaultSecondReminder extends PlanStatus("In Default - Second Reminder")
  case object PendingCancellation extends PlanStatus("Pending - Cancellation")
  case object PendingCompletion extends PlanStatus("Pending - Completion")
  case object ResolvedCancelled extends PlanStatus("Resolved - Cancelled")
  case object ResolvedCompleted extends PlanStatus("Resolved - Completed")
  case object MonitoringSuspended extends PlanStatus("TTP - Monitoring suspended")
  case object PendingCancellationLetter extends PlanStatus("Pending - Cancellation Letter")

}

final case class CreatePlanResponse(
  customerReference: CustomerReference,
  planId: PlanId,
  caseId: CaseId,
  planStatus: PlanStatus
)

object CreatePlanResponse {
  implicit val format = Json.format[CreatePlanResponse]
}
