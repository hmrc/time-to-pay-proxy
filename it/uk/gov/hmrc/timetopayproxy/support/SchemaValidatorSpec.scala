/*
 * Copyright 2022 HM Revenue & Customs
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
import com.networknt.schema.{ JsonSchema, JsonSchemaFactory, SpecVersion }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.timetopayproxy.models.GenerateQuoteResponse

import java.nio.file.Paths
import scala.collection.JavaConverters.asScalaSetConverter

class SchemaValidatorSpec extends AnyWordSpec with Matchers {

  lazy val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
  lazy val objectMapper = new ObjectMapper

  def loadSchema(path: String): JsonSchema =
    schemaFactory.getSchema(Paths.get(path).toUri)

  def loadJson(path: String): JsonNode =
    objectMapper.readTree(Paths.get(path).toFile)

  "The generateQuote response schema" should {

    "be valid according to the meta schema" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/metaSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/schemas/generate/postGenerateResponseSchema.json")
      val errors = schema.validate(json).asScala

      errors shouldEqual Set.empty
    }

    "validate an example request" in {
      val schema = loadSchema("resources/public/api/conf/1.0/schemas/generate/postGenerateResponseSchema.json")
      val json = loadJson("resources/public/api/conf/1.0/examples/generate/postGenerateResponse.json")
      val errors = schema.validate(json).asScala

      errors shouldEqual Set.empty
    }

    "should parse to the model" in {
      val raw = scala.io.Source
        .fromFile("resources/public/api/conf/1.0/examples/generate/postGenerateResponse.json")
        .mkString
      Json.parse(raw).as[GenerateQuoteResponse]
    }

  }

}
