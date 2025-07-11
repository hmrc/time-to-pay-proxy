/*
 * Copyright 2024 HM Revenue & Customs
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

import com.fasterxml.jackson.databind.JsonNode
import org.openapi4j.parser.OpenApi3Parser
import org.openapi4j.parser.model.v3.OpenApi3

import java.nio.file.Paths

abstract class ApiSchema(filename: String, defaultSchemaName: String) extends OpenApi3Parser {

  val fullSchema: OpenApi3 = parse(Paths.get(filename).toFile, false)
  val defaultSchema: JsonNode = fullSchema.getComponents.getSchema(defaultSchemaName).toNode

}

object AffordableQuotesRequestSchema
    extends ApiSchema(
      filename = "resources/public/api/conf/1.0/application.yaml",
      defaultSchemaName = "AffordableQuotesRequest"
    )

object AffordableQuotesResponseSchema
    extends ApiSchema(
      filename = "resources/public/api/conf/1.0/application.yaml",
      defaultSchemaName = "AffordableQuotesResponse"
    )

object ChargeInfoRequestSchema extends ApiSchema(
      filename = "resources/public/api/conf/1.0/yamlSchemas/time-to-pay-v1.0.10-proposedA.yaml",
      defaultSchemaName = "TTPChargeInfoRequest"
    )

object ChargeInfoResponseSchema extends ApiSchema(
      filename = "resources/public/api/conf/1.0/yamlSchemas/time-to-pay-v1.0.10-proposedA.yaml",
      defaultSchemaName = "TTPChargeInfoResponse"
    )
