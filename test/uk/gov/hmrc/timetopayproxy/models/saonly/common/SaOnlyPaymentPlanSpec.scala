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

package uk.gov.hmrc.timetopayproxy.models.saonly.common

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsNumber, JsResultException, Json }
import uk.gov.hmrc.timetopayproxy.models.FrequencyLowercase
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds

import java.time.LocalDate

class SaOnlyPaymentPlanSpec extends AnyFreeSpec {

  "SaOnlyPaymentPlan" - {
    "should encode and decode correctly" - {
      "with no optional values present" in {
        val planWithNoOptionalFields = SaOnlyPaymentPlan(
          arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2025-05-05")),
          ttpEndDate = TtpEndDate(LocalDate.parse("2026-05-05")),
          frequency = FrequencyLowercase.Monthly,
          initialPaymentDate = None,
          initialPaymentAmount = None,
          ddiReference = None
        )

        val jsonPlanWithNoOptionalFields = Json.parse(
          """
            |{
            |  "arrangementAgreedDate": "2025-05-05",
            |  "ttpEndDate": "2026-05-05",
            |  "frequency": "monthly"
            |}
            |""".stripMargin
        )

        Json.toJson(planWithNoOptionalFields) shouldBe jsonPlanWithNoOptionalFields
        jsonPlanWithNoOptionalFields.as[SaOnlyPaymentPlan] shouldBe planWithNoOptionalFields
      }

      "with all optional values present" in {
        val planWithAllOptionalFields = SaOnlyPaymentPlan(
          arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2025-05-05")),
          ttpEndDate = TtpEndDate(LocalDate.parse("2026-05-05")),
          frequency = FrequencyLowercase.Monthly,
          initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2025-05-05"))),
          initialPaymentAmount = Some(GbpPounds.createOrThrow(BigDecimal(123.45))),
          ddiReference = Some(DdiReference("Test DDI Reference"))
        )

        val jsonPlanWithAllOptionalFields = Json.parse(
          """
            |{
            |  "arrangementAgreedDate": "2025-05-05",
            |  "ttpEndDate": "2026-05-05",
            |  "frequency": "monthly",
            |  "initialPaymentDate": "2025-05-05",
            |  "initialPaymentAmount": 123.45,
            |  "ddiReference": "Test DDI Reference"
            |}
            |""".stripMargin
        )

        Json.toJson(planWithAllOptionalFields) shouldBe jsonPlanWithAllOptionalFields
        jsonPlanWithAllOptionalFields.as[SaOnlyPaymentPlan] shouldBe planWithAllOptionalFields
      }
    }

    "should correctly fail to decode from invalid JSON" in {
      val badJson = JsNumber(2)

      assertThrows[JsResultException](
        badJson.as[SaOnlyPaymentPlan]
      )
    }
  }

}
