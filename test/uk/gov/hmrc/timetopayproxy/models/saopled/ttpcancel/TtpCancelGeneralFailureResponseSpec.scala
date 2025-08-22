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

package uk.gov.hmrc.timetopayproxy.models.saopled.ttpcancel

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsSuccess, JsValue, Json, Reads, Writes }
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps._
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

final class TtpCancelGeneralFailureResponseSpec extends AnyFreeSpec {

  object TestData {
    object WithNoDeclaredOptions {
      def obj: TtpCancelGeneralFailureResponse = TtpCancelGeneralFailureResponse(
        code = 400,
        details = "some error details"
      )

      def json: JsValue = Json.parse(
        """{
          |  "code": 400,
          |  "details": "some error details"
          |}
          |""".stripMargin
      )
    }
  }

  "TtpCancelGeneralFailureResponse" - {

    "implicit JSON writer (data going to our clients)" - {
      def writerToClients: Writes[TtpCancelGeneralFailureResponse] = implicitly[Writes[TtpCancelGeneralFailureResponse]]

      "when no optional fields are applicable" - {
        def json: JsValue = TestData.WithNoDeclaredOptions.json
        def obj: TtpCancelGeneralFailureResponse = TestData.WithNoDeclaredOptions.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpCancel.openApiResponseGeneralFailureSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }
    }

    "implicit JSON reader (data coming from time-to-pay)" - {
      def readerFromTtp: Reads[TtpCancelGeneralFailureResponse] = implicitly[Reads[TtpCancelGeneralFailureResponse]]

      "when no optional fields are applicable" - {
        def json: JsValue = TestData.WithNoDeclaredOptions.json
        def obj: TtpCancelGeneralFailureResponse = TestData.WithNoDeclaredOptions.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPayProxy.TtpCancel.openApiResponseGeneralFailureSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }
    }
  }
}
