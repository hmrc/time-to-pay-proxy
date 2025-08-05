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

import play.api.libs.json.{ JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue }

private[impl] object StableStringGenerator {

  /** Whether or not `1.23E+5` is an integer is apparently debatable. Mathematically, it is an integer.
    *   The JSON schema validator doesn't think so.
    *   What the dozens of production JSON libraries in other services think, we don't know.
    *
    * Play!'s standard `toString` operation will aggressively apply scientific notation for large `JsNumber` integers.
    *
    * This method attempts to sidestep that distinction by rendering all JSON integers without scientific notation.
    * The causes any large integer ending in a zero (e.g. 123456789012345678901234567890) to be
    *   displayed as 1.2345678901234567890123456789E+29.
    *   Our schema validator library doesn't think that 1.2345678901234567890123456789E+29 is an integer. It is.
    *
    * Doing so should make schema testing more stable, since the prod behaviour is not something easily verified.
    * This is a more natural implementation of `JsValue.toString`, to print very large integers as OBVIOUS integers.
    *   Most of the time the distinction will not matter.
    *   We do this by avoiding calls to `.toString` on `JsNumber`, delegating instead to the inner BigDecimal.
    */
  def stableStringForSchemaValidator(json: JsValue): String =
    json match {
      case JsNull =>
        JsNull.toString()
      case boolean: JsBoolean =>
        boolean.toString()
      case JsNumber(value) =>
        value.toString()
      case jsString: JsString =>
        jsString.toString()
      case JsArray(value) =>
        value.map(stableStringForSchemaValidator).mkString("[", ",", "]")
      case JsObject(underlying) =>
        underlying
          .map { case (name, value) =>
            s"${JsString(name)}:${stableStringForSchemaValidator(value)}"
          }
          .mkString("{", ",", "}")
    }
}
