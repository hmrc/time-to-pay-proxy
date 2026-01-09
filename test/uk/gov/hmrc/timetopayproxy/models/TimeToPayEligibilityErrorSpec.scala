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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsSuccess, JsValue, Json, Reads }
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

final class TimeToPayEligibilityErrorSpec extends AnyFreeSpec {
  "TimeToPayEligibilityError" - {
    object TestData {
      object WithNoDeclaredOptions {
        def obj: TimeToPayEligibilityError = TimeToPayEligibilityError(
          code = "TIME_TO_PAY_ELIGIBILITY_ERROR_CODE",
          reason = "A reason for the time-to-pay-eligibility error"
        )

        def json: JsValue = Json.parse(
          """{
            |  "code": "TIME_TO_PAY_ELIGIBILITY_ERROR_CODE",
            |  "reason": "A reason for the time-to-pay-eligibility error"
            |}
            |""".stripMargin
        )
      }
    }

    "implicit JSON reader (data coming from time-to-pay-eligibility)" - {
      def readerFromTtp: Reads[TimeToPayEligibilityError] = implicitly[Reads[TimeToPayEligibilityError]]

      "when no optional fields are applicable" - {
        def json: JsValue = TestData.WithNoDeclaredOptions.json
        def obj: TimeToPayEligibilityError = TestData.WithNoDeclaredOptions.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay-eligibility schema" - {
          "for the charge info endpoint" in {
            val schema = Validators.TimeToPayEligibility.ChargeInfo.Live.openApiErrorSchema

            schema.validateAndGetErrors(json) shouldBe Nil
          }
        }
      }
    }
  }
}
