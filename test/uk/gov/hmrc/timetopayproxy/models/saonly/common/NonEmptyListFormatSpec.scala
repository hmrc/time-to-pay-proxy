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

package uk.gov.hmrc.timetopayproxy.models.saonly.common

import cats.data.NonEmptyList
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json._

final case class NelInts(values: NonEmptyList[Int])

object NelInts {
  implicit val valuesFormat: Format[NonEmptyList[Int]] = NonEmptyListFormat.nonEmptyListFormat[Int]

  implicit val format: OFormat[NelInts] = Json.format[NelInts]
}

class NonEmptyListFormatSpec extends AnyFreeSpec {

  "NonEmptyListFormat" - {

    "should encode and decode correctly" in {
      val intsList = NelInts(NonEmptyList.of(1, 2))
      val intsListJson = Json.parse(
        """{
          | "values": [
          | 1, 2
          |]
          |}""".stripMargin
      )

      Json.toJson(intsList) shouldBe intsListJson

      intsListJson.as[NelInts] shouldBe intsList
    }

    "should correctly fail to decode invalid JSON" - {

      "when given a string instead of a JSON array" in {
        val naughtyJson = JsString("I'm a naughty JSON String!")

        assertThrows[JsResultException](
          naughtyJson.as[NelInts]
        )
      }

      "when given an empty JSON array" in {
        val emptyJsonArray = Json.parse(
          """{
            | "values": []
            |}""".stripMargin
        )

        assertThrows[JsResultException](emptyJsonArray.as[NelInts])
      }
    }
  }
}
