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
import com.networknt.schema.SpecVersion
import play.api.libs.json.{ Json, OFormat }
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl.DebtTransSchemaValidator.{ OpenApi3DerivedSchema, SimpleJsonSchema }

/** This class and its schemas will demonstrate the odd behaviour of `BigInt` in Play JSON.
  * Only to be used to test the schema test utilities, since it has absolutely no business semantics.
  */
private[impl] final case class ClassWithBigInteger(needsToBeInteger: BigInt)

private[impl] object ClassWithBigInteger {
  def jsonSchemaV4: SimpleJsonSchema =
    new SimpleJsonSchema(
      jsonSchemaFilename = "test/resources/schemas/general/class-with-big-integer/json-schema-v4.json",
      SpecVersion.VersionFlag.V4,
      metaSchemaValidation = Some(Valid(()))
    )

  def jsonSchemaV7: SimpleJsonSchema =
    new SimpleJsonSchema(
      jsonSchemaFilename = "test/resources/schemas/general/class-with-big-integer/json-schema-v7.json",
      SpecVersion.VersionFlag.V7,
      metaSchemaValidation = Some(Valid(()))
    )

  def openApiSchema: OpenApi3DerivedSchema =
    new OpenApi3DerivedSchema(
      openApiYamlFilename = "test/resources/schemas/general/class-with-big-integer/openapi-schema.yml",
      defaultJsonSubschemaName = "MainObject",
      metaSchemaValidation = Some(Valid(())),
      restrictAdditionalProperties = true
    )

  implicit def format: OFormat[ClassWithBigInteger] = Json.format[ClassWithBigInteger]
}
