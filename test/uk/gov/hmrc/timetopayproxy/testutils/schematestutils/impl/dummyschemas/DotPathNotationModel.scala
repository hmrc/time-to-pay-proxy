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

package uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl.dummyschemas

import cats.data.Validated.Valid
import play.api.libs.json.*
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl.DebtTransSchemaValidator.OpenApi3PathSchema
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl.dummyschemas.DotPathNotationModel.MultichoiceField

/** This class and its schemas will demonstrate the dot notation for accessing schemas.
  * Only to be used to test the schema test utilities, since it has absolutely no business semantics.
  */
private[impl] final case class DotPathNotationModel(multichoiceField: MultichoiceField)

private[impl] object DotPathNotationModel {
  implicit val format: OFormat[DotPathNotationModel] = Json.format[DotPathNotationModel]

  def openApiSchema: OpenApi3PathSchema =
    new OpenApi3PathSchema(
      openApiYamlFilename =
        "test/resources/schemas/general/dot-path-notation/openapi-schema-for-dot-path-notation.yaml",
      subschemaPath = "paths./example/route/endpoint.post.requestBody.content.application/json.schema",
      metaSchemaValidation = Some(Valid(())),
      restrictAdditionalProperties = true
    )

  sealed trait MultichoiceField
  object MultichoiceField {
    implicit val reads: Reads[MultichoiceField] = (json: JsValue) =>
      json.validate[ClassForDotNotationSchemaInteger] match {
        case success @ JsSuccess(_, _) => success
        case JsError(firstErrors)      =>
          json.validate[ClassForDotNotationSchemaBoolean] match {
            case success @ JsSuccess(_, _) => success
            case JsError(secondErrors)     => JsError(firstErrors ++ secondErrors)
          }
      }

    implicit val writes: Writes[MultichoiceField] = Writes {
      case ClassForDotNotationSchemaInteger(integerField) => Json.toJson(integerField)
      case ClassForDotNotationSchemaBoolean(booleanField) => Json.toJson(booleanField)
    }
  }

  private[impl] final case class ClassForDotNotationSchemaInteger(integerField: Int) extends MultichoiceField

  private[impl] object ClassForDotNotationSchemaInteger {
    implicit val format: OFormat[ClassForDotNotationSchemaInteger] = Json.format[ClassForDotNotationSchemaInteger]
  }

  private[impl] final case class ClassForDotNotationSchemaBoolean(booleanField: Boolean) extends MultichoiceField

  private[impl] object ClassForDotNotationSchemaBoolean {
    implicit val format: OFormat[ClassForDotNotationSchemaBoolean] = Json.format[ClassForDotNotationSchemaBoolean]
  }
}
