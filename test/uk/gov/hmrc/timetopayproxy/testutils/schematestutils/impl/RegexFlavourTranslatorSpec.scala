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

final class RegexFlavourTranslatorSpec extends AnyFreeSpec {
  "RegexFlavourTranslator" - {

    ".ecmaScriptRegexFlavourToJavaFlavour" - {
      val locationContext = "Some Location Context"

      /** Wrap around regex strings before asserting equality because
        *   ScalaTest likes to add square brackets [] around mismatched characters to make the errors more 'readable'.
        *   We don't want that because it's not readable, not with so many important square brackets.
        */
      final case class RegexWrapper(regex: String)

      "for regexes that are valid in both ECMAScript and Java" - {
        final case class TestCase(unmodifiedRegex: String)(implicit val pos: Position)

        val validEsAndJavaRegexes: List[TestCase] = List(
          TestCase(""""""),
          TestCase("""^$"""),
          // This matches "A]B" in Java, but [] doesn't work in ES, so keep it the way it is.
          TestCase("""A[]]B"""),
          TestCase("""A[\[]B"""),
          TestCase("""A\[[\[]B"""),
          TestCase("""A[\\\[]B"""),
          TestCase("""A[\[]+B[\[]C"""),
          TestCase("""A[\\\[]+B[\\\[]C"""),
          TestCase("""A\[\[]+B\[\[]C"""),
          TestCase("""[\[][\\\[][\\\\\[]"""),
          // The following are examples taken from our specs and act more like regression tests.
          TestCase("""^[a-zA-Z]*$"""),
          TestCase("""^[0-9]{12}$|^.{0}$"""),
          TestCase("""^[A-Z0-9]{1,6}$"""),
          TestCase("""^[A-Za-z0-9]{1,10}$"""),
          TestCase("""^[a-zA-Z0-9 -/:-@\[-`]{1,35}$"""),
          TestCase("""^[A-Z0-9 ]{1,8}$"""),
          TestCase("""^[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}$"""),
          TestCase("""^(?:[A-Za-z\d+/]{4})*(?:[A-Za-z\d+/]{3}=|[A-Za-z\d+/]{2}==)?$"""),
          TestCase("""^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\.?[0-9]{0,9}Z$""")
        )

        for (regexModificationTestCase <- validEsAndJavaRegexes) {
          import regexModificationTestCase.{ pos, unmodifiedRegex }

          s"does not transform ES regex $unmodifiedRegex" in {
            val transformed: String =
              RegexFlavourTranslator.ecmaScriptRegexFlavourToJavaFlavour(
                unmodifiedRegex,
                locationContext
              )

            RegexWrapper(transformed) shouldBe RegexWrapper(unmodifiedRegex)
          }
        }
      }

      "for regexes that are only valid in ECMAScript, but not in Java" - {
        final case class TestCase(inputRegex: String, outputRegex: String)(implicit val pos: Position)

        val regexesThatNeedModification: List[TestCase] = List(
          TestCase("""A[[]B""", """A[\[]B"""),
          TestCase("""A[\\[]B""", """A[\\\[]B"""),
          TestCase("""A[[]+B[[]C""", """A[\[]+B[\[]C"""),
          TestCase("""A[\\[]+B[\\[]C""", """A[\\\[]+B[\\\[]C"""),
          TestCase("""A[\\\\[]+B[\\\\[]C""", """A[\\\\\[]+B[\\\\\[]C"""),
          TestCase("""[[][\\[][\\\\[]""", """[\[][\\\[][\\\\\[]"""),
          TestCase("""[\\\]\\\\\][[]""", """[\\\]\\\\\]\[\[]"""),
          // The following are examples taken from our specs and act more like regression tests.
          TestCase("""^[a-zA-Z0-9 -/:-@[-`]{1,35}$""", """^[a-zA-Z0-9 -/:-@\[-`]{1,35}$""")
        )

        for (regexModificationTestCase <- regexesThatNeedModification) {
          import regexModificationTestCase.{ inputRegex, outputRegex, pos }

          s"transforms ES regex $inputRegex into Java regex $outputRegex" in {
            val transformed: String =
              RegexFlavourTranslator.ecmaScriptRegexFlavourToJavaFlavour(inputRegex, locationContext)

            RegexWrapper(transformed) shouldBe RegexWrapper(outputRegex)
          }
        }
      }

    }
  }
}
