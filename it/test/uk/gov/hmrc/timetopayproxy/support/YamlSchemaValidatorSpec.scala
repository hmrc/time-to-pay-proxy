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
import com.softwaremill.quicklens.{ ModifyPimp, QuicklensEach }
import org.openapi4j.schema.validator.v3.SchemaValidator
import org.openapi4j.schema.validator.{ ValidationContext, ValidationData }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.affordablequotes.{ AffordableQuoteInstalment, AffordableQuotePaymentPlan, AffordableQuoteResponse, AffordableQuotesRequest }
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps.{ RichJsValueWithAssertions, RichJsValueWithMergingOperations }

import java.nio.file.Paths
import java.time.{ LocalDate, LocalDateTime }
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{ Failure, Success, Try }

class YamlSchemaValidatorSpec extends AnyWordSpec with Matchers {

  "application.yaml" should {
    "be valid according to the meta schema" in new TestBase {
      val schema: JsonSchema =
        loadYamlAndConvertToJsonSchema("resources/public/api/conf/1.0/yamlSchemas/metaSchema.yaml")
      val json: JsonNode = loadYamlAndConvertToJsonNode("resources/public/api/conf/1.0/application.yaml")
      val errors: Set[ValidationMessage] = schema.validate(json).asScala.toSet

      errors shouldEqual Set.empty
    }
  }

  "AffordableQuotes request" should {
    val affordableQuotesRequestOnlyMandatoryFields: AffordableQuotesRequest =
      AffordableQuotesRequest(
        channelIdentifier = "eSSTTP",
        paymentPlanAffordableAmount = 1310,
        paymentPlanFrequency = Frequency.Single,
        paymentPlanMinLength = Duration(1),
        paymentPlanMaxLength = Duration(6),
        accruedDebtInterest = 13.26,
        paymentPlanStartDate = LocalDate.parse("2022-07-08"),
        initialPaymentDate = None,
        initialPaymentAmount = None,
        debtItemCharges = List(
          DebtItemChargeSelfServe(
            outstandingDebtAmount = BigDecimal(1487.81),
            mainTrans = "5330",
            subTrans = "1090",
            debtItemChargeId = DebtItemChargeId("XW006559808862"),
            interestStartDate = None,
            debtItemOriginalDueDate = LocalDate.parse("2022-05-22"),
            isInterestBearingCharge = None,
            useChargeReference = None
          )
        ),
        customerPostcodes = List(CustomerPostCode(PostCode("BN127ER"), postcodeDate = LocalDate.parse("2022-05-22")))
      )

    val affordableQuotesRequestWithAllFields: AffordableQuotesRequest =
      affordableQuotesRequestOnlyMandatoryFields.copy(
        initialPaymentDate = Some(LocalDate.parse("2022-06-18")),
        initialPaymentAmount = Some(1000),
        debtItemCharges = List(
          affordableQuotesRequestOnlyMandatoryFields.debtItemCharges.head.copy(
            interestStartDate = Some(LocalDate.parse("2022-05-22")),
            isInterestBearingCharge = Some(IsInterestBearingCharge(true)),
            useChargeReference = Some(UseChargeReference(false))
          )
        )
      )

    def jsonBodyContainingOnlyMandatoryFields: JsValue =
      Json.parse("""{
                   |  "channelIdentifier": "eSSTTP",
                   |  "paymentPlanAffordableAmount": 1310,
                   |  "paymentPlanFrequency": "single",
                   |  "paymentPlanMaxLength": 6,
                   |  "paymentPlanMinLength": 1,
                   |  "accruedDebtInterest": 13.26,
                   |  "paymentPlanStartDate": "2022-07-08",
                   |  "debtItemCharges": [
                   |    {
                   |      "outstandingDebtAmount": 1487.81,
                   |      "mainTrans": "5330",
                   |      "subTrans": "1090",
                   |      "debtItemChargeId": "XW006559808862",
                   |      "debtItemOriginalDueDate": "2022-05-22"
                   |    }
                   |   ],
                   |  "customerPostcodes": [
                   |    {
                   |      "addressPostcode": "BN127ER",
                   |      "postcodeDate": "2022-05-22"
                   |    }
                   |  ]
                   |}""".stripMargin)

    def jsonBodyContainingOnlyOptionalFields: JsValue =
      Json.parse("""{
                   |  "initialPaymentDate": "2022-06-18",
                   |  "initialPaymentAmount": 1000,
                   |  "debtItemCharges": [
                   |    {
                   |      "interestStartDate": "2022-05-22",
                   |      "isInterestBearingCharge": true,
                   |      "useChargeReference": false
                   |    }
                   |  ]
                   |  }""".stripMargin)

    def jsonBodyContainingABrokenField: JsValue =
      Json.parse("""{
                   |"initialPaymentDate": "Not A Date"
                   |}""".stripMargin)

    "be valid according to application.yaml using openAPI" when {
      "given only mandatory fields" in new TestBase {
        val requestExample: String =
          jsonBodyContainingOnlyMandatoryFields.toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesRequestSchema, requestExample)

        errors shouldEqual Map.empty
      }

      "given all fields" in new TestBase {
        val requestExample: String =
          jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(jsonBodyContainingOnlyOptionalFields).toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesRequestSchema, requestExample)

        errors shouldEqual Map.empty
      }
    }

    "successfully read and write to the model" when {
      "given only mandatory fields" in new TestBase {
        AffordableQuotesRequest.format.reads(jsonBodyContainingOnlyMandatoryFields) shouldBe JsSuccess(
          affordableQuotesRequestOnlyMandatoryFields
        )

        AffordableQuotesRequest.format.writes(
          affordableQuotesRequestOnlyMandatoryFields
        ) shouldBeEquivalentTo jsonBodyContainingOnlyMandatoryFields
      }

      "given all fields" in new TestBase {
        AffordableQuotesRequest.format.reads(
          jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(jsonBodyContainingOnlyOptionalFields)
        ) shouldBe JsSuccess(
          affordableQuotesRequestWithAllFields
        )

        AffordableQuotesRequest.format.writes(
          affordableQuotesRequestWithAllFields
        ) shouldBeEquivalentTo jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(
          jsonBodyContainingOnlyOptionalFields
        )
      }
    }

    "return errors when validating against the application.yaml file" when {
      "given an empty json object" in new TestBase {
        val requestExample: String = JsObject.empty.toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesRequestSchema, requestExample)

        errors should not be Map.empty
      }

      "missing mandatory fields" in new TestBase {
        val requestExample: String =
          jsonBodyContainingOnlyOptionalFields.toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesRequestSchema, requestExample)

        errors.keys should contain("validationError")
        errors("validationError") should contain(
          "Field 'channelIdentifier' is required. (code: 1026)\nFrom: <required>"
        )
      }

      "a field is broken" in new TestBase {
        val requestExample: String =
          jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(jsonBodyContainingABrokenField).toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesRequestSchema, requestExample)

        errors.keys should contain("validationError")
        errors("validationError") should contain(
          "initialPaymentDate: Value 'Not A Date' does not match format 'date'. (code: 1007)\nFrom: initialPaymentDate.<format>"
        )
      }
    }

    "return json validation errors when reading from json" when {
      "given an empty json object" in new TestBase {
        AffordableQuotesRequest.format.reads(JsObject.empty) shouldBe a[JsError]
      }

      "missing mandatory fields" in new TestBase {
        AffordableQuotesRequest.format.reads(jsonBodyContainingOnlyOptionalFields) match {
          case JsSuccess(_, _) => fail("Should not have parsed json with missing fields")
          case JsError(errors) =>
            errors should contain(
              (JsPath \ "debtItemCharges")(0) \ "mainTrans",
              List(JsonValidationError("error.path.missing"))
            )
        }
      }

      "a field is broken" in new TestBase {
        AffordableQuotesRequest.format.reads(
          jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(jsonBodyContainingABrokenField)
        ) match {
          case JsSuccess(_, _) => fail("Should not have parsed json with missing fields")
          case JsError(errors) => errors.map(_._1) should contain(JsPath \ "initialPaymentDate")
        }
      }
    }
  }

  "AffordableQuotes response" should {
    val affordableQuotesResponseOnlyMandatoryFields: AffordableQuoteResponse =
      AffordableQuoteResponse(
        LocalDateTime.parse("2022-03-23T13:49:51.141"),
        List(
          AffordableQuotePaymentPlan(
            numberOfInstalments = 8,
            planDuration = Duration(4),
            planInterest = 48.05,
            totalDebt = 7451,
            totalDebtIncInt = 7512.88,
            collections = Collections(
              initialCollection = None,
              List(RegularCollection(dueDate = LocalDate.parse("2022-07-08"), amountDue = 1628.21))
            ),
            instalments = List(
              AffordableQuoteInstalment(
                DebtItemChargeId("XW006559808862"),
                dueDate = LocalDate.parse("2022-06-18"),
                amountDue = 1000,
                instalmentNumber = 1,
                instalmentInterestAccrued = 4.59,
                instalmentBalance = 4808.96,
                debtItemOriginalDueDate = LocalDate.parse("2022-05-22")
              )
            )
          )
        )
      )

    val affordableQuotesResponseWithAllFields: AffordableQuoteResponse =
      affordableQuotesResponseOnlyMandatoryFields
        .modify(_.paymentPlans.each.collections)
        .setTo(
          Collections(
            Some(InitialCollection(dueDate = LocalDate.parse("2022-06-18"), amountDue = 1000.45)),
            affordableQuotesResponseOnlyMandatoryFields.paymentPlans.flatMap(_.collections.regularCollections)
          )
        )

    def jsonBodyContainingOnlyMandatoryFields: JsValue =
      Json.parse("""{
                   |  "processingDateTime": "2022-03-23T13:49:51.141",
                   |  "paymentPlans": [
                   |    {
                   |      "numberOfInstalments": 8,
                   |      "planDuration": 4,
                   |      "totalDebt": 7451,
                   |      "totalDebtIncInt": 7512.88,
                   |      "planInterest": 48.05,
                   |      "collections": {
                   |        "regularCollections": [
                   |          {
                   |            "dueDate": "2022-07-08",
                   |            "amountDue": 1628.21
                   |          }
                   |        ]
                   |      },
                   |      "instalments": [
                   |        {
                   |          "instalmentNumber": 1,
                   |          "dueDate": "2022-06-18",
                   |          "instalmentInterestAccrued": 4.59,
                   |          "instalmentBalance": 4808.96,
                   |          "debtItemChargeId": "XW006559808862",
                   |          "amountDue": 1000,
                   |          "debtItemOriginalDueDate": "2022-05-22"
                   |        }
                   |      ]
                   |    }
                   |  ]
                   |}""".stripMargin)

    def jsonBodyContainingOnlyOptionalFields: JsValue =
      Json.parse("""{
                   |  "paymentPlans": [
                   |    {
                   |      "collections": {
                   |        "initialCollection": {
                   |          "dueDate": "2022-06-18",
                   |          "amountDue": 1000.45
                   |        }
                   |      }
                   |    }
                   |  ]
                   |}""".stripMargin)

    def jsonBodyContainingABrokenField: JsValue =
      Json.parse("""{
                   |  "paymentPlans": [
                   |    {
                   |      "collections": {
                   |        "initialCollection": {
                   |          "dueDate": "Not A Date",
                   |          "amountDue": 1000
                   |        }
                   |      }
                   |    }
                   |  ]
                   |}""".stripMargin)

    "be valid according to application.yaml using openAPI" when {
      "given only mandatory fields" in new TestBase {
        val responseExample: String =
          jsonBodyContainingOnlyMandatoryFields.toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesResponseSchema, responseExample)

        errors shouldEqual Map.empty
      }

      "given all fields" in new TestBase {
        val responseExample: String =
          jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(jsonBodyContainingOnlyOptionalFields).toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesResponseSchema, responseExample)

        errors shouldEqual Map.empty
      }
    }

    "successfully read and write to the model" when {
      "given only mandatory fields" in new TestBase {
        AffordableQuoteResponse.format.reads(jsonBodyContainingOnlyMandatoryFields) shouldBe JsSuccess(
          affordableQuotesResponseOnlyMandatoryFields
        )

        AffordableQuoteResponse.format.writes(
          affordableQuotesResponseOnlyMandatoryFields
        ) shouldBeEquivalentTo jsonBodyContainingOnlyMandatoryFields
      }

      "given all fields" in new TestBase {
        AffordableQuoteResponse.format.reads(
          jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(jsonBodyContainingOnlyOptionalFields)
        ) shouldBe JsSuccess(
          affordableQuotesResponseWithAllFields
        )

        AffordableQuoteResponse.format.writes(
          affordableQuotesResponseWithAllFields
        ) shouldBeEquivalentTo jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(
          jsonBodyContainingOnlyOptionalFields
        )
      }
    }

    "return errors when validating against the application.yaml file" when {
      "given an empty json object" in new TestBase {
        val responseExample: String = JsObject.empty.toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesResponseSchema, responseExample)

        errors should not be Map.empty
      }

      "missing mandatory fields" in new TestBase {
        val responseExample: String =
          jsonBodyContainingOnlyOptionalFields.toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesResponseSchema, responseExample)

        errors.keys should contain("validationError")
        errors("validationError") should contain(
          "paymentPlans.0: Field 'planDuration' is required. (code: 1026)\nFrom: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.<required>"
        )
      }

      "a field is broken" in new TestBase {
        val responseExample: String =
          jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(jsonBodyContainingABrokenField).toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesResponseSchema, responseExample)

        println(Json.prettyPrint(Json.parse(responseExample)))
        errors.keys should contain("validationError")
        errors("validationError") should contain(
          "paymentPlans.0.collections.initialCollection.dueDate: Value 'Not A Date' does not match format 'date'. (code: 1007)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.collections.initialCollection.<#/components/schemas/InitialCollection>.dueDate.<format>"
        )
      }
    }

    "return json validation errors when reading from json" when {
      "given an empty json object" in new TestBase {
        AffordableQuoteResponse.format.reads(JsObject.empty) shouldBe a[JsError]
      }

      "missing mandatory fields" in new TestBase {
        AffordableQuoteResponse.format.reads(jsonBodyContainingOnlyOptionalFields) match {
          case JsSuccess(_, _) => fail("Should not have parsed json with missing fields")
          case JsError(errors) =>
            errors should contain(
              (JsPath \ "paymentPlans")(0) \ "numberOfInstalments",
              List(JsonValidationError("error.path.missing"))
            )
        }
      }

      "a field is broken" in new TestBase {
        AffordableQuoteResponse.format.reads(
          jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(jsonBodyContainingABrokenField)
        ) match {
          case JsSuccess(_, _) => fail("Should not have parsed json with missing fields")
          case JsError(errors) =>
            errors.map(_._1) should contain(
              (JsPath \ "paymentPlans")(0) \ "collections" \ "initialCollection" \ "dueDate"
            )
        }
      }
    }
  }
}

trait TestBase {
  def validate(api: ApiSchema, body: String): Map[String, List[String]] = {
    val output = new ValidationData
    val json = new ObjectMapper().readTree(body)

    Try {
      new SchemaValidator(new ValidationContext(api.fullSchema.getContext), null, api.defaultSchema)
        .validate(json, output)
    } match {
      case Failure(e) =>
        Map("Exception found" -> List(e.getMessage))
      case Success(_) =>
        output
          .results()
          .items()
          .asScala
          .toList
          .map(e => "validationError" -> e.toString)
          .groupBy(_._1)
          .view
          .mapValues(_.map(_._2).toList)
          .toMap
    }
  }

  lazy val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
  lazy val objectMapper = new ObjectMapper

  def loadYamlAndConvertToJsonSchema(path: String): JsonSchema =
    schemaFactory.getSchema(loadYamlAndConvertToJsonNode(path))

  def loadYamlAndConvertToJsonNode(path: String): JsonNode = {
    val yamlReader = new ObjectMapper(new YAMLFactory()).registerModule(DefaultScalaModule)
    yamlReader.readTree(Paths.get(path).toFile)
  }
}
