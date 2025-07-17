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

package uk.gov.hmrc.timetopayproxy.models.chargeInfoApi

import com.fasterxml.jackson.databind.ObjectMapper
import org.openapi4j.schema.validator.v3.SchemaValidator
import org.openapi4j.schema.validator.{ ValidationContext, ValidationData }
import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.{ JsValue, Json, Writes }
import org.scalatest.matchers.should.Matchers._
import uk.gov.hmrc.timetopayproxy.support.{ ApiSchema, ChargeInfoRequestSchema }

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{ Failure, Success, Try }

class ChargeInfoRequestSpec extends AnyFreeSpec {

  object TestData {
    object WithNoDeclaredOptions {
      def obj: ChargeInfoRequest = ChargeInfoRequest(
        channelIdentifier = ChargeInfoChannelIdentifier("Channel Identifier"),
        identifications = List(
          Identification(idType = IDType("id type 1"), idValue = IDValue("id value 1")),
          Identification(idType = IDType("id type 2"), idValue = IDValue("id value 2"))
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
    "implicit json writer" - {
      def writer: Writes[ChargeInfoRequest] = implicitly[Writes[ChargeInfoRequest]]

      def obj = TestData.WithNoDeclaredOptions.obj

      def json = TestData.WithNoDeclaredOptions.json

      "should write the expected JSON structure" in {
        writer.writes(obj) shouldBe json
      }

      "should write JSON compatible with the API Schema" in new TestBase {
        val errors: Map[String, List[String]] = validate(ChargeInfoRequestSchema, json.toString())

        errors shouldEqual Map.empty
      }
    }
  }
}

trait TestBase {
  lazy val objectMapper = new ObjectMapper

  def validate(api: ApiSchema, body: String): Map[String, List[String]] = {
    val output = new ValidationData
    val json = new ObjectMapper().readTree(body)

    Try {
      new SchemaValidator(new ValidationContext(api.fullSchema.getContext), null, api.defaultSchema)
        .validate(json, output)
    } match {
      case Failure(e) =>
        Map("Exception found" -> List(e.getMessage))
      case Success(_) =>
        output
          .results()
          .items()
          .asScala
          .toList
          .map(e => "validationError" -> e.toString)
          .groupBy(_._1)
          .view
          .mapValues(_.map(_._2).toList)
          .toMap
    }
  }
}
