/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl.dummyschemas

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.*
import play.api.libs.json.*
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl.dummyschemas.DotPathNotationModel.{ ClassForDotNotationSchemaBoolean, ClassForDotNotationSchemaInteger }

class DotPathNotationModelSpec extends AnyFreeSpec {
  def reader: Reads[DotPathNotationModel] = implicitly[Reads[DotPathNotationModel]]

  "ClassForDotNotationSchemaInteger" - {
    val pathNotationIntegerJson: JsValue =
      Json.parse(
        """
          |{
          | "multichoiceField": {
          |   "integerField": 100
          |  }
          |}
          |""".stripMargin
      )

    val pathNotationIntegerModel: DotPathNotationModel = DotPathNotationModel(ClassForDotNotationSchemaInteger(100))

    "implicit json reader" - {
      "reads the expected json for a ClassForDotNotationSchemaInteger" in {
        println(pathNotationIntegerJson)
        reader.reads(pathNotationIntegerJson) shouldBe JsSuccess(pathNotationIntegerModel)
      }

      "reads according to the schema for ClassForDotNotationSchemaInteger" in {
        DotPathNotationModel.openApiSchema.validateAndGetErrors(pathNotationIntegerJson) shouldBe Nil
      }

      "reads an incorrectly formatted object" in {
        val json = Json.parse(
          """
            |{
            | "multichoiceField": {
            |  "wrongField": 100
            | }
            |}
            |""".stripMargin
        )

        reader.reads(json) shouldBe
          JsError(errors =
            List(
              (JsPath \ "multichoiceField" \ "integerField") -> List(JsonValidationError(List("error.path.missing"))),
              (JsPath \ "multichoiceField" \ "booleanField") -> List(JsonValidationError(List("error.path.missing")))
            )
          )
      }
    }
  }

  "ClassForDotNotationSchemaBoolean" - {
    val pathNotationBooleanJson: JsValue =
      Json.parse(
        """
          |{
          | "multichoiceField": {
          |   "booleanField": true
          |  }
          |}
          |""".stripMargin
      )

    val pathNotationBooleanModel: DotPathNotationModel = DotPathNotationModel(ClassForDotNotationSchemaBoolean(true))

    "implicit json reader" - {
      "reads the expected json for a ClassForDotNotationSchemaBoolean" in {
        reader.reads(pathNotationBooleanJson) shouldBe JsSuccess(pathNotationBooleanModel)
      }

      "reads according to the schema for ClassForDotNotationSchemaBoolean" in {
        DotPathNotationModel.openApiSchema.validateAndGetErrors(pathNotationBooleanJson) shouldBe Nil
      }

      "reads an incorrectly formatted object" in {
        val json = Json.parse(
          """
            |{
            | "multichoiceField": {
            |  "wrongField": false
            | }
            |}
            |""".stripMargin
        )
        reader.reads(json) shouldBe
          JsError(errors =
            List(
              (JsPath \ "multichoiceField" \ "integerField") -> List(JsonValidationError(List("error.path.missing"))),
              (JsPath \ "multichoiceField" \ "booleanField") -> List(JsonValidationError(List("error.path.missing")))
            )
          )
      }
    }
  }
}
