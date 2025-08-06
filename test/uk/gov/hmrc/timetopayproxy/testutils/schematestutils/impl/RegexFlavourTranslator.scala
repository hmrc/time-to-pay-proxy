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

import play.api.Logger

import scala.util.matching.Regex

private[impl] object RegexFlavourTranslator {

  private val logger = Logger(this.getClass)

  def ecmaScriptRegexFlavourToJavaFlavour(esPattern: String, locationContext: String): String = {
    // Matches every understandable square bracket character class, whether or not it's valid in Java or ES.
    //   i.e. every backslash-and-char and every unescaped char that's not a ']', surrounded by '[' and ']'.
    // Starts with an assertion that the first matched square bracket is not escaped. Escaped '[' are just normal chars.
    //   i.e. the opening '[' is not preceded by an even number of backslashes '\'.
    // Ignores the special pattern `[]]` because it's not valid in ECMAScript and means something different in Java.
    val customCharacterClassMatchingPattern = """(?<=(?<!\\)(?:\\\\){0,1000})\[((?:\\.|[^\\\]])+)]""".r

    // Add a backslash to every unescaped '[' inside the outer '[' and ']'. In Java it's invalid to not escape it.
    val javaPattern: String = customCharacterClassMatchingPattern.replaceAllIn(
      esPattern,
      replacer = (m: Regex.Match) => {
        // Add a backslash before every unescaped '['. (When it's NOT preceded by an even number of backslashes '\')
        val newCharacterClassContent = m.group(1).replaceAll("""(?<=(?<!\\)(?:\\\\){0,1000})\[""", """\\$0""")

        val result = s"[$newCharacterClassContent]"

        // The method we're in will process our result's escape sequences, so we need to double every backslash.
        result.replaceAll("""\\""", """\\\\""")
      }
    )

    if (esPattern != javaPattern) {
      logger.warn(
        // Newlines around the text to make it more readable, since it's already got newlines in the middle.
        s"""
           |Updated OpenAPI regex flavour from EcmaScript to Java: $esPattern ==> $javaPattern
           |  in $locationContext
           |""".stripMargin
      )
    }

    javaPattern
  }

}
