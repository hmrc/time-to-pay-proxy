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

package uk.gov.hmrc.timetopayproxy.models.saopledttp

import cats.data.NonEmptyList
import play.api.libs.json.{ Json, OFormat }
import uk.gov.hmrc.timetopayproxy.models.common._

final case class CancelPaymentPlan(
  arrangementAgreedDate: ArrangementAgreedDate,
  ttpEndDate: TtpEndDate,
  frequency: FrequencyLowercase,
  cancellationDate: CancellationDate,
  initialPaymentDate: Option[InitialPaymentDate],
  initialPaymentAmount: Option[GbpPounds]
)

final case class CancelRequest(
  identifications: NonEmptyList[Identification],
  paymentPlan: CancelPaymentPlan,
  instalments: NonEmptyList[SaOpLedInstalment],
  channelIdentifier: ChannelIdentifier,
  transitioned: Option[TransitionedIndicator]
)

object CancelPaymentPlan {
  implicit val format: OFormat[CancelPaymentPlan] = Json.format[CancelPaymentPlan]
}

object CancelRequest {

  // Custom JSON format to handle NonEmptyList serialization
  import play.api.libs.json._

  implicit val nelIdentificationFormat: Format[NonEmptyList[Identification]] =
    new Format[NonEmptyList[Identification]] {
      def reads(json: JsValue): JsResult[NonEmptyList[Identification]] = json match {
        case JsArray(values) if values.nonEmpty =>
          // Manual traverse for JsResult
          val validatedList = values.foldLeft[JsResult[List[Identification]]](JsSuccess(List.empty)) { (acc, jsValue) =>
            acc.flatMap { list =>
              jsValue.validate[Identification].map(id => list :+ id)
            }
          }
          validatedList.flatMap { identifications =>
            NonEmptyList.fromList(identifications) match {
              case Some(nel) => JsSuccess(nel)
              case None      => JsError("List cannot be empty")
            }
          }
        case JsArray(_) => JsError("List cannot be empty")
        case _          => JsError("Expected array")
      }

      def writes(nel: NonEmptyList[Identification]): JsValue = Json.toJson(nel.toList)
    }

  implicit val nelInstalmentFormat: Format[NonEmptyList[SaOpLedInstalment]] =
    new Format[NonEmptyList[SaOpLedInstalment]] {
      def reads(json: JsValue): JsResult[NonEmptyList[SaOpLedInstalment]] = json match {
        case JsArray(values) if values.nonEmpty =>
          // Manual traverse for JsResult
          val validatedList = values.foldLeft[JsResult[List[SaOpLedInstalment]]](JsSuccess(List.empty)) {
            (acc, jsValue) =>
              acc.flatMap { list =>
                jsValue.validate[SaOpLedInstalment].map(instalment => list :+ instalment)
              }
          }
          validatedList.flatMap { instalments =>
            NonEmptyList.fromList(instalments) match {
              case Some(nel) => JsSuccess(nel)
              case None      => JsError("List cannot be empty")
            }
          }
        case JsArray(_) => JsError("List cannot be empty")
        case _          => JsError("Expected array")
      }

      def writes(nel: NonEmptyList[SaOpLedInstalment]): JsValue = Json.toJson(nel.toList)
    }

  implicit val format: OFormat[CancelRequest] = Json.format[CancelRequest]
}
