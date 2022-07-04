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

import java.time.LocalDate
import play.api.libs.json.Json

sealed abstract class UpdatePlanStatus(override val entryName: String) extends EnumEntry

object UpdatePlanStatus extends Enum[UpdatePlanStatus] with PlayJsonEnum[UpdatePlanStatus] {
  val values: scala.collection.immutable.IndexedSeq[UpdatePlanStatus] = findValues

  case object Complete extends UpdatePlanStatus("complete")
  case object Cancelled extends UpdatePlanStatus("cancelled")

  def valueOf(value: String): UpdatePlanStatus =
    value match {
      case "complete"  => Complete
      case "cancelled" => Cancelled
    }

}

final case class UpdatePlanResponse(
  customerReference: CustomerReference,
  planId: PlanId,
  planStatus: UpdatePlanStatus,
  planUpdatedDate: LocalDate
)

object UpdatePlanResponse {
  implicit val format = Json.format[UpdatePlanResponse]
}
