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
import com.fasterxml.jackson.databind.node.{ MissingNode, ObjectNode }
import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper }
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.networknt.schema.dialect.Dialects
import com.networknt.schema.path.{ NodePath, PathType }
import com.networknt.schema.{ Schema, SchemaRegistry, SchemaRegistryConfig, SpecificationVersion }
import com.networknt.schema.regex.JDKRegularExpressionFactory
import com.networknt.schema.utils.JsonNodes
import org.scalactic.source.Position
import org.scalatest.Assertions.fail
import org.scalatest.matchers.should.Matchers.*
import play.api.libs.json.{ JsString, JsValue }

import java.util.Locale
import scala.annotation.unused
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.Using

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
    version: SpecificationVersion,
    metaSchemaValidation: Option[ValidatedNel[String, Unit]]
  ) extends DebtTransSchemaValidator {

    /** Constructing this class will automatically verify it against the meta-schema. */
    metaSchemaValidation match {
      case None                           => ()
      case Some(expectedValidationResult) =>
        val validator = MetaSchemas.jsonSchemaSchema(version)
        val errors: List[String] = validator.validateFromPathAndGetErrors(jsonOrYamlPath = jsonSchemaFilename)

        withClue(s"Validate $jsonSchemaFilename against $validator\n\n") {
          errors shouldBe expectedValidationResult.fold[List[String]](_.toList, { case () => Nil })
        }
    }

    private val schema: Schema = {
      val config = SchemaRegistryConfig
        .builder()
        .regularExpressionFactory(
          // Without depending on other libraries, it's not possible to validate ECMAScript regular expressions.
          // Not much point anyway, because the abandoned OpenAPI validator library doesn't know how to use them.
          JDKRegularExpressionFactory.getInstance()
        )
        .build()

      val registry = SchemaRegistry.withDefaultDialect(
        version,
        builder => {
          builder.schemaRegistryConfig(config)
          ()
        }
      )

      registry.getSchema(InternalUtils.readJsonNode(jsonOrYamlPath = jsonSchemaFilename))
    }

    protected def validateAndGetErrors(json: JsonNode)(implicit pos: Position): List[String] =
      schema.validate(json).asScala.map(_.toString).toList.sorted

    override def toString: String = s"""${getClass.getSimpleName}(${JsString(jsonSchemaFilename)}, $version)"""
  }

  final class OpenApi3DerivedSchema private[schematestutils] (
    openApiYamlFilename: String,
    defaultJsonSubschemaName: String,
    metaSchemaValidation: Option[ValidatedNel[String, Unit]],
    restrictAdditionalProperties: Boolean
  ) extends DebtTransSchemaValidator {

    private val fullOpenApiNode: JsonNode =
      InternalUtils.readJsonNode(jsonOrYamlPath = openApiYamlFilename)

    /** Constructing this class will automatically verify it against the meta-schema. */
    metaSchemaValidation match {
      case None                           => ()
      case Some(expectedValidationResult) =>
        val actualVersion = fullOpenApiNode.path("openapi").asText("")
        val validator = MetaSchemas.yamlSchemaSchema(version = actualVersion)

        val errors = validator.validateFromPathAndGetErrors(jsonOrYamlPath = openApiYamlFilename)

        withClue(s"Validate $openApiYamlFilename against $validator\n\n") {
          errors shouldBe expectedValidationResult.fold[List[String]](_.toList, { case () => Nil })
        }
    }

    private val schema: Schema = {
      val componentsSchemasPath: NodePath =
        new NodePath(PathType.JSON_POINTER).append("components").append("schemas")

      val mutableRoot: JsonNode = fullOpenApiNode.deepCopy[JsonNode]
      val schemasNode: JsonNode =
        Option(JsonNodes.get[JsonNode](mutableRoot, componentsSchemasPath))
          .getOrElse(MissingNode.getInstance())

      validateOpenApiSchemaStructure(schemasNode)

      InternalUtils.rewriteRegexPatterns(schemasNode, openApiYamlFilename)

      if (restrictAdditionalProperties) {
        InternalUtils.injectAdditionalPropertiesFalse(schemasNode)
      }

      val config = InternalUtils.createSchemaRegistryConfig()

      val registry = InternalUtils.createSchemaRegistry(config)

      val documentSchema: Schema = registry.getSchema(mutableRoot)

      documentSchema.getSubSchema(componentsSchemasPath.append(defaultJsonSubschemaName))
    }

    private def validateOpenApiSchemaStructure(openApiSchemasNode: JsonNode): Unit = {
      if (openApiSchemasNode.isMissingNode)
        fail(s"No components/schemas found in $openApiYamlFilename")

      if (openApiSchemasNode.path(defaultJsonSubschemaName).isMissingNode)
        fail(s"Could not find subschema '$defaultJsonSubschemaName' in $openApiYamlFilename")
    }

    protected def validateAndGetErrors(jsonNode: JsonNode)(implicit pos: Position): List[String] =
      schema.validate(jsonNode).asScala.map(_.toString).toList.sorted

    override def toString: String =
      s"""${getClass.getSimpleName}(${JsString(defaultJsonSubschemaName)} in $openApiYamlFilename)"""
  }

  /** Validates JSON responses against a schema located at a specific path within an OpenAPI document.
    *
    * This class allows you to validate against any schema in the OpenAPI document by specifying a dot-separated path,
    * rather than being limited to schemas in components/schemas.
    */
  final class OpenApi3PathSchema private[schematestutils] (
    openApiYamlFilename: String,
    /** Dot-separated path to the sub schema within the OpenAPI document. Examples:
      * "paths./example/route/endpoint.post.requestBody.content.application/json.schema"
      */
    subschemaPath: String,
    metaSchemaValidation: Option[ValidatedNel[String, Unit]],
    restrictAdditionalProperties: Boolean
  ) extends DebtTransSchemaValidator {

    private val fullOpenApiNode: JsonNode =
      InternalUtils.readJsonNode(jsonOrYamlPath = openApiYamlFilename)

    /** Constructing this class will automatically verify it against the meta-schema. */
    metaSchemaValidation match {
      case None                           => ()
      case Some(expectedValidationResult) =>
        val actualVersion = fullOpenApiNode.path("openapi").asText("")
        val validator = MetaSchemas.yamlSchemaSchema(version = actualVersion)

        val errors = validator.validateFromPathAndGetErrors(jsonOrYamlPath = openApiYamlFilename)

        withClue(s"Validate $openApiYamlFilename against $validator\n\n") {
          errors shouldBe expectedValidationResult.fold[List[String]](_.toList, { case () => Nil })
        }
    }

    private val schema: Schema = {
      val responseSchemaPath: NodePath = buildNodePathFromString(subschemaPath)

      val mutableRoot: JsonNode = fullOpenApiNode.deepCopy[JsonNode]
      val schemasNode: JsonNode =
        Option(JsonNodes.get[JsonNode](mutableRoot, responseSchemaPath))
          .getOrElse(MissingNode.getInstance())

      validateByPathOpenApiSchemaStructure(schemasNode)

      InternalUtils.rewriteRegexPatterns(schemasNode, openApiYamlFilename)

      if (restrictAdditionalProperties) {
        InternalUtils.injectAdditionalPropertiesFalse(schemasNode)
      }

      val config = InternalUtils.createSchemaRegistryConfig()

      val registry = InternalUtils.createSchemaRegistry(config)

      val documentSchema: Schema = registry.getSchema(mutableRoot)

      documentSchema
    }

    /** Builds a NodePath from a dot-separated path string. Examples:
      *   - "paths./example/route/endpoint.post.requestBody.content.application/json.schema"
      *
      * @param subschemaPath
      *   dot-separated path string
      * @return
      *   NodePath object
      */
    private def buildNodePathFromString(subschemaPath: String): NodePath = {
      val pathSegments = subschemaPath.split("\\.")

      pathSegments.foldLeft(new NodePath(PathType.JSON_POINTER)) { (acc, segment) =>
        acc.append(segment)
      }
    }

    private def validateByPathOpenApiSchemaStructure(openApiSchemasNode: JsonNode): Unit =
      if (openApiSchemasNode.isMissingNode)
        fail(s"No schema paths found in $openApiYamlFilename")

    protected def validateAndGetErrors(jsonNode: JsonNode)(implicit pos: Position): List[String] =
      schema.validate(jsonNode).asScala.map(_.toString).toList.sorted
  }

  /** Assortment of utilities needed to make the validation work. */
  private object InternalUtils {

    def readJsonNode(jsonOrYamlPath: String)(implicit @unused pos: Position): JsonNode = {
      val fileContent = Using(Source.fromFile(jsonOrYamlPath))(_.mkString).get

      jsonOrYamlPath.toLowerCase(Locale.ENGLISH) match {
        case s"$_.json"             => jsonToJsonNode(fileContent)
        case s"$_.yaml" | s"$_.yml" => yamlToJsonNode(fileContent)
        case _                      =>
          fail(s"Cannot read file because it doesn't have a json/yaml/yml file extension: $jsonOrYamlPath")
      }
    }

    def yamlToJsonNode(yaml: String): JsonNode = new ObjectMapper(new YAMLFactory()).readTree(yaml)

    def jsonToJsonNode(json: String): JsonNode = new ObjectMapper().readTree(json)

    /** Recursively rewrites every {{{"$ref": "#/components/schemas/Foo"}}} to {{{"$ref": "#/$defs/Foo"}}} so that the
      * JSON schema is handled properly
      *
      * Fix the regular expression bug in the schema validator library. The regex flavour SHOULD be ECMA-262, but the
      * library uses the Java flavour to validate regexes. The biggest difference is that the YAML is allowed to declare
      * {{{[[]}}}, but Java forbids it without escaping: {{{[\[]}}}
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
    def rewriteRegexPatterns(node: JsonNode, openApiYamlFilename: String): Unit =
      if (node.isObject) {
        val jsonObjectNode = node.asInstanceOf[ObjectNode]

        Option(jsonObjectNode.get("pattern")).foreach { patternNode =>
          val oldPattern = patternNode.asText()
          val newPattern = RegexFlavourTranslator.ecmaScriptRegexFlavourToJavaFlavour(
            esPattern = oldPattern,
            locationContext = s"schema in file $openApiYamlFilename"
          )

          if (newPattern != oldPattern) {
            jsonObjectNode.put("pattern", newPattern)
          }
        }

        jsonObjectNode.properties().asScala.foreach(entry => rewriteRegexPatterns(entry.getValue, openApiYamlFilename))

      } else if (node.isArray) {
        node.elements().asScala.foreach(rewriteRegexPatterns(_, openApiYamlFilename))
      }

    /** Recursively injects {{{"additionalProperties": false}}} into every object schema node that has "properties" but
      * no "additionalProperties". openapi4j had ValidationOptions.ADDITIONAL_PROPS_RESTRICT This effectively does the
      * same thing by changing the schema before validation instead of doing this as part of validation.
      */
    def injectAdditionalPropertiesFalse(node: JsonNode): Unit =
      if (node.isObject) {
        val obj = node.asInstanceOf[ObjectNode]

        val hasProperties = obj.has("properties")
        val hasAdditionalProperties = obj.has("additionalProperties")

        if (hasProperties && !hasAdditionalProperties)
          obj.put("additionalProperties", false)

        obj.properties().asScala.foreach(entry => injectAdditionalPropertiesFalse(entry.getValue))

      } else if (node.isArray) {
        node.elements().asScala.foreach(injectAdditionalPropertiesFalse)
      }

    def createSchemaRegistryConfig(): SchemaRegistryConfig =
      SchemaRegistryConfig
        .builder()
        .regularExpressionFactory(JDKRegularExpressionFactory.getInstance())
        .formatAssertionsEnabled(true)
        .build()

    def createSchemaRegistry(config: SchemaRegistryConfig): SchemaRegistry =
      SchemaRegistry.withDialect(
        Dialects.getOpenApi30,
        builder => {
          builder.schemaRegistryConfig(config)
          ()
        }
      )
  }

}
