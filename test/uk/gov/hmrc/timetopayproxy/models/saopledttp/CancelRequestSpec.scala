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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._
import uk.gov.hmrc.timetopayproxy.models.common._

import java.time.LocalDate

class CancelRequestSpec extends AnyWordSpec with Matchers {

  private val identification = Identification(IdType("UTR"), IdValue("1234567890"))
  private val paymentPlan = CancelPaymentPlan(
    arrangementAgreedDate = ArrangementAgreedDate(LocalDate.of(2025, 1, 1)),
    ttpEndDate = TtpEndDate(LocalDate.of(2025, 12, 31)),
    frequency = FrequencyLowercase.Monthly,
    cancellationDate = CancellationDate(LocalDate.of(2025, 6, 15)),
    initialPaymentDate = Some(InitialPaymentDate(LocalDate.of(2025, 2, 1))),
    initialPaymentAmount = Some(GbpPounds.createOrThrow(BigDecimal("150.00")))
  )
  private val instalment = SaOpLedInstalment(
    dueDate = InstalmentDueDate(LocalDate.of(2025, 3, 1)),
    amountDue = GbpPounds.createOrThrow(BigDecimal("300.00"))
  )

  private val cancelRequest = CancelRequest(
    identifications = NonEmptyList.one(identification),
    paymentPlan = paymentPlan,
    instalments = NonEmptyList.one(instalment),
    channelIdentifier = ChannelIdentifier.SelfService,
    transitioned = Some(TransitionedIndicator(true))
  )

  "CancelRequest" when {
    "serializing to JSON" should {
      "produce correct JSON structure" in {
        val json = Json.toJson(cancelRequest)

        (json \ "identifications").as[JsArray].value should have length 1
        (json \ "identifications" \ 0 \ "idType").as[String] shouldBe "UTR"
        (json \ "identifications" \ 0 \ "idValue").as[String] shouldBe "1234567890"

        (json \ "paymentPlan" \ "arrangementAgreedDate").as[String] shouldBe "2025-01-01"
        (json \ "paymentPlan" \ "ttpEndDate").as[String] shouldBe "2025-12-31"
        (json \ "paymentPlan" \ "frequency").as[String] shouldBe "monthly"
        (json \ "paymentPlan" \ "cancellationDate").as[String] shouldBe "2025-06-15"
        (json \ "paymentPlan" \ "initialPaymentDate").as[String] shouldBe "2025-02-01"
        (json \ "paymentPlan" \ "initialPaymentAmount").as[BigDecimal] shouldBe BigDecimal("150.00")

        (json \ "instalments").as[JsArray].value should have length 1
        (json \ "instalments" \ 0 \ "dueDate").as[String] shouldBe "2025-03-01"
        (json \ "instalments" \ 0 \ "amountDue").as[BigDecimal] shouldBe BigDecimal("300.00")

        (json \ "channelIdentifier").as[String] shouldBe "selfService"
        (json \ "transitioned").as[Boolean] shouldBe true
      }
    }

    "deserializing from JSON" should {
      "parse valid JSON correctly" in {
        val json = Json.obj(
          "identifications" -> Json.arr(
            Json.obj(
              "idType"  -> "UTR",
              "idValue" -> "1234567890"
            )
          ),
          "paymentPlan" -> Json.obj(
            "arrangementAgreedDate" -> "2025-01-01",
            "ttpEndDate"            -> "2025-12-31",
            "frequency"             -> "monthly",
            "cancellationDate"      -> "2025-06-15",
            "initialPaymentDate"    -> "2025-02-01",
            "initialPaymentAmount"  -> 150.00
          ),
          "instalments" -> Json.arr(
            Json.obj(
              "dueDate"   -> "2025-03-01",
              "amountDue" -> 300.00
            )
          ),
          "channelIdentifier" -> "selfService",
          "transitioned"      -> true
        )

        val result = json.validate[CancelRequest]
        result shouldBe a[JsSuccess[_]]

        val request = result.get
        request.identifications.head shouldBe identification
        request.paymentPlan.frequency shouldBe FrequencyLowercase.Monthly
        request.channelIdentifier shouldBe ChannelIdentifier.SelfService
      }

      "fail validation for empty arrays" in {
        val json = Json.obj(
          "identifications" -> Json.arr(),
          "paymentPlan" -> Json.obj(
            "arrangementAgreedDate" -> "2025-01-01",
            "ttpEndDate"            -> "2025-12-31",
            "frequency"             -> "monthly",
            "cancellationDate"      -> "2025-06-15"
          ),
          "instalments" -> Json.arr(
            Json.obj(
              "dueDate"   -> "2025-03-01",
              "amountDue" -> 300.00
            )
          ),
          "channelIdentifier" -> "selfService"
        )

        val result = json.validate[CancelRequest]
        result shouldBe a[JsError]
      }
    }
  }
}
