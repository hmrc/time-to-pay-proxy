/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.models.cdcs.chargemigration

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{ Format, JsValue, Json }
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps.RichJsValueWithAssertions
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

class ChargeMigrationRequestSpec extends AnyFreeSpecLike with Matchers {

  "ChargeMigrationRequest" - {

    val format = implicitly[Format[ChargeMigrationRequest]]

    val chargeMigrationRequest: JsValue = Json.parse(
      """{
        |  "planId": "planId",
        |  "migratedAt": "2026-07-08T13:49:51.123Z",
        |  "planCreationChannel": "advisor",
        |  "chargeMigrations": [
        |    {
        |      "originalChargeId": "chargeId01",
        |      "replacementDebtItemChargeId": "chargeId02",
        |      "replacementCharges": [
        |        {
        |          "parentMainTrans": "5330",
        |          "mainTrans": "5330",
        |          "subTrans": "7006",
        |          "originalDebtAmount": 5000,
        |          "interestStartDate": "2026-06-30",
        |          "paymentHistory": [
        |            {
        |              "paymentDate": "2026-06-30",
        |              "paymentAmount": 20
        |            }
        |          ]
        |        },
        |        {
        |          "parentMainTrans": "5330",
        |          "mainTrans": "5330",
        |          "subTrans": "7007",
        |          "originalDebtAmount": 5000,
        |          "interestStartDate": "2026-06-30"
        |        }
        |      ]
        |    }
        |  ]
        |}""".stripMargin
    )

    "should match the OpenAPI schema" in {
      val errors =
        Validators.TimeToPayProxy.ChargeMigrationRequest.Live.openApiRequestSchema
          .validateAndGetErrors(chargeMigrationRequest)

      errors shouldBe Nil
    }

    "deserialises to the model class" in {
      val result = format.reads(chargeMigrationRequest)

      result.isSuccess shouldBe true
      val _: ChargeMigrationRequest = result.get
    }

    "deserialises and reserialises to the same thing" in {
      val readModel: ChargeMigrationRequest =
        format.reads(chargeMigrationRequest).get

      val writtenModel: JsValue =
        format.writes(readModel)

      writtenModel shouldBeEquivalentTo chargeMigrationRequest
    }
  }
}
