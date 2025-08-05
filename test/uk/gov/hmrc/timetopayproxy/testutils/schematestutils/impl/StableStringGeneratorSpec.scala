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

package uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl

import org.scalactic.source.Position
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsArray, JsFalse, JsNull, JsNumber, JsObject, JsString, JsTrue, JsValue, Json }

final class StableStringGeneratorSpec extends AnyFreeSpec {
  "StableStringGenerator" - {

    ".stableStringForSchemaValidator" - {
      import StableStringGenerator.{ stableStringForSchemaValidator => stringifier }

      /** This here is the main point for the method.
        *
        * Should it no longer be needed, delete the tested util method and replace its calls with `jsValue.toString`.
        */
      object ProblematicValueForBigJsNumber {
        val inputAndCorrectToString: String = "123456789012345678901234567890"
        val incorrectToString: String = "1.2345678901234567890123456789E+29"

        val bigDecimal: BigDecimal = BigDecimal(inputAndCorrectToString)

        val jsNumber: JsNumber = JsNumber(bigDecimal)
      }

      "when checking the necessity of the tested utility method" - {

        "when using JSON directly with the schema validators" - {
          // When none of the schema validators report integer errors,
          //   it's time to delete the custom stringifier utility.

          import dummyschemas.ClassWithBigInteger

          val badValue: ClassWithBigInteger = ClassWithBigInteger(BigInt("1100000000000000000000000"))
          val badValueJson: JsValue = Json.toJson(badValue)

          "using OpenAPI schema validator" - {
            "works with the custom stringifier" in {
              ClassWithBigInteger.openApiSchema.validateJsonAndGetErrors(stringifier(badValueJson)) shouldBe Nil
            }
            "fails with the default stringifier" in {
              ClassWithBigInteger.openApiSchema.validateJsonAndGetErrors(badValueJson.toString) shouldBe List(
                """needsToBeInteger: Type expected 'integer', found 'number'. (code: 1027)
                  |From: needsToBeInteger.<#/components/schemas/needsToBeInteger>.<type>""".stripMargin
              )
            }
          }

          "using JSON schema version 7 validator" - {
            "works with the custom stringifier" in {
              ClassWithBigInteger.jsonSchemaV7.validateJsonAndGetErrors(stringifier(badValueJson)) shouldBe Nil
            }
            "works with the default stringifier" in {
              ClassWithBigInteger.jsonSchemaV7.validateJsonAndGetErrors(badValueJson.toString) shouldBe Nil
            }
          }

          "using JSON schema version 4 validator" - {
            "works with the custom stringifier" in {
              ClassWithBigInteger.jsonSchemaV4.validateJsonAndGetErrors(stringifier(badValueJson)) shouldBe Nil
            }
            "fails with the default stringifier" in {
              ClassWithBigInteger.jsonSchemaV4.validateJsonAndGetErrors(badValueJson.toString) shouldBe List(
                "/needsToBeInteger: number found, integer expected"
              )
            }
          }
        }

        "verify that the default toString still introduces spurious scientific notation for integers" in {
          JsNumber(BigDecimal("1")).toString shouldBe "1"
          JsNumber(BigDecimal("11")).toString shouldBe "11"
          JsNumber(BigDecimal("110")).toString shouldBe "110"
          JsNumber(BigDecimal("1100")).toString shouldBe "1100"
          JsNumber(BigDecimal("11000")).toString shouldBe "11000"
          JsNumber(BigDecimal("110000")).toString shouldBe "110000"
          JsNumber(BigDecimal("1100000")).toString shouldBe "1100000"
          JsNumber(BigDecimal("11000000")).toString shouldBe "11000000"
          JsNumber(BigDecimal("110000000")).toString shouldBe "110000000"
          JsNumber(BigDecimal("1100000000")).toString shouldBe "1100000000"
          JsNumber(BigDecimal("11000000000")).toString shouldBe "11000000000"
          JsNumber(BigDecimal("110000000000")).toString shouldBe "110000000000"
          JsNumber(BigDecimal("1100000000000")).toString shouldBe "1100000000000"
          JsNumber(BigDecimal("11000000000000")).toString shouldBe "11000000000000"
          JsNumber(BigDecimal("110000000000000")).toString shouldBe "110000000000000"
          JsNumber(BigDecimal("1100000000000000")).toString shouldBe "1100000000000000"
          JsNumber(BigDecimal("11000000000000000")).toString shouldBe "11000000000000000"
          JsNumber(BigDecimal("110000000000000000")).toString shouldBe "110000000000000000"
          JsNumber(BigDecimal("1100000000000000000")).toString shouldBe "1100000000000000000"
          JsNumber(BigDecimal("11000000000000000000")).toString shouldBe "11000000000000000000"
          // Scientific notation will interfere with the schema validator's idea of what an "integer" is.
          JsNumber(BigDecimal("110000000000000000000")).toString shouldBe "1.1E+20"
          JsNumber(BigDecimal("1100000000000000000000")).toString shouldBe "1.1E+21"
          JsNumber(BigDecimal("11000000000000000000000")).toString shouldBe "1.1E+22"
          JsNumber(BigDecimal("110000000000000000000000")).toString shouldBe "1.1E+23"
          JsNumber(BigDecimal("1100000000000000000000000")).toString shouldBe "1.1E+24"

          // But when we don't have trailing zeroes, it magically skips the scientific notation.
          JsNumber(BigDecimal("1111111111111111111111111")).toString shouldBe "1111111111111111111111111"

          // The scientific notation starts at E+20, no matter how many significant digits there are.
          JsNumber(BigDecimal("111111111111111110000")).toString shouldBe "1.1111111111111111E+20"
        }
      }

      "works for JsNull" in {
        stringifier(JsNull) shouldBe JsNull.toString
      }

      "works for JsFalse" in {
        stringifier(JsFalse) shouldBe JsFalse.toString
      }

      "works for JsTrue" in {
        stringifier(JsTrue) shouldBe JsTrue.toString
      }

      "works for JsString" - {
        "when it is empty" in {
          val jsString = JsString("")
          stringifier(jsString) shouldBe jsString.toString
        }
        "when it contains ASCII characters" in {
          val string = (0.toChar to '~').mkString("")
          string.length shouldBe 127

          val jsString = JsString(string)

          stringifier(jsString) shouldBe jsString.toString
        }
        "when it contains complex characters" in {
          // The entries will be verified against a regex to make sure they're not mangled by our tooling.
          // You may use https://unicodeplus.com etc to check the UTF-16 encodings correpond to their Unicode values.
          val specialCharacterMappings: List[(String, List[String])] = List(
            (
              "Plane 0: (Basic Multilingual plane)",
              List(
                "U+0046: \u0046", // The Latin letter F
                "U+0416: \u0416", // Cyrillic Zhe
                "U+0986: \u0986", // Bengali A
                "U+33D6: \u33D6", // CJK compatibility 'mol' character.
                "U+FE46: \uFE46", // CJK 'White Sesame Dot' punctuation.
                "U+FFFD: \uFFFD" // Unicode 'Replacement Character' (diamond with question mark)
              )
            ),
            (
              "Plane 1: (Supplementary Multilingual Plane)",
              List(
                "U+1EC86: \uD83B\uDC86", // Indic Siyaq Number Four Hundred, unlikely to render correctly.
                "U+1F606: \uD83D\uDE06" // Emoji laughing face: 'Smiling Face With Open Mouth And Tightly-Closed Eyes'
              )
            ),
            (
              "Planes 15-16: (Supplementary Private Use Area planes)",
              List(
                "U+10FFFD: \uDBFF\uDFFD" // One of the largest unicode values, with no predetermined glyph.
              )
            )
          )

          val specialCharacterEntries: List[String] = specialCharacterMappings.flatMap(_._2)
          withClue("Validate special character entries.\n\n") {
            specialCharacterEntries.size shouldBe 9
            specialCharacterEntries.foreach { specialCharacterEntry: String =>
              withClue(s"Validate special character entry ${JsString(specialCharacterEntry)}") {
                // Must be titled with a freeform text description, followed by a colon,
                //   followed by 1-2 UTF-16 code points.
                specialCharacterEntry should fullyMatch regex "^[^:]+: ([^ ]{1,2})$".r
              }
            }
          }

          val string = specialCharacterEntries.mkString(" ; ")

          val jsString = JsString(string)

          stringifier(jsString) shouldBe jsString.toString
        }
      }

      "works for JsNumber" - {
        "works for a selection of decimal values" - {
          final case class TestCase(input: String, output: String)(implicit val pos: Position)
          object TestCase {
            def apply(normalInput: String)(implicit pos: Position): TestCase = TestCase(normalInput, normalInput)
          }

          val testCases: List[TestCase] = List(
            TestCase("0"),
            TestCase("1"),
            TestCase("11"),
            TestCase("-201"),
            TestCase("-1001.233"),
            TestCase("10.233"),
            TestCase("1.023E+22"),
            TestCase("-1.023E+22"),
            TestCase("1.023E-22"),
            TestCase("-1.023E-22"),
            TestCase("123456790123456790123456790123456790123456790123456790123456790123456790123456790123456790"),
            TestCase("123456790123456790123456790123456790123456790123456790.123456790123456790123456790123456790"),
            TestCase("-123456790123456790123456790123456790123456790123456790123456790123456790123456790123456790"),
            TestCase("-123456790123456790123456790123456790123456790123456790.123456790123456790123456790123456790"),
            TestCase("1234567.123456790123456790123456790123456790"),
            TestCase("-1234567.123456790123456790123456790123456790"),
            TestCase("1234567.123456790123456790123456790000000000"),
            TestCase("-1234567.123456790123456790123456790000000000"),
            TestCase(
              "-1234567.1234567901234567901234567900000000001E+5555",
              "-1.2345671234567901234567901234567900000000001E+5561"
            ),
            TestCase(
              "-1234567.1234567901234567901234567900000000001E-5555",
              "-1.2345671234567901234567901234567900000000001E-5549"
            ),
            TestCase("1.5E+1234567890"),
            TestCase("1.5E-1234567890"),
            TestCase("1.5e-555", "1.5E-555"),
            TestCase(ProblematicValueForBigJsNumber.inputAndCorrectToString)
          )

          for (testCase <- testCases) {
            import testCase.{ input, output, pos }
            s"given ${JsString(input)}" in {
              withClue("Validate that the BigDecimal stringifies to the expected value.\n\n") {
                BigDecimal(input).toString shouldBe output
              }
              stringifier(JsNumber(BigDecimal(input))) shouldBe output
            }
          }
        }

        "behaves differently from the standard toString in the case of the problematic value" in {
          withClue("Check that the problem is still present.\n\n") {
            ProblematicValueForBigJsNumber.jsNumber.toString should not be
              ProblematicValueForBigJsNumber.inputAndCorrectToString

            ProblematicValueForBigJsNumber.jsNumber.toString shouldBe ProblematicValueForBigJsNumber.incorrectToString
          }

          stringifier(ProblematicValueForBigJsNumber.jsNumber) shouldBe
            ProblematicValueForBigJsNumber.inputAndCorrectToString
        }
      }

      "works for JsObject" - {
        "when it is empty" in {
          val jsObject = JsObject.empty
          stringifier(jsObject) shouldBe jsObject.toString
        }

        "when it contains simple values without bugs" in {
          val jsObject = JsObject(
            List(
              "key1"                         -> JsNull,
              "key2"                         -> JsTrue,
              "key3"                         -> JsFalse,
              (0.toChar to '~').mkString("") -> JsString("Hello"),
              "\uDBFF\uDFFD"                 -> JsNumber(BigDecimal("123456789"))
            )
          )

          stringifier(jsObject) shouldBe jsObject.toString
        }

        "when it contains simple values with bugs" in {
          val jsObject = JsObject(
            List(
              "key0" -> JsNull,
              "key1" -> JsTrue,
              "key2" -> JsFalse,
              "key3" -> JsString("Hello"),
              "key4" -> JsNumber(BigDecimal("123456789")),
              "key5" -> ProblematicValueForBigJsNumber.jsNumber
            )
          )

          withClue("Check that the problem is still present.\n\n") {
            stringifier(jsObject) should not be jsObject.toString
          }

          withClue("Validate test data.\n\n") {
            import ProblematicValueForBigJsNumber.incorrectToString

            jsObject.toString shouldBe
              s"""{"key0":null,"key1":true,"key2":false,"key3":"Hello","key4":123456789,"key5":$incorrectToString}"""
          }

          locally {
            import ProblematicValueForBigJsNumber.inputAndCorrectToString

            stringifier(jsObject) shouldBe
              s"""{"key0":null,"key1":true,"key2":false,"key3":"Hello","key4":123456789,"key5":$inputAndCorrectToString}"""
          }
        }

        "when it contains complex values with problems" in {
          val jsArray = JsObject(
            List(
              "field0" -> JsArray(
                List(
                  JsNull,
                  ProblematicValueForBigJsNumber.jsNumber
                )
              ),
              "badNumber" -> ProblematicValueForBigJsNumber.jsNumber
            )
          )

          withClue("Check that the problem is still present.\n\n") {
            stringifier(jsArray) should not be jsArray.toString
          }

          withClue("Validate test data.\n\n") {
            import ProblematicValueForBigJsNumber.incorrectToString

            jsArray.toString shouldBe s"""{"field0":[null,$incorrectToString],"badNumber":$incorrectToString}"""
          }

          locally {
            import ProblematicValueForBigJsNumber.inputAndCorrectToString

            stringifier(jsArray) shouldBe
              s"""{"field0":[null,$inputAndCorrectToString],"badNumber":$inputAndCorrectToString}"""
          }
        }
      }

      "works for JsArray" - {
        "when it is empty" in {
          val jsArray = JsArray.empty
          stringifier(jsArray) shouldBe jsArray.toString
        }

        "when it contains simple values without problems" in {
          val jsArray = JsArray(
            List(
              JsNull,
              JsTrue,
              JsFalse,
              JsString("Hello"),
              JsNumber(BigDecimal("123456789"))
            )
          )

          stringifier(jsArray) shouldBe jsArray.toString
        }

        "when it contains simple values with problems" in {
          val jsArray = JsArray(
            List(
              JsNull,
              JsTrue,
              JsFalse,
              JsString("Hello"),
              JsNumber(BigDecimal("123456789")),
              ProblematicValueForBigJsNumber.jsNumber
            )
          )

          withClue("Check that the problem is still present.\n\n") {
            stringifier(jsArray) should not be jsArray.toString
          }

          withClue("Validate test data.\n\n") {
            import ProblematicValueForBigJsNumber.incorrectToString

            jsArray.toString shouldBe s"""[null,true,false,"Hello",123456789,$incorrectToString]"""
          }

          locally {
            import ProblematicValueForBigJsNumber.inputAndCorrectToString

            stringifier(jsArray) shouldBe s"""[null,true,false,"Hello",123456789,$inputAndCorrectToString]"""
          }
        }

        "when it contains complex values with problems" in {
          val jsArray = JsArray(
            List(
              JsObject(
                List(
                  "field0"    -> JsNull,
                  "badNumber" -> ProblematicValueForBigJsNumber.jsNumber
                )
              ),
              ProblematicValueForBigJsNumber.jsNumber
            )
          )

          withClue("Check that the problem is still present.\n\n") {
            stringifier(jsArray) should not be jsArray.toString
          }

          withClue("Validate test data.\n\n") {
            import ProblematicValueForBigJsNumber.incorrectToString

            jsArray.toString shouldBe
              s"""[{"field0":null,"badNumber":$incorrectToString},$incorrectToString]"""
          }

          locally {
            import ProblematicValueForBigJsNumber.inputAndCorrectToString

            stringifier(jsArray) shouldBe
              s"""[{"field0":null,"badNumber":$inputAndCorrectToString},$inputAndCorrectToString]"""
          }
        }
      }

      "round-trips" - {
        "are still affected by ANOTHER bug, when PARSING JSON numbers instead of when reading them" in {
          val inputString = "123456789012345678901234567890123456789012345678901234567890"
          val incorrectParseString = "1.234567890123456789012345678901235E+59"

          val parsedBigDecimal: BigDecimal = Json.parse(inputString).as[JsNumber].value

          withClue("If this bug is ever fixed, we can test this case too.\n\n") {
            parsedBigDecimal should not be BigDecimal(inputString)
            parsedBigDecimal shouldBe BigDecimal(incorrectParseString)
          }
        }
      }
    }

  }
}
