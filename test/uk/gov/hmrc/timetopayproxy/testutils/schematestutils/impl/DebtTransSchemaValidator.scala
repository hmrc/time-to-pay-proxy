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

package uk.gov.hmrc.timetopayproxy.testutils.schematestutils.impl

import cats.data.ValidatedNel
import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper }
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.networknt.schema.regex.JDKRegularExpressionFactory
import com.networknt.schema.{ JsonSchema, JsonSchemaFactory, SchemaValidatorsConfig, SpecVersion }
import org.openapi4j.core.model.v3.OAI3
import org.openapi4j.core.validation.ValidationResults
import org.openapi4j.parser.OpenApi3Parser
import org.openapi4j.parser.model.v3.{ OpenApi3, Schema }
import org.openapi4j.schema.validator.v3.{ SchemaValidator, ValidationOptions }
import org.openapi4j.schema.validator.{ ValidationContext, ValidationData }
import org.scalactic.source.Position
import org.scalatest.Assertions.fail
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsString, JsValue }

import java.nio.file.Paths
import java.util.Locale
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try, Using }

/** Provides a JSON schema for validating JSON requests/responses. See the companion for the predefined instances. */
sealed trait DebtTransSchemaValidator {

  /** Validates a given `JsonNode`. Unlikely to be useful in tests, but it's the most basic thing we can do. */
  protected def validateAndGetErrors(jsonNode: JsonNode)(implicit pos: Position): List[String]

  /** Validates JSON given as a `String`. */
  final def validateJsonAndGetErrors(json: String)(implicit pos: Position): List[String] = {
    val jsonNode: JsonNode = DebtTransSchemaValidator.InternalUtils.jsonToJsonNode(json)
    validateAndGetErrors(jsonNode)
  }

  /** Converts YAML `String` to JSON, then validates it. */
  final def validateYamlAndGetErrors(yaml: String)(implicit pos: Position): List[String] = {
    val jsonNode: JsonNode = DebtTransSchemaValidator.InternalUtils.yamlToJsonNode(yaml)
    validateAndGetErrors(jsonNode)
  }

  /** Converts a given `JsValue` to a `String`, then validates it. */
  final def validateAndGetErrors(json: JsValue)(implicit pos: Position): List[String] = {
    val jsonString: String = StableStringGenerator.stableStringForSchemaValidator(json)
    validateJsonAndGetErrors(jsonString)
  }

  /** Reads YAML or JSON from a file, then validates it. */
  final def validateFromPathAndGetErrors(jsonOrYamlPath: String)(implicit pos: Position): List[String] = {
    val jsonNode = DebtTransSchemaValidator.InternalUtils.readJsonNode(jsonOrYamlPath = jsonOrYamlPath)
    validateAndGetErrors(jsonNode)
  }

}

object DebtTransSchemaValidator {

  /** @param jsonSchemaFilename can be a JSON or a YAML file. Note that this is NOT an OpenAPI spec. */
  final class SimpleJsonSchema private[schematestutils] (
    jsonSchemaFilename: String,
    version: SpecVersion.VersionFlag,
    metaSchemaValidation: Option[ValidatedNel[String, Unit]]
  ) extends DebtTransSchemaValidator {

    /** Constructing this class will automatically verify it against the meta-schema. */
    metaSchemaValidation match {
      case None => ()
      case Some(expectedValidationResult) =>
        val validator = MetaSchemas.jsonSchemaSchema(version)
        val errors: List[String] = validator.validateFromPathAndGetErrors(jsonOrYamlPath = jsonSchemaFilename)

        withClue(s"Validate $jsonSchemaFilename against $validator\n\n") {
          errors shouldBe expectedValidationResult.fold[List[String]](_.toList, { case () => Nil })
        }
    }

    private val jsonSchema: JsonSchema = {
      val schemaFactory: JsonSchemaFactory = JsonSchemaFactory.getInstance(version)

      val config = SchemaValidatorsConfig
        .builder()
        .regularExpressionFactory(
          // Without depending on other libraries, it's not possible to validate ECMAScript regular expressions.
          // Not much point anyway, because the abandoned OpenAPI validator library doesn't know how to use them.
          JDKRegularExpressionFactory.getInstance()
        )
        .build()

      schemaFactory.getSchema(
        InternalUtils.readJsonNode(jsonOrYamlPath = jsonSchemaFilename),
        config
      )
    }

    protected def validateAndGetErrors(json: JsonNode)(implicit pos: Position): List[String] =
      jsonSchema.validate(json).asScala.map(_.toString).toList.sorted

    override def toString: String = s"""${getClass.getSimpleName}(${JsString(jsonSchemaFilename)}, $version)"""
  }

  final class OpenApi3DerivedSchema private[schematestutils] (
    openApiYamlFilename: String,
    defaultJsonSubschemaName: String,
    metaSchemaValidation: Option[ValidatedNel[String, Unit]],
    restrictAdditionalProperties: Boolean
  ) extends DebtTransSchemaValidator {

    /** The full context of the schema, including any additional definitions or 'components'. */
    private val fullOpenApiSchemaMutable: OpenApi3 = {
      val openApi3Parser: OpenApi3Parser = new OpenApi3Parser()
      val parsedOpenApiSpec = openApi3Parser.parse(Paths.get(openApiYamlFilename).toFile, false)

      /** Fix the regular expression bug in the schema validator library.
        * The regex flavour SHOULD be ECMA-262, but the library uses the Java flavour to validate regexes.
        * The biggest difference is that the YAML is allowed to declare {{{[[]}}},
        *   but Java forbids it without escaping: {{{[\[]}}}
        *
        * This is the faulty YAML entry that initially required this hack:
        * {{{
        *   addressLine1:
        *     type: string
        *     maxLength: 35
        *     example: "ADDRESS LINE 1"
        *     pattern: '^[a-zA-Z0-9 -/:-@[-`]{1,35}$'
        *     description: "Incoming address line 1 from ETMP"
        * }}}
        */
      def changeRegexesToFollowJavaFlavour(): Unit = {

        /** Schemas can contain other schemas. */
        def recursivelyWorkSchema(schemaEntry: (String, Schema)): Unit = {
          val (schemaName, schema) = schemaEntry

          Option(schema.getProperties).foreach { nestedSchemas =>
            nestedSchemas.asScala.foreach { nestedSchemaEntry: (String, Schema) =>
              recursivelyWorkSchema(nestedSchemaEntry)
            }
          }

          Option(schema.getPattern).foreach { oldPattern: String =>
            val newPattern = RegexFlavourTranslator.ecmaScriptRegexFlavourToJavaFlavour(
              esPattern = oldPattern,
              locationContext = s"schema ${JsString(schemaName)} of file $openApiYamlFilename"
            )
            schema.setPattern(newPattern)
          }
        }

        parsedOpenApiSpec.getComponents.getSchemas.asScala.foreach { schemaEntry: (String, Schema) =>
          recursivelyWorkSchema(schemaEntry)
        }
      }

      changeRegexesToFollowJavaFlavour()
      parsedOpenApiSpec
    }

    metaSchemaValidation match {
      case None => ()
      case Some(expectedValidationResult) =>
        val actualVersion = fullOpenApiSchemaMutable.getOpenapi
        val validator = MetaSchemas.yamlSchemaSchema(version = actualVersion)

        val errors = validator.validateFromPathAndGetErrors(jsonOrYamlPath = openApiYamlFilename)

        withClue(s"Validate $openApiYamlFilename against $validator\n\n") {
          errors shouldBe expectedValidationResult.fold[List[String]](_.toList, { case () => Nil })
        }
    }

    /** The JSON schema corresponding to the root of the JSON we want to validate. */
    private val defaultJsonSubschema: JsonNode = {
      val maybeInnerSchema = Option(fullOpenApiSchemaMutable.getComponents.getSchema(defaultJsonSubschemaName))
      maybeInnerSchema match {
        case Some(innerSchema) => innerSchema.toNode
        case None              => fail(s"Could not find inner/default schema: $this")
      }
    }

    protected def validateAndGetErrors(jsonNode: JsonNode)(implicit pos: Position): List[String] = {
      val outputMutable: ValidationData[_] = new ValidationData()

      Try {
        val validationContext =
          new ValidationContext[OAI3](fullOpenApiSchemaMutable.getContext)
            .setOption(ValidationOptions.ADDITIONAL_PROPS_RESTRICT, restrictAdditionalProperties)

        val validator = new SchemaValidator(validationContext, null, defaultJsonSubschema)
        val _: Boolean = validator.validate(jsonNode, outputMutable)
      } match {
        case Failure(e) =>
          fail(s"Failed to run validation; check that the file is valid: $openApiYamlFilename\n", e)
        case Success(()) =>
          val validationItems: List[ValidationResults.ValidationItem] = outputMutable.results().items().asScala.toList

          validationItems.map(e => e.toString).sorted
      }
    }

    override def toString: String =
      s"""${getClass.getSimpleName}(${JsString(defaultJsonSubschemaName)} in $openApiYamlFilename )"""
  }

  /** Assortment of utilities needed to make the validation work. */
  private object InternalUtils {

    def readJsonNode(jsonOrYamlPath: String)(implicit pos: Position): JsonNode = {
      val fileContent = Using(Source.fromFile(jsonOrYamlPath))(_.mkString).get

      jsonOrYamlPath.toLowerCase(Locale.ENGLISH) match {
        case s"$_.json"             => jsonToJsonNode(fileContent)
        case s"$_.yaml" | s"$_.yml" => yamlToJsonNode(fileContent)
        case _ =>
          fail(s"Cannot read file because it doesn't have a json/yaml/yml file extension: $jsonOrYamlPath")
      }
    }

    def yamlToJsonNode(yaml: String): JsonNode = new ObjectMapper(new YAMLFactory()).readTree(yaml)

    def jsonToJsonNode(json: String): JsonNode = new ObjectMapper().readTree(json)
  }

}
