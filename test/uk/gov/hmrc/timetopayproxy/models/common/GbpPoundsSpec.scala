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

package uk.gov.hmrc.timetopayproxy.models.common

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

class GbpPoundsSpec extends AnyWordSpec with Matchers {

  "GbpPounds" when {
    "createOrThrow method" should {
      "create GbpPounds for valid amounts" in {
        GbpPounds.createOrThrow(BigDecimal("100.50")).value shouldBe BigDecimal("100.50")
        GbpPounds.createOrThrow(BigDecimal("100.00")).value shouldBe BigDecimal("100.00")
        GbpPounds.createOrThrow(BigDecimal("100")).value shouldBe BigDecimal("100.00")
        GbpPounds.createOrThrow(BigDecimal("0.01")).value shouldBe BigDecimal("0.01")
        GbpPounds.createOrThrow(BigDecimal("0")).value shouldBe BigDecimal("0.00")
        GbpPounds.createOrThrow(BigDecimal("999999.99")).value shouldBe BigDecimal("999999.99")
      }

      "handle rounding correctly" in {
        GbpPounds.createOrThrow(BigDecimal("100.5")).value shouldBe BigDecimal("100.50")
      }

      "throw exception for invalid amounts" in {
        an[IllegalArgumentException] should be thrownBy GbpPounds.createOrThrow(BigDecimal("100.123"))
        an[IllegalArgumentException] should be thrownBy GbpPounds.createOrThrow(BigDecimal("0.001"))
        an[IllegalArgumentException] should be thrownBy GbpPounds.createOrThrow(BigDecimal("-1"))
      }
    }

    "serializing to JSON" should {
      "produce correct JSON values" in {
        val pounds = GbpPounds.createOrThrow(BigDecimal("100.50"))
        Json.toJson(pounds) shouldBe JsNumber(BigDecimal("100.50"))
      }
    }

    "deserializing from JSON" should {
      "parse valid amounts correctly" in {
        JsNumber(BigDecimal("100.50")).validate[GbpPounds] shouldBe JsSuccess(GbpPounds(BigDecimal("100.50")))
        JsNumber(BigDecimal("0.00")).validate[GbpPounds] shouldBe JsSuccess(GbpPounds(BigDecimal("0.00")))
      }

      "reject amounts with too many decimal places" in {
        JsNumber(BigDecimal("100.123")).validate[GbpPounds] shouldBe a[JsError]
      }

      "reject non-numeric values" in {
        JsString("not a number").validate[GbpPounds] shouldBe a[JsError]
      }
    }
  }
}
