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

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.networknt.schema.{JsonSchema, JsonSchemaFactory, SpecVersion}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.timetopayproxy.models._

import java.nio.file.Paths
import scala.jdk.CollectionConverters.CollectionHasAsScala

class SchemaValidatorSpec extends AnyWordSpec with Matchers {

  lazy val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
  lazy val objectMapper = new ObjectMapper

  def loadSchema(path: String): JsonSchema =
    schemaFactory.getSchema(Paths.get(path).toUri)

  def loadJson(path: String): JsonNode =
    objectMapper.readTree(Paths.get(path).toFile)

  "The generateQuote request schema" should {

    "be valid according to the meta schema" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/metaSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/schemas/generate/postGenerateRequestSchema.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

    "validate an example request" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/generate/postGenerateRequestSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/examples/generate/postGenerateRequest.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

    "should parse to the model" in {
      val raw = scala.io.Source
        .fromFile("resources/public/api/conf/1.0/examples/generate/postGenerateRequest.json")
        .mkString
      Json.parse(raw).as[GenerateQuoteRequest]
    }
  }

  "The generateQuote response schema" should {

    "be valid according to the meta schema" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/metaSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/schemas/generate/postGenerateResponseSchema.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

    "validate an example response" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/generate/postGenerateResponseSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/examples/generate/postGenerateResponse.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

    "should parse to the model" in {
      val raw = scala.io.Source
        .fromFile("resources/public/api/conf/1.0/examples/generate/postGenerateResponse.json")
        .mkString
      Json.parse(raw).as[GenerateQuoteResponse]
    }
  }

  "The createPlan request schema" should {

    "be valid according to the meta schema" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/metaSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/schemas/create/postCreateRequestSchema.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

    "validate an example request" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/create/postCreateRequestSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/examples/create/postCreateRequest.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

    "should parse to the model" in {
      val raw = scala.io.Source
        .fromFile("resources/public/api/conf/1.0/examples/create/postCreateRequest.json")
        .mkString
      Json.parse(raw).as[CreatePlanRequest]
    }
  }

  "The viewPlan response schema" should {

    "be valid according to the meta schema" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/metaSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/schemas/view/getViewResponseSchema.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

    "validate an example response" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/view/getViewResponseSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/examples/view/getViewResponse.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

    "should parse to the model" in {
      val raw = scala.io.Source
        .fromFile("resources/public/api/conf/1.0/examples/view/getViewResponse.json")
        .mkString
      Json.parse(raw).as[ViewPlanResponseDropTwo]
    }
  }

  "The updatePlan request schema" should {

    "be valid according to the meta schema" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/metaSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/schemas/update/putUpdateRequestSchema.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

    "validate an example request" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/update/putUpdateRequestSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/examples/update/putUpdateRequest.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

  }

  "The updatePlan response schema" should {

    "be valid according to the meta schema" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/metaSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/schemas/update/putUpdateResponseSchema.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

    "validate an example response" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/update/putUpdateResponseSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/examples/update/putUpdateResponse.json")
      val errors = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }

  }
}
