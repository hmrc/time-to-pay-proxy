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

import cats.data.NonEmptyList
import com.fasterxml.jackson.core.util.{ DefaultIndenter, DefaultPrettyPrinter }
import com.fasterxml.jackson.databind.{ ObjectMapper, SerializationFeature }
import org.scalactic.source.Position
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json._
import play.api.libs.json.jackson.PlayJsonMapperModule

import scala.collection.immutable.SortedMap

object JsonAssertionOps {

  implicit final class RichJsValueWithAssertions(json: JsValue) {

    /** Like ScalaTest's `shouldBe`, attempts a readable comparison of two `JsValue`,
      *   with complete and deterministic error messages to help with large JSON diffs.
      *
      * <li>Used like you would use `shouldBe`. Failures and passes are consistent with `shouldBe`.</li>
      * <li>Always sorts object fields before diffing them, for clarity and copy-pastability.</li>
      * <li>Prints the failure error in a ScalaTest-consistent format that allows a diff in IDEs that support it.</li>
      * <li>
      * Usage:
      * <pre>
      * import uk.gov.hmrc.timetopay.testutils.JsonAssertionOps._
      * // ...
      * myJsValue1 shouldBeEquivalentTo myJsValue2 // Same as you would use `shouldBe`.
      * </pre>
      * </li>
      */
    def shouldBeEquivalentTo(other: JsValue)(implicit pos: Position): Unit =
      if (json != other) {
        val diffableJson1 = json.diffableString + "\n"
        val diffableJson2 = other.diffableString + "\n"

        // The exact phrasing "was not equal to" is how ScalaTest phrases differences
        //   and allows modern IDEs like IntelliJ to show a diff.
        fail(s"$diffableJson1 was not equal to $diffableJson2")
      }

  }

  implicit final class RichJsValueWithSorting(json: JsValue) {

    /** Deeply sorts the object fields of a JSON value, for reliable diffing. */
    def sortedFields: JsValue =
      json match {
        case JsNull            => json
        case _: JsBoolean      => json
        case _: JsNumber       => json
        case _: JsString       => json
        case JsArray(children) => JsArray(children.map(_.sortedFields))
        case JsObject(fields) =>
          JsObject(SortedMap.from(fields.view.mapValues(_.sortedFields)))
      }
  }

  implicit final class RichJsValueWithPrinting(json: JsValue) {

    /** All this does is print it in a way that's nice to diff.
      * Sorts the fields of all JSON objects, but does not reorder the array elements.
      * This string conversion preserves equality of JSON objects.
      */
    def diffableString: String = json.sortedFields.indentedString

    /** Pretty-prints the JSON by using indentation in both arrays and objects.
      * Jackson's (and Play's) default pretty-printer does not indent arrays.
      */
    def indentedString: String =
      RichJsValueWithPrinting.mapper
        .writer(RichJsValueWithPrinting.normalJsonPrettyPrinter)
        .writeValueAsString(json)
  }

  private object RichJsValueWithPrinting {

    /** This is created again because Jackson's (Play's) default one is private. */
    private val mapper: ObjectMapper =
      new ObjectMapper()
        .registerModule(new PlayJsonMapperModule(JsonParserSettings.settings))
        .enable(SerializationFeature.INDENT_OUTPUT)

    /** Pretty-prints JSON using consistent two-space indentation for objects and arrays. */
    private def normalJsonPrettyPrinter: DefaultPrettyPrinter = {
      // By default, we're using system-dependent indentation and we don't indent arrays.
      // Both of those "features" are a problem when comparing JSON values in tests.
      def indenter = new DefaultIndenter("  ", "\n")

      new DefaultPrettyPrinter()
        .withArrayIndenter(indenter)
        .withObjectIndenter(indenter)
    }

  }

  implicit final class RichJsValueWithMergingOperations(mainJson1: JsValue) {

    /** Deeply merges one JSON object/array with another.
      * This is a test utility that fails on any clashes.
      * Does not reject identical values that appear on both sides.
      *
      * "Strict" because it will not delete any data.
      * "Deep" because it will go through both arrays and objects.
      *
      * Note that this logic is not appropriate for production code.
      * Here we need to throw in order to fail incorrect tests, but production code would use ValidatedNel.
      */
    def strictDeepMerge(mainJson2: JsValue)(implicit pos: Position): JsValue = {

      def recursive(json1: JsValue, json2: JsValue, path: JsPath): JsValue =
        (json1, json2) match {
          case (obj1: JsObject, obj2: JsObject) =>
            val allKeys: Set[String] = obj1.keys.toSet ++ obj2.keys

            JsObject(
              SortedMap.from(
                allKeys.map { key =>
                  val values = NonEmptyList.fromListUnsafe(List(obj1.value.get(key), obj2.value.get(key)).flatten)

                  val maybeNewValue = values.reduce((json1, json2) => recursive(json1, json2, path \ key))
                  (key, maybeNewValue)
                }
              )
            )

          case (arr1: JsArray, arr2: JsArray) =>
            val indexes = 0 until Math.max(arr1.value.size, arr2.value.size)

            JsArray(
              indexes.map { idx =>
                val maybeValue1 = arr1.value.drop(idx).headOption
                val maybeValue2 = arr2.value.drop(idx).headOption
                val values = NonEmptyList.fromListUnsafe(List(maybeValue1, maybeValue2).flatten)

                values.reduce((json1, json2) => recursive(json1, json2, path \ idx))
              }
            )

          case (unknown1, unknown2) =>
            if (unknown1 == unknown2)
              unknown1
            else {
              val pathStr = if (path.path.isEmpty) "/" else path.toString()
              fail(s"Cannot merge $unknown1 and $unknown2 at path $pathStr in $mainJson1 and $mainJson2")
            }
        }

      recursive(mainJson1, mainJson2, JsPath)
    }
  }

}
