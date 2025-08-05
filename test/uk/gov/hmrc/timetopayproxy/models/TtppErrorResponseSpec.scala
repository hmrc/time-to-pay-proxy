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
import play.api.libs.json.{ JsValue, Json, Writes }
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps._
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

final class TtppErrorResponseSpec extends AnyFreeSpec {
  "TtppErrorResponse" - {
    object TestData {
      object WithNoDeclaredOptions {
        def obj: TtppErrorResponse = TtppErrorResponse(
          statusCode = 400,
          errorMessage = "A TTP Proxy error message"
        )

        def json: JsValue = Json.parse(
          """{
            |  "statusCode": 400,
            |  "errorMessage": "A TTP Proxy error message"
            |}
            |""".stripMargin
        )
      }
    }

    "implicit JSON writer (data going to our clients)" - {
      def writerToClients: Writes[TtppErrorResponse] = implicitly[Writes[TtppErrorResponse]]

      "when no optional fields are applicable" - {
        def json: JsValue = TestData.WithNoDeclaredOptions.json

        def obj: TtppErrorResponse = TestData.WithNoDeclaredOptions.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.openApiResponseErrorSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }
    }
  }
}
