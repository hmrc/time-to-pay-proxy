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

import org.scalatest.EitherValues._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsError, JsResult, Json, Reads, Writes }

import java.math.{ BigDecimal => JavaBigDecimal, MathContext, RoundingMode }

final class GbpPoundsSpec extends AnyFreeSpec {
  "GbpPounds" - {

    ".apply(BigDecimal)" - {
      "given values that don't require rounding" - {
        "given £0" in {
          val eitherAmount = GbpPounds(0)
          val amount = eitherAmount.value
          amount.value shouldBe BigDecimal(0)
          amount.value.scale shouldBe 2
        }
        "given £0.0" in {
          val eitherAmount = GbpPounds(0.0)
          val amount = eitherAmount.value
          amount.value shouldBe BigDecimal(0)
          amount.value.scale shouldBe 2
        }
        "given £0.00" in {
          val eitherAmount = GbpPounds(0.00)
          val amount = eitherAmount.value
          amount.value shouldBe BigDecimal(0)
          amount.value.scale shouldBe 2
        }
        "given £0.0000" in {
          val eitherAmount = GbpPounds(0.0000)
          val amount = eitherAmount.value
          amount.value shouldBe BigDecimal(0)
          amount.value.scale shouldBe 2
        }
        "given £0.5" in {
          val eitherAmount = GbpPounds(0.5)
          val amount = eitherAmount.value
          amount.value shouldBe BigDecimal(0.50)
          amount.value.scale shouldBe 2
        }
        "given £0.50" in {
          val eitherAmount = GbpPounds(0.50)
          val amount = eitherAmount.value
          amount.value shouldBe BigDecimal(0.50)
          amount.value.scale shouldBe 2
        }
        "given £0.55" in {
          val eitherAmount = GbpPounds(0.55)
          val amount = eitherAmount.value
          amount.value shouldBe BigDecimal(0.55)
          amount.value.scale shouldBe 2
        }
        "given £-123.55" in {
          val eitherAmount = GbpPounds(-123.55)
          val amount = eitherAmount.value
          amount.value shouldBe BigDecimal(-123.55)
          amount.value.scale shouldBe 2
        }
        "given £-123.55000" in {
          val eitherAmount = GbpPounds(-123.55000)
          val amount = eitherAmount.value
          amount.value shouldBe BigDecimal(-123.55)
          amount.value.scale shouldBe 2
        }
      }

      "given values that require rounding" - {
        "given £0.555" in {
          val eitherAmount = GbpPounds(0.555)
          val error: String = eitherAmount.left.value
          error shouldBe "Number of digits after decimal point should not exceed 2: 0.555"
        }
        "given £0.554" in {
          val eitherAmount = GbpPounds(0.554)
          val error: String = eitherAmount.left.value
          error shouldBe "Number of digits after decimal point should not exceed 2: 0.554"
        }
        "given £0.5555" in {
          val eitherAmount = GbpPounds(0.5555)
          val error: String = eitherAmount.left.value
          error shouldBe "Number of digits after decimal point should not exceed 2: 0.5555"
        }
        "given £0.5554" in {
          val eitherAmount = GbpPounds(0.5554)
          val error: String = eitherAmount.left.value
          error shouldBe "Number of digits after decimal point should not exceed 2: 0.5554"
        }
        "given £-1234.5554" in {
          val eitherAmount = GbpPounds(-1234.5554)
          val error: String = eitherAmount.left.value
          error shouldBe "Number of digits after decimal point should not exceed 2: -1234.5554"
        }
      }
    }

    ".createOrThrow" - {
      "given values that don't require rounding" - {
        "given £0" in {
          val amount = GbpPounds.createOrThrow(0)
          amount.value shouldBe BigDecimal(0)
          amount.value.scale shouldBe 2
        }
        "given £0.0" in {
          val amount = GbpPounds.createOrThrow(0.0)
          amount.value shouldBe BigDecimal(0)
          amount.value.scale shouldBe 2
        }
        "given £0.00" in {
          val amount = GbpPounds.createOrThrow(0.00)
          amount.value shouldBe BigDecimal(0)
          amount.value.scale shouldBe 2
        }
        "given £0.0000" in {
          val amount = GbpPounds.createOrThrow(0.0000)
          amount.value shouldBe BigDecimal(0)
          amount.value.scale shouldBe 2
        }
        "given £0.5" in {
          val amount = GbpPounds.createOrThrow(0.5)
          amount.value shouldBe BigDecimal(0.50)
          amount.value.scale shouldBe 2
        }
        "given £0.50" in {
          val amount = GbpPounds.createOrThrow(0.50)
          amount.value shouldBe BigDecimal(0.50)
          amount.value.scale shouldBe 2
        }
        "given £0.55" in {
          val amount = GbpPounds.createOrThrow(0.55)
          amount.value shouldBe BigDecimal(0.55)
          amount.value.scale shouldBe 2
        }
        "given £-123.55" in {
          val amount = GbpPounds.createOrThrow(-123.55)
          amount.value shouldBe BigDecimal(-123.55)
          amount.value.scale shouldBe 2
        }
        "given £-123.55000" in {
          val amount = GbpPounds.createOrThrow(-123.55000)
          amount.value shouldBe BigDecimal(-123.55)
          amount.value.scale shouldBe 2
        }
      }

      "given values that require rounding" - {
        "given £0.555" in {
          val exception = the[IllegalArgumentException] thrownBy GbpPounds.createOrThrow(0.555)
          exception.getMessage shouldBe "Number of digits after decimal point should not exceed 2: 0.555"
        }
        "given £0.554" in {
          val exception = the[IllegalArgumentException] thrownBy GbpPounds.createOrThrow(0.554)
          exception.getMessage shouldBe "Number of digits after decimal point should not exceed 2: 0.554"
        }
        "given £0.5555" in {
          val exception = the[IllegalArgumentException] thrownBy GbpPounds.createOrThrow(0.5555)
          exception.getMessage shouldBe "Number of digits after decimal point should not exceed 2: 0.5555"
        }
        "given £0.5554" in {
          val exception = the[IllegalArgumentException] thrownBy GbpPounds.createOrThrow(0.5554)
          exception.getMessage shouldBe "Number of digits after decimal point should not exceed 2: 0.5554"
        }
        "given £-1234.5554" in {
          val exception = the[IllegalArgumentException] thrownBy GbpPounds.createOrThrow(-1234.5554)
          exception.getMessage shouldBe "Number of digits after decimal point should not exceed 2: -1234.5554"
        }

        "given £0.5554 if the parameter has rounding enabled (will still throw)" in {
          val roundingMathContext = new MathContext(4, RoundingMode.HALF_UP)
          val roundableBigDecimal = new BigDecimal(new JavaBigDecimal(0.5554, roundingMathContext), roundingMathContext)

          val exception = the[IllegalArgumentException] thrownBy GbpPounds.createOrThrow(roundableBigDecimal)
          exception.getMessage shouldBe "Number of digits after decimal point should not exceed 2: 0.5554"
        }
      }
    }

    "implicit JSON writer" - {
      val writer = implicitly[Writes[GbpPounds]]

      "given £0" in {
        val amount = GbpPounds.createOrThrow(0)
        writer.writes(amount).toString shouldBe "0"
      }
      "given £0.0" in {
        val amount = GbpPounds.createOrThrow(0.0)
        writer.writes(amount).toString shouldBe "0"
      }
      "given £0.00" in {
        val amount = GbpPounds.createOrThrow(0.00)
        writer.writes(amount).toString shouldBe "0"
      }
      "given £0.0000" in {
        val amount = GbpPounds.createOrThrow(0.0000)
        writer.writes(amount).toString shouldBe "0"
      }

      "given £0.5" in {
        val amount = GbpPounds.createOrThrow(0.5)
        writer.writes(amount).toString shouldBe "0.5"
      }
      "given £0.50" in {
        val amount = GbpPounds.createOrThrow(0.50)
        writer.writes(amount).toString shouldBe "0.5"
      }
      "given £0.5000" in {
        val amount = GbpPounds.createOrThrow(0.5000)
        writer.writes(amount).toString shouldBe "0.5"
      }

      "given £0.55" in {
        val amount = GbpPounds.createOrThrow(0.55)
        writer.writes(amount).toString shouldBe "0.55"
      }

      "given £-123.55" in {
        val amount = GbpPounds.createOrThrow(-123.55)
        writer.writes(amount).toString shouldBe "-123.55"
      }
    }

    "implicit JSON reader" - {
      val reader = implicitly[Reads[GbpPounds]]

      "given 0" in {
        val result: JsResult[GbpPounds] = reader.reads(Json.parse("0"))
        result.get.value shouldBe BigDecimal(0)
        result.get.value.scale shouldBe 2
      }
      "given 0.0" in {
        val result: JsResult[GbpPounds] = reader.reads(Json.parse("0.0"))
        result.get.value shouldBe BigDecimal(0)
        result.get.value.scale shouldBe 2
      }
      "given 0.00" in {
        val result: JsResult[GbpPounds] = reader.reads(Json.parse("0.00"))
        result.get.value shouldBe BigDecimal(0)
        result.get.value.scale shouldBe 2
      }
      "given 0.0000" in {
        val result: JsResult[GbpPounds] = reader.reads(Json.parse("0.0000"))
        result.get.value shouldBe BigDecimal(0)
        result.get.value.scale shouldBe 2
      }

      "given 0.5" in {
        val result: JsResult[GbpPounds] = reader.reads(Json.parse("0.5"))
        result.get.value shouldBe BigDecimal(0.50)
        result.get.value.scale shouldBe 2
      }
      "given 0.50" in {
        val result: JsResult[GbpPounds] = reader.reads(Json.parse("0.50"))
        result.get.value shouldBe BigDecimal(0.50)
        result.get.value.scale shouldBe 2
      }
      "given 0.5000" in {
        val result: JsResult[GbpPounds] = reader.reads(Json.parse("0.5000"))
        result.get.value shouldBe BigDecimal(0.50)
        result.get.value.scale shouldBe 2
      }

      "given 0.55" in {
        val result: JsResult[GbpPounds] = reader.reads(Json.parse("0.55"))
        result.get.value shouldBe BigDecimal(0.55)
        result.get.value.scale shouldBe 2
      }

      "given -123.55" in {
        val result: JsResult[GbpPounds] = reader.reads(Json.parse("-123.55"))
        result.get.value shouldBe BigDecimal(-123.55)
        result.get.value.scale shouldBe 2
      }

      "given 0.555" in {
        val result: JsResult[GbpPounds] = reader.reads(Json.parse("0.555"))
        result shouldBe JsError("Number of digits after decimal point should not exceed 2: 0.555")
      }
    }

  }
}
