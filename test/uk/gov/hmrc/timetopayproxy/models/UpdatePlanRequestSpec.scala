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

package uk.gov.hmrc.timetopayproxy.models

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.libs.json.{ JsString, JsValue, Json }

class UpdatePlanRequestSpec extends AnyFreeSpecLike {
  "UpdatePlanRequest" - {
    "when all optional fields are populated" - {
      val updatePlanRequest: UpdatePlanRequest =
        UpdatePlanRequest(
          customerReference = CustomerReference("custRef123"),
          planId = PlanId("planId1"),
          updateType = UpdateType("paymentDetails"),
          channelIdentifier = Some(ChannelIdentifier.SelfService),
          planStatus = Some(PlanStatus.ResolvedCompleted),
          completeReason = Some(CompleteReason.PaymentInFullCaps),
          cancellationReason = Some(CancellationReason("debt-resolved")),
          thirdPartyBank = Some(true),
          payments = Some(List(PaymentInformation(PaymentMethod.BankPayments, Some(PaymentReference("paymentRef")))))
        )

      val updatePlanRequestJson: JsValue =
        Json.parse("""{
                     |  "customerReference": "custRef123",
                     |  "planId": "planId1",
                     |  "updateType": "paymentDetails",
                     |  "channelIdentifier": "selfService",
                     |  "planStatus": "Resolved - Completed",
                     |  "completeReason": "Payment in Full",
                     |  "cancellationReason": "debt-resolved",
                     |  "thirdPartyBank": true,
                     |  "payments":
                     |  [
                     |    {
                     |      "paymentMethod": "Bank payments",
                     |      "paymentReference": "paymentRef"
                     |    }
                     |  ]
                     |}""".stripMargin)

      "should be deserialized correctly" in {
        updatePlanRequestJson.as[UpdatePlanRequest] shouldBe updatePlanRequest
      }

      "should be serialized correctly" in {
        Json.toJson(updatePlanRequest) shouldBe updatePlanRequestJson
      }
    }

    "when all option fields are omitted" - {
      val updatePlanRequest: UpdatePlanRequest =
        UpdatePlanRequest(
          customerReference = CustomerReference("custRef123"),
          planId = PlanId("planId1"),
          updateType = UpdateType("paymentDetails"),
          channelIdentifier = None,
          planStatus = None,
          completeReason = None,
          cancellationReason = None,
          thirdPartyBank = None,
          payments = None
        )

      val updatePlanRequestJson: JsValue =
        Json.parse("""{
                     |  "customerReference": "custRef123",
                     |  "planId": "planId1",
                     |  "updateType": "paymentDetails"
                     |}""".stripMargin)

      "should be deserialized correctly" in {
        updatePlanRequestJson.as[UpdatePlanRequest] shouldBe updatePlanRequest
      }

      "should be serialized correctly" in {
        Json.toJson(updatePlanRequest) shouldBe updatePlanRequestJson
      }
    }

    "when updateType is 'planStatus'" - {
      "and 'planStatus' is not empty" - {
        "should deserialize correctly" in {
          val updatePlanRequestJson: JsValue =
            Json.parse("""{
                         |  "customerReference": "custRef123",
                         |  "planId": "planId1",
                         |  "updateType": "planStatus",
                         |  "planStatus": "Resolved - Completed"
                         |}""".stripMargin)

          val updatePlanRequest: UpdatePlanRequest =
            UpdatePlanRequest(
              customerReference = CustomerReference("custRef123"),
              planId = PlanId("planId1"),
              updateType = UpdateType("planStatus"),
              channelIdentifier = None,
              planStatus = Some(PlanStatus.ResolvedCompleted),
              completeReason = None,
              cancellationReason = None,
              thirdPartyBank = None,
              payments = None
            )

          updatePlanRequestJson.as[UpdatePlanRequest] shouldBe updatePlanRequest
        }
      }

      "and 'planStatus' is empty" - {
        "should throw an error when deserializing" in {
          val updatePlanRequestJson: JsValue =
            Json.parse("""{
                         |  "customerReference": "custRef123",
                         |  "planId": "planId1",
                         |  "updateType": "planStatus"
                         |}""".stripMargin)

          val deserializationError: IllegalArgumentException = intercept[IllegalArgumentException](
            updatePlanRequestJson.as[UpdatePlanRequest]
          )

          deserializationError.getMessage shouldBe
            "requirement failed: Invalid UpdatePlanRequest payload: Payload has a missing field or an invalid format. Field name: planStatus."
        }
      }
    }
  }

  "PaymentMethod" - {
    val paymentMethodMappings =
      Table(
        ("enum", "entryName"),
        PaymentMethod.DirectDebit  -> "directDebit",
        PaymentMethod.Bacs         -> "BACS",
        PaymentMethod.BankPayments -> "Bank payments",
        PaymentMethod.Cheque       -> "cheque",
        PaymentMethod.CardPayment  -> "cardPayment",
        PaymentMethod.OnGoingAward -> "Ongoing award"
      )

    "should be deserialized correctly" in {
      forAll(paymentMethodMappings) { case (paymentMethod, entryName) =>
        val paymentMethodJson: JsValue = JsString(entryName)

        paymentMethodJson.as[PaymentMethod] shouldBe paymentMethod
      }
    }

    "should be serialized correctly" in {
      forAll(paymentMethodMappings) { case (paymentMethod, entryName) =>
        val paymentMethodJson: JsValue = JsString(entryName)

        Json.toJson(paymentMethod) shouldBe paymentMethodJson
      }
    }

    ".values" - {
      val paymentMethodEnums: Seq[PaymentMethod] =
        Seq(
          PaymentMethod.DirectDebit,
          PaymentMethod.Bacs,
          PaymentMethod.BankPayments,
          PaymentMethod.Cheque,
          PaymentMethod.CardPayment,
          PaymentMethod.OnGoingAward
        )

      "should return the list of enums" in {
        PaymentMethod.values shouldBe paymentMethodEnums
      }
    }
  }
}
