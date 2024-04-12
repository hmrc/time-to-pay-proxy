/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.support

import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper }
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.networknt.schema.{ JsonSchema, JsonSchemaFactory, SpecVersion, ValidationMessage }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.timetopayproxy.models._

import java.io.File
import java.nio.file.Paths
import scala.jdk.CollectionConverters.CollectionHasAsScala

class YamlSchemaValidatorSpec extends AnyWordSpec with Matchers {

  lazy val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
  lazy val objectMapper = new ObjectMapper

  def loadJson(path: String): JsonNode =
    objectMapper.readTree(Paths.get(path).toFile)

  def loadYamlAndConvertToJsonSchema(path: String): JsonSchema = {
    schemaFactory.getSchema(loadYamlAndConvertToJsonNode(path))
  }

  def loadYamlAndConvertToJsonNode(path: String): JsonNode = {
    val yamlReader = new ObjectMapper(new YAMLFactory()).registerModule(DefaultScalaModule)
    yamlReader.readTree(Paths.get(path).toFile)
  }

  "application.yaml" should {
    "be valid according to the meta schema" in {
      val schema: JsonSchema = loadYamlAndConvertToJsonSchema("resources/public/api/conf/1.0/yamlSchemas/metaSchema.yaml")
      val json: JsonNode = loadYamlAndConvertToJsonNode("resources/public/api/conf/1.0/application.yaml")
      val errors: Set[ValidationMessage] = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }
  }

  "AffordableQuotes request" should {
    "be valid according to application.yaml" in {
      val yaml: JsonSchema = loadYamlAndConvertToJsonSchema("resources/public/api/conf/1.0/application.yaml")
      val json: JsonNode = loadJson("resources/public/api/conf/1.0/examples/affordableQuote/request.json")
      val errors: Set[ValidationMessage] = yaml.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }
  }

  "AffordableQuotes response" should {
    "be valid according to application.yaml" in {
      val yaml: JsonSchema = loadYamlAndConvertToJsonSchema("resources/public/api/conf/1.0/application.yaml")
      val json: JsonNode = loadJson("resources/public/api/conf/1.0/examples/affordableQuote/response.json")
      val errors: Set[ValidationMessage] = yaml.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }
  }

  "AffordableQuotesRequest model" should {
    "parse to and from json" when {
      "given only mandatory fields" in {}
      "given all fields" in {}
    }
  }

  "AffordableQuotesResponse model" should {
    "parse to and from json" when {
      "given only mandatory fields" in {}
      "given all fields" in {}
    }
  }
}
