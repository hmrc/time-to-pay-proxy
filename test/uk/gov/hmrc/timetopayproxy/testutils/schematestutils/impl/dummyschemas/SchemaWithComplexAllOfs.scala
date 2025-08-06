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

/*
 * Copyright 2025 HM Revenue & Customs
 *
 */

import cats.data.Validated.Valid
import play.api.libs.json.{ Json, OFormat }
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl.DebtTransSchemaValidator.OpenApi3DerivedSchema

/** This class and its schemas represent demonstrate the behaviour of `AllOf` in Play JSON.
  */
private[impl] final case class SchemaWithComplexAllOfs(
  string: String,
  number: Option[Int],
  stringArray: List[String],
  numberArray: List[Int]
)

private[impl] object SchemaWithComplexAllOfs {

  def openApiSchemaWithAdditionalPropertiesRestricted: OpenApi3DerivedSchema =
    new OpenApi3DerivedSchema(
      openApiYamlFilename = "test/resources/schemas/general/schema-with-complex-allOf/openapi-schema.yaml",
      defaultJsonSubschemaName = "MainObject",
      metaSchemaValidation = Some(Valid(())),
      restrictAdditionalProperties = true
    )

  def openApiSchemaWithoutAdditionalPropertiesRestricted: OpenApi3DerivedSchema =
    new OpenApi3DerivedSchema(
      openApiYamlFilename = "test/resources/schemas/general/schema-with-complex-allOf/openapi-schema.yaml",
      defaultJsonSubschemaName = "MainObject",
      metaSchemaValidation = Some(Valid(())),
      restrictAdditionalProperties = false
    )

  implicit def format: OFormat[SchemaWithComplexAllOfs] = Json.format[SchemaWithComplexAllOfs]
}
