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

package uk.gov.hmrc.timetopayproxy.models.saopled.chargeInfoApi

import cats.data.NonEmptyList
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsSuccess, JsValue, Json, Reads, Writes }
import uk.gov.hmrc.timetopayproxy.models.{ IdType, IdValue, Identification }
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps._
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

class ChargeInfoRequestSpec extends AnyFreeSpec {

  object TestData {
    object WithNoDeclaredOptions {
      def obj: ChargeInfoRequest = ChargeInfoRequest(
        channelIdentifier = ChargeInfoChannelIdentifier("Channel Identifier"),
        identifications = NonEmptyList.of(
          Identification(idType = IdType("id type 1"), idValue = IdValue("id value 1")),
          Identification(idType = IdType("id type 2"), idValue = IdValue("id value 2"))
        ),
        regimeType = RegimeType.SA
      )

      def json: JsValue = Json.parse(
        """{
          |  "channelIdentifier": "Channel Identifier",
          |  "identifications": [
          |    {
          |      "idType": "id type 1",
          |      "idValue": "id value 1"
          |    },
          |    {
          |      "idType": "id type 2",
          |      "idValue": "id value 2"
          |    }
          |  ],
          |  "regimeType": "SA"
          |}
          |""".stripMargin
      )
    }
  }

  "ChargeInfoRequest" - {

    "implicit JSON writer (data going to time-to-pay-eligibility)" - {
      def writerToTtp: Writes[ChargeInfoRequest] = implicitly[Writes[ChargeInfoRequest]]

      "when no optional fields are applicable" - {
        def json: JsValue = TestData.WithNoDeclaredOptions.json
        def obj: ChargeInfoRequest = TestData.WithNoDeclaredOptions.obj

        "writes the correct JSON" in {
          writerToTtp.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with the time-to-pay-eligibility schema" in {
          val schema = Validators.TimeToPayEligibility.ChargeInfo.openApiRequestSchema
          val writtenJson: JsValue = writerToTtp.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }
    }

    "implicit JSON reader (data coming from our clients)" - {
      def readerFromClients: Reads[ChargeInfoRequest] = implicitly[Reads[ChargeInfoRequest]]

      "when no optional fields are applicable" - {
        def json: JsValue = TestData.WithNoDeclaredOptions.json
        def obj: ChargeInfoRequest = TestData.WithNoDeclaredOptions.obj

        "reads the JSON correctly" in {
          readerFromClients.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.ChargeInfo.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }
    }

  }
}
