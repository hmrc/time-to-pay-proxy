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

package uk.gov.hmrc.timetopayproxy.models.currency

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsResult, Json, Reads, Writes }

final class GbpPoundsUncheckedSpec extends AnyFreeSpec {
  "GbpPounds" - {
    "implicit JSON writer" - {
      val writer = implicitly[Writes[GbpPoundsUnchecked]]

      "given £0" in {
        val amount = GbpPoundsUnchecked(0)
        writer.writes(amount).toString shouldBe "0"
      }
      "given £0.0" in {
        val amount = GbpPoundsUnchecked(0.0)
        writer.writes(amount).toString shouldBe "0"
      }
      "given £0.00" in {
        val amount = GbpPoundsUnchecked(0.00)
        writer.writes(amount).toString shouldBe "0"
      }
      "given £0.0000" in {
        val amount = GbpPoundsUnchecked(0.0000)
        writer.writes(amount).toString shouldBe "0"
      }

      "given £0.5" in {
        val amount = GbpPoundsUnchecked(0.5)
        writer.writes(amount).toString shouldBe "0.5"
      }
      "given £0.50" in {
        val amount = GbpPoundsUnchecked(0.50)
        writer.writes(amount).toString shouldBe "0.5"
      }
      "given £0.5000" in {
        val amount = GbpPoundsUnchecked(0.5000)
        writer.writes(amount).toString shouldBe "0.5"
      }

      "given £0.55" in {
        val amount = GbpPoundsUnchecked(0.55)
        writer.writes(amount).toString shouldBe "0.55"
      }

      "given £-123.55" in {
        val amount = GbpPoundsUnchecked(-123.55)
        writer.writes(amount).toString shouldBe "-123.55"
      }

      "given £0.555, will still serialise it because it is 'Unchecked'" in {
        val amount = GbpPoundsUnchecked(BigDecimal(0.555))
        writer.writes(amount).toString shouldBe "0.555"
      }
    }

    "implicit JSON reader" - {
      val reader = implicitly[Reads[GbpPoundsUnchecked]]

      "given 0" in {
        val result: JsResult[GbpPoundsUnchecked] = reader.reads(Json.parse("0"))
        result.get.value shouldBe BigDecimal(0)
        result.get.value.scale shouldBe 0
      }
      "given 0.0" in {
        val result: JsResult[GbpPoundsUnchecked] = reader.reads(Json.parse("0.0"))
        result.get.value shouldBe BigDecimal(0)
        result.get.value.scale shouldBe 1
      }
      "given 0.00" in {
        val result: JsResult[GbpPoundsUnchecked] = reader.reads(Json.parse("0.00"))
        result.get.value shouldBe BigDecimal(0)
        result.get.value.scale shouldBe 2
      }
      "given 0.0000" in {
        val result: JsResult[GbpPoundsUnchecked] = reader.reads(Json.parse("0.0000"))
        result.get.value shouldBe BigDecimal(0)
        result.get.value.scale shouldBe 4
      }

      "given 0.5" in {
        val result: JsResult[GbpPoundsUnchecked] = reader.reads(Json.parse("0.5"))
        result.get.value shouldBe BigDecimal(0.50)
        result.get.value.scale shouldBe 1
      }
      "given 0.50" in {
        val result: JsResult[GbpPoundsUnchecked] = reader.reads(Json.parse("0.50"))
        result.get.value shouldBe BigDecimal(0.50)
        result.get.value.scale shouldBe 2
      }
      "given 0.5000" in {
        val result: JsResult[GbpPoundsUnchecked] = reader.reads(Json.parse("0.5000"))
        result.get.value shouldBe BigDecimal(0.50)
        result.get.value.scale shouldBe 4
      }

      "given 0.55" in {
        val result: JsResult[GbpPoundsUnchecked] = reader.reads(Json.parse("0.55"))
        result.get.value shouldBe BigDecimal(0.55)
        result.get.value.scale shouldBe 2
      }

      "given -123.55" in {
        val result: JsResult[GbpPoundsUnchecked] = reader.reads(Json.parse("-123.55"))
        result.get.value shouldBe BigDecimal(-123.55)
        result.get.value.scale shouldBe 2
      }

      "given 0.555, will still deserialise it because it is 'Unchecked'" in {
        val result: JsResult[GbpPoundsUnchecked] = reader.reads(Json.parse("0.555"))
        result.get.value shouldBe BigDecimal(0.555)
        result.get.value.scale shouldBe 3
      }
    }

  }
}
