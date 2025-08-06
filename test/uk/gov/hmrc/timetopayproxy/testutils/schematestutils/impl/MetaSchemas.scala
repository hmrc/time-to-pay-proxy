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

import com.networknt.schema.SpecVersion
import org.scalactic.source.Position
import org.scalatest.Assertions.fail
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl.DebtTransSchemaValidator.SimpleJsonSchema

/** Schemas to validate (arbitrary versions of) other schemas, be it OpenApi YAML, JSON etc.
  *
  * This is needed because schema validation libraries
  *   don't usually complain about misspelled optional fields or unrecognised fields.
  *   The only way to check for that is by validating them against a strict schema.
  */
private[impl] object MetaSchemas {

  def yamlSchemaSchema(version: String)(implicit pos: Position): SimpleJsonSchema =
    version match {
      case s"3.0.$_" =>
        // Downloaded from:
        //   https://github.com/OAI/OpenAPI-Specification/blob/bdf9337a8866bd2f568ff9c0f0e946d62473ecc7/schemas/v3.0/schema.yaml
        new SimpleJsonSchema(
          jsonSchemaFilename = "test/resources/schemas/general/openapi-meta-schema-v3.0.yaml",
          version = SpecVersion.VersionFlag.V4,
          metaSchemaValidation = None
        )
      case _ =>
        fail(
          s"Cannot validate YAML schema $version against a meta-schema because no such meta-schema has been added yet."
        )
    }

  /** JSON schema to validate the structure of a JSON schema.
    * @param version what JSON schema version we want this meta-schema to target.
    */
  def jsonSchemaSchema(version: SpecVersion.VersionFlag)(implicit pos: Position): SimpleJsonSchema =
    version match {
      case SpecVersion.VersionFlag.V4 =>
        new SimpleJsonSchema(
          jsonSchemaFilename = "test/resources/schemas/general/json-meta-schema-v4.json",
          // This version is independent of parameter. It's file-specific.
          SpecVersion.VersionFlag.V4,
          metaSchemaValidation = None
        )
      case SpecVersion.VersionFlag.V7 =>
        new SimpleJsonSchema(
          jsonSchemaFilename = "test/resources/schemas/general/json-meta-schema-v7.json",
          // This version is independent of parameter. It's file-specific.
          SpecVersion.VersionFlag.V7,
          metaSchemaValidation = None
        )
      case _ =>
        fail(
          s"Cannot validate JSON schema $version against a meta-schema because no such meta-schema has been added yet."
        )
    }
}
