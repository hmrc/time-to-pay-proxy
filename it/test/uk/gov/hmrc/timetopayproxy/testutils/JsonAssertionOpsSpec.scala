/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.testutils

import org.scalatest.exceptions.TestFailedException
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json._

final class JsonAssertionOpsSpec extends AnyFreeSpec {
  "JsonAssertionOps" - {
    // The implicit classes in JsonAssertionOps are only imported individually in their own sections,
    //   to avoid cross-contamination of untested code.

    "RichJsValueWithAssertions" - {
      import JsonAssertionOps.RichJsValueWithAssertions

      ".shouldBeEquivalentTo" - {
        "for two identical simple JSON" - {
          val cases: List[(JsValue, JsValue)] = List(
            (JsNull, JsNull),
            (JsFalse, JsFalse),
            (JsTrue, JsTrue),
            (JsNumber(BigDecimal(123.1)), JsNumber(BigDecimal(123.1))),
            (JsString("abc"), JsString("abc")),
            (JsObject.empty, JsObject.empty),
            (JsArray.empty, JsArray.empty)
          )

          for ((value1, value2) <- cases)
            s"for $value1 and $value2" in {
              value1.shouldBeEquivalentTo(value2)
            }
        }

        "for two different simple JSON values" - {
          val cases: List[(JsValue, JsValue)] = List(
            (JsNull, JsFalse),
            (JsFalse, JsTrue),
            (JsTrue, JsNull),
            (JsNumber(BigDecimal(123.1)), JsNumber(BigDecimal(123.2))),
            (JsString("abc"), JsString("ab"))
          )

          for ((value1, value2) <- cases)
            s"for $value1 and $value2" in {
              // We cannot assert on the message because it's deliberately different when run in IntelliJ vs SBT.
              val exception = the[TestFailedException] thrownBy value1.shouldBeEquivalentTo(value2)
              exception.getMessage() shouldBe s"""$value1\n was not equal to $value2\n"""
            }
        }

        "for two different and complex JSON values" in {
          val value1 = Json.parse(
            """{
              |  "field2" : [
              |    "def"
              |  ],
              |  "field3" : [
              |    {
              |      "field1" : "value"
              |    },
              |    {
              |      "field2" : "value"
              |    }
              |  ],
              |  "field1" : [
              |    "abc",
              |    1,
              |    false,
              |    null,
              |    true,
              |    {
              |      "field2" : "value",
              |      "field1" : "value"
              |    }
              |  ]
              |}""".stripMargin
          )

          val value2 = Json.parse(
            """{
              |  "field1" : [ "abc" ]
              |}
              |""".stripMargin
          )

          // We cannot assert on the message because it's deliberately different when run in IntelliJ vs SBT.
          val exception = the[TestFailedException] thrownBy value1.shouldBeEquivalentTo(value2)

          // Mind the sorting of the object fields! (for determinism and easier JSON test maintenance)
          exception.getMessage() shouldBe
            s"""{
               |  "field1" : [
               |    "abc",
               |    1,
               |    false,
               |    null,
               |    true,
               |    {
               |      "field1" : "value",
               |      "field2" : "value"
               |    }
               |  ],
               |  "field2" : [
               |    "def"
               |  ],
               |  "field3" : [
               |    {
               |      "field1" : "value"
               |    },
               |    {
               |      "field2" : "value"
               |    }
               |  ]
               |}
               | was not equal to {
               |  "field1" : [
               |    "abc"
               |  ]
               |}
               |""".stripMargin
        }
      }
    }

    "RichJsValueWithMergingOperations" - {
      import JsonAssertionOps.RichJsValueWithMergingOperations

      ".strictDeepMerge" - {
        "for two primitive values" - {
          "which are identical" - {
            "and both true" in {
              JsTrue.strictDeepMerge(JsTrue) shouldBe JsTrue
            }
            "and both false" in {
              JsFalse.strictDeepMerge(JsFalse) shouldBe JsFalse
            }
            "and both null" in {
              JsNull.strictDeepMerge(JsNull) shouldBe JsNull
            }
            "and both a number" in {
              val value = JsNumber(BigDecimal(123.4444))
              value.strictDeepMerge(value) shouldBe value
            }
            "and both a string" in {
              val value = JsString("my string")
              value.strictDeepMerge(value) shouldBe value
            }
          }

          "which are different" - {
            val cases: List[(JsValue, JsValue)] = List(
              (JsNull, JsTrue),
              (JsFalse, JsTrue),
              (JsNumber(BigDecimal("1.234")), JsNumber(BigDecimal("1.2345"))),
              (JsString("abc"), JsString("ab"))
            )

            for ((value1, value2) <- cases) {
              s"""for $value1 and $value2""" in {
                val exception = the[TestFailedException] thrownBy value1.strictDeepMerge(value2)
                exception.getMessage() shouldBe s"Cannot merge $value1 and $value2 at path / in $value1 and $value2"
              }
              s"""for $value2 and $value1 (reversed case)""" in {
                val exception = the[TestFailedException] thrownBy value2.strictDeepMerge(value1)
                exception.getMessage() shouldBe s"Cannot merge $value2 and $value1 at path / in $value2 and $value1"
              }
            }
          }
        }

        "for a primitive and an object" - {
          val cases: List[(JsValue, JsValue)] = List(
            (JsNull, JsObject.empty),
            (JsFalse, JsObject.empty),
            (JsTrue, JsObject.empty),
            (JsNumber(BigDecimal("1.234")), JsObject.empty),
            (JsString("abc"), JsObject.empty),
            (JsNull, Json.parse("""{"myField":null}""")),
            (JsFalse, Json.parse("""{"myField":null}""")),
            (JsTrue, Json.parse("""{"myField":null}""")),
            (JsNumber(BigDecimal("1.234")), Json.parse("""{"myField":null}""")),
            (JsString("abc"), Json.parse("""{"myField":null}"""))
          )

          for ((value1, value2) <- cases) {
            s"""for $value1 and $value2""" in {
              val exception = the[TestFailedException] thrownBy value1.strictDeepMerge(value2)
              exception.getMessage() shouldBe s"Cannot merge $value1 and $value2 at path / in $value1 and $value2"
            }
            s"""for $value2 and $value1 (reversed case)""" in {
              val exception = the[TestFailedException] thrownBy value2.strictDeepMerge(value1)
              exception.getMessage() shouldBe s"Cannot merge $value2 and $value1 at path / in $value2 and $value1"
            }
          }
        }

        "for a primitive and an array" - {
          val cases: List[(JsValue, JsValue)] = List(
            (JsNull, Json.parse("""[]""")),
            (JsFalse, Json.parse("""[]""")),
            (JsTrue, Json.parse("""[]""")),
            (JsNumber(BigDecimal("1.234")), Json.parse("""[]""")),
            (JsString("abc"), Json.parse("""[]""")),
            (JsNull, Json.parse("""["abc", "def", {}]""")),
            (JsFalse, Json.parse("""["abc", "def", {}]""")),
            (JsTrue, Json.parse("""["abc", "def", {}]""")),
            (JsNumber(BigDecimal("1.234")), Json.parse("""["abc", "def", {}]""")),
            (JsString("abc"), Json.parse("""["abc", "def", {}]"""))
          )

          for ((value1, value2) <- cases) {
            s"""for $value1 and $value2""" in {
              val exception = the[TestFailedException] thrownBy value1.strictDeepMerge(value2)
              exception.getMessage() shouldBe s"Cannot merge $value1 and $value2 at path / in $value1 and $value2"
            }
            s"""for $value2 and $value1 (reversed case)""" in {
              val exception = the[TestFailedException] thrownBy value2.strictDeepMerge(value1)
              exception.getMessage() shouldBe s"Cannot merge $value2 and $value1 at path / in $value2 and $value1"
            }
          }
        }

        "for an object and an array" - {
          "if they are {} and []" in {
            val value1 = JsObject.empty
            val value2 = JsArray.empty
            val exception = the[TestFailedException] thrownBy value1.strictDeepMerge(value2)
            exception.getMessage() shouldBe s"Cannot merge $value1 and $value2 at path / in $value1 and $value2"
          }
          "if they are [] and {}" in {
            val value1 = JsArray.empty
            val value2 = JsObject.empty
            val exception = the[TestFailedException] thrownBy value1.strictDeepMerge(value2)
            exception.getMessage() shouldBe s"Cannot merge $value1 and $value2 at path / in $value1 and $value2"
          }
        }

        "for two objects" - {
          "when they are both empty" in {
            JsObject.empty.strictDeepMerge(JsObject.empty) shouldBe JsObject.empty
          }

          "when the first object is included in the second" in {
            val value1 = Json.parse("""{ "ka": "va" }""")
            val value2 = Json.parse("""{ "ka": "va", "kb": "vb" }""")
            value1.strictDeepMerge(value2) shouldBe value2
          }

          "when the second object is included in the first" in {
            val value1 = Json.parse("""{ "ka": "va", "kb": "vb" }""")
            val value2 = Json.parse("""{ "ka": "va" }""")
            value1.strictDeepMerge(value2) shouldBe value1
          }

          "when the two objects are disjoint" in {
            val value1 = Json.parse("""{ "ka": "va" }""")
            val value2 = Json.parse("""{ "kb": "vb" }""")
            value1.strictDeepMerge(value2) shouldBe Json.parse("""{ "ka": "va", "kb": "vb" }""")
          }

          "when the two objects are compatible and partially overlap" in {
            val value1 = Json.parse("""{ "ka": "va", "kc": "vc" }""")
            val value2 = Json.parse("""{ "kb": "vb", "kc": "vc" }""")
            value1.strictDeepMerge(value2) shouldBe Json.parse("""{ "ka": "va", "kb": "vb", "kc": "vc" }""")
          }

          "when the two objects differ in one field" in {
            val value1 = Json.parse("""{ "ka": "va", "kc": "vc1" }""")
            val value2 = Json.parse("""{ "kb": "vb", "kc": "vc2" }""")
            val exception = the[TestFailedException] thrownBy value1.strictDeepMerge(value2)
            exception.getMessage() shouldBe s"""Cannot merge "vc1" and "vc2" at path /kc in $value1 and $value2"""
          }

          "when the two objects are compatible and contain other compatible objects" in {
            val value1 = Json.parse("""{ "ka": "va", "kc": { "kca": "vca" } }""")
            val value2 = Json.parse("""{ "kb": "vb", "kc": { "kcb": "vcb" } }""")
            value1.strictDeepMerge(value2) shouldBe
              Json.parse("""{ "ka": "va", "kb": "vb", "kc": { "kca": "vca", "kcb": "vcb" } }""")
          }

          "when the two objects are incompatible because they contain other incompatible objects" in {
            val value1 = Json.parse("""{ "ka": "va", "kc": { "kca": "vca1" } }""")
            val value2 = Json.parse("""{ "kb": "vb", "kc": { "kca": "vca2" } }""")
            val exception = the[TestFailedException] thrownBy value1.strictDeepMerge(value2)
            exception.getMessage() shouldBe
              s"""Cannot merge "vca1" and "vca2" at path /kc/kca in $value1 and $value2"""
          }
        }

        "for two arrays" - {
          "when they are both empty" in {
            JsArray.empty.strictDeepMerge(JsArray.empty) shouldBe JsArray.empty
          }

          "when the first array is a prefix of the second" in {
            val value1 = Json.parse("""[1, null, ""]""")
            val value2 = Json.parse("""[1, null, "", 4]""")
            value1.strictDeepMerge(value2) shouldBe value2
          }

          "when the second array is a prefix of the first" in {
            val value1 = Json.parse("""[1, null, "", 4]""")
            val value2 = Json.parse("""[1, null, ""]""")
            value1.strictDeepMerge(value2) shouldBe value1
          }

          "when the first array is a suffix of the second" in {
            val value1 = Json.parse("""[null, "", 4]""")
            val value2 = Json.parse("""[1, null, "", 4]""")
            val exception = the[TestFailedException] thrownBy value1.strictDeepMerge(value2)
            exception.getMessage() shouldBe s"Cannot merge null and 1 at path (0) in $value1 and $value2"
          }

          "when the second array is a suffix of the first" in {
            val value1 = Json.parse("""[1, null, "", 4]""")
            val value2 = Json.parse("""[null, "", 4]""")
            val exception = the[TestFailedException] thrownBy value1.strictDeepMerge(value2)
            exception.getMessage() shouldBe s"Cannot merge 1 and null at path (0) in $value1 and $value2"
          }

          "when two incompatible arrays have the same length" - {
            "and they differ at index 0" in {
              val value1 = Json.parse("""[1, 2]""")
              val value2 = Json.parse("""[0, 2]""")
              val exception = the[TestFailedException] thrownBy value1.strictDeepMerge(value2)
              exception.getMessage() shouldBe s"Cannot merge 1 and 0 at path (0) in $value1 and $value2"
            }
            "and they differ at index 1" in {
              val value1 = Json.parse("""[0, 1, 3]""")
              val value2 = Json.parse("""[0, 2, 3]""")
              val exception = the[TestFailedException] thrownBy value1.strictDeepMerge(value2)
              exception.getMessage() shouldBe s"Cannot merge 1 and 2 at path (1) in $value1 and $value2"
            }
          }

          "when the two arrays are identical and contain other arrays" in {
            val value = Json.parse("""[0, [1, [2, 3, 4, 5]], 6]""")
            value.strictDeepMerge(value) shouldBe value
          }
        }

        "for complex combinations" - {
          "which are compatible" in {
            val value1 = Json.parse(
              """{
                |  "ka": [
                |    null,
                |    false,
                |    true,
                |    {
                |      "ka3a": false
                |    },
                |    [1, 2, 3]
                |  ],
                |  "kb2": "vb2",
                |  "kc": {
                |    "kca": [1, 2, 3, 4]
                |  }
                |}""".stripMargin
            )
            val value2 = Json.parse(
              """{
                |  "ka": [
                |    null,
                |    false,
                |    true,
                |    {
                |      "ka3b": true
                |    },
                |    [1, 2, 3, 4]
                |  ],
                |  "kb1": "vb1",
                |  "kc": {
                |    "kca": [1, 2, 3.0]
                |  }
                |}""".stripMargin
            )
            val reuslt = Json.parse(
              """{
                |  "ka": [
                |    null,
                |    false,
                |    true,
                |    {
                |      "ka3a": false,
                |      "ka3b": true
                |    },
                |    [1, 2, 3, 4]
                |  ],
                |  "kb1": "vb1",
                |  "kb2": "vb2",
                |  "kc": {
                |    "kca": [1, 2, 3, 4]
                |  }
                |}""".stripMargin
            )
            value1.strictDeepMerge(value2) shouldBe reuslt
          }
        }
      }
    }

    "RichJsValueWithPrinting" - {
      import JsonAssertionOps.RichJsValueWithPrinting

      ".indentedString" - {
        "works on a complex JSON value" in {
          // The default JSON pretty printer does put array elements on multiple lines, which can cause
          //  readability issues when diffing.
          val jsonString =
            """{
              |  "field1" : [
              |    "abc",
              |    1,
              |    false,
              |    null,
              |    true,
              |    {
              |      "field1" : "value"
              |    },
              |    {
              |      "field2" : "value"
              |    }
              |  ],
              |  "field2" : [
              |    "def"
              |  ],
              |  "field3" : [
              |    {
              |      "field1" : "value"
              |    },
              |    {
              |      "field2" : "value"
              |    }
              |  ]
              |}""".stripMargin

          val json = Json.parse(jsonString)

          json.indentedString shouldBe jsonString
        }
      }

      ".diffableString" - {
        "for an object that has all the possible types of JSON values" in {
          val jsonString =
            """{
              |  "field4": "value 1",
              |  "field2": "value 2",
              |  "field5": [
              |    456,
              |    123,
              |    { "field2": "value 1", "field3": "value 2", "field1": "value 3" },
              |    789,
              |    false,
              |    true,
              |    null
              |  ],
              |  "field1": "value 4",
              |  "field3": "value 5",
              |  "field6": "value 6"
              |}
              |""".stripMargin

          val json = Json.parse(jsonString)

          json.diffableString shouldBe
            """{
              |  "field1" : "value 4",
              |  "field2" : "value 2",
              |  "field3" : "value 5",
              |  "field4" : "value 1",
              |  "field5" : [
              |    456,
              |    123,
              |    {
              |      "field1" : "value 3",
              |      "field2" : "value 1",
              |      "field3" : "value 2"
              |    },
              |    789,
              |    false,
              |    true,
              |    null
              |  ],
              |  "field6" : "value 6"
              |}""".stripMargin
        }
      }

    }

    "RichJsValueWithSorting" - {
      import JsonAssertionOps.RichJsValueWithSorting

      ".sortedFields" - {
        "with on a simple JSON object" in {
          val json = Json.parse(
            """{
              |  "field4" : "value 1",
              |  "field2" : "value 2",
              |  "field5" : "value 3",
              |  "field1" : "value 4",
              |  "field3" : "value 5",
              |  "field6" : "value 6"
              |}
              |""".stripMargin
          )

          json.sortedFields shouldBe Json.parse(
            """{
              |  "field1" : "value 4",
              |  "field2" : "value 2",
              |  "field3" : "value 5",
              |  "field4" : "value 1",
              |  "field5" : "value 3",
              |  "field6" : "value 6"
              |}
              |""".stripMargin
          )
        }

        "with a nested JSON object that also has arrays" in {
          val json = Json.parse(
            """{
              |  "field4" : "value 1",
              |  "field2" : "value 2",
              |  "field5" : [
              |    456,
              |    123,
              |    {
              |      "field2": "value 1",
              |      "field3": "value 2",
              |      "field1": "value 3"
              |    },
              |    789,
              |    false,
              |    true,
              |    null
              |  ],
              |  "field1" : "value 4",
              |  "field3" : "value 5",
              |  "field6" : "value 6"
              |}
              |""".stripMargin
          )

          json.sortedFields shouldBe Json.parse(
            """{
              |  "field1" : "value 4",
              |  "field2" : "value 2",
              |  "field3" : "value 5",
              |  "field4" : "value 1",
              |  "field5" : [
              |    456,
              |    123,
              |    {
              |      "field1": "value 3",
              |      "field2": "value 1",
              |      "field3": "value 2"
              |    },
              |    789,
              |    false,
              |    true,
              |    null
              |  ],
              |  "field6" : "value 6"
              |}
              |""".stripMargin
          )
        }
      }
    }
  }
}
