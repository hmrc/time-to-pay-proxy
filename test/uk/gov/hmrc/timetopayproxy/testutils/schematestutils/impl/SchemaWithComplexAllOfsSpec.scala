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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.Json
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl.dummyschemas.SchemaWithComplexAllOfs

class SchemaWithComplexAllOfsSpec extends AnyFreeSpec {

  "Schema validations using a schema with allOf (complex types)" - {
    "should pass if additional properties are not restricted" in {

      val allOfJson = """{
                        |"string": "test1",
                        |"number": 1,
                        |"stringArray": ["test1"],
                        |"numberArray": [1]
                        |}""".stripMargin

      val errors =
        SchemaWithComplexAllOfs.openApiSchemaWithoutAdditionalPropertiesRestricted.validateAndGetErrors(
          Json.parse(allOfJson)
        )

      errors shouldBe Nil
    }

    "should produce errors if additional properties are restricted" in {

      val allOfJson = """{
                        |"string": "test1",
                        |"number": 1,
                        |"stringArray": ["test1"],
                        |"numberArray": [1]
                        |}""".stripMargin

      val errors =
        SchemaWithComplexAllOfs.openApiSchemaWithAdditionalPropertiesRestricted.validateAndGetErrors(
          Json.parse(allOfJson)
        )

      errors should not be Nil
    }
  }
}
