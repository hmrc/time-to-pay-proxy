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
        paymentPlanFrequency = FrequencyCapitalised.Single,
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
            isInterestBearingCharge = IsInterestBearingCharge(true),
            useChargeReference = UseChargeReference(false)
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
            isInterestBearingCharge = IsInterestBearingCharge(true),
            useChargeReference = UseChargeReference(false)
          )
        )
      )

    def jsonBodyContainingOnlyMandatoryFields: JsValue =
      Json.parse("""{
                   |  "channelIdentifier": "eSSTTP",
                   |  "paymentPlanAffordableAmount": 1310,
                   |  "paymentPlanFrequency": "Single",
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
                   |      "debtItemOriginalDueDate": "2022-05-22",
                   |      "isInterestBearingCharge": true,
                   |      "useChargeReference": false
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
                   |      "interestStartDate": "2022-05-22"
                   |    }
                   |  ]
                   |  }""".stripMargin)

    def jsonBodyContainingABrokenField: JsValue =
      Json.parse("""{
                   |"initialPaymentDate": "Not A Date"
                   |}""".stripMargin)

    def jsonBodyContainingManyBrokenFields: JsValue =
      Json.parse("""{
                   |  "channelIdentifier": true,
                   |  "paymentPlanAffordableAmount": "1310",
                   |  "paymentPlanFrequency": "monthly",
                   |  "paymentPlanMaxLength": 6.5,
                   |  "paymentPlanMinLength": 1.3,
                   |  "accruedDebtInterest": "13.26",
                   |  "paymentPlanStartDate": "2022--08",
                   |  "debtItemCharges": [
                   |    {
                   |      "outstandingDebtAmount": "1487.81",
                   |      "mainTrans": 5330,
                   |      "subTrans": 1090,
                   |      "debtItemChargeId": true,
                   |      "debtItemOriginalDueDate": "2022-05-",
                   |      "isInterestBearingCharge": "true",
                   |      "useChargeReference": "false"
                   |    }
                   |   ],
                   |  "customerPostcodes": [
                   |    {
                   |      "addressPostcode": 123,
                   |      "postcodeDate": "-05-22"
                   |    }
                   |  ]
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

        errors.keys.toList shouldBe List("validationError")
        errors("validationError") shouldBe List(
          "debtItemCharges.0: Field 'debtItemChargeId' is required. (code: 1026)\nFrom: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.<required>",
          "debtItemCharges.0: Field 'debtItemOriginalDueDate' is required. (code: 1026)\nFrom: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.<required>",
          "debtItemCharges.0: Field 'mainTrans' is required. (code: 1026)\nFrom: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.<required>",
          "debtItemCharges.0: Field 'outstandingDebtAmount' is required. (code: 1026)\nFrom: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.<required>",
          "debtItemCharges.0: Field 'subTrans' is required. (code: 1026)\nFrom: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.<required>",
          "debtItemCharges.0: Field 'isInterestBearingCharge' is required. (code: 1026)\nFrom: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.<required>",
          "debtItemCharges.0: Field 'useChargeReference' is required. (code: 1026)\nFrom: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.<required>",
          "Field 'channelIdentifier' is required. (code: 1026)\nFrom: <required>",
          "Field 'paymentPlanAffordableAmount' is required. (code: 1026)\nFrom: <required>",
          "Field 'paymentPlanFrequency' is required. (code: 1026)\nFrom: <required>",
          "Field 'paymentPlanMaxLength' is required. (code: 1026)\nFrom: <required>",
          "Field 'paymentPlanMinLength' is required. (code: 1026)\nFrom: <required>",
          "Field 'accruedDebtInterest' is required. (code: 1026)\nFrom: <required>",
          "Field 'paymentPlanStartDate' is required. (code: 1026)\nFrom: <required>",
          "Field 'customerPostcodes' is required. (code: 1026)\nFrom: <required>"
        )
      }

      "a field is broken" in new TestBase {
        val requestExample: String =
          jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(jsonBodyContainingABrokenField).toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesRequestSchema, requestExample)

        errors.keys.toList shouldBe List("validationError")
        errors("validationError") shouldBe List(
          "initialPaymentDate: Value 'Not A Date' does not match format 'date'. (code: 1007)\nFrom: initialPaymentDate.<format>"
        )
      }

      "many fields are broken" in new TestBase {
        val responseExample: String =
          jsonBodyContainingManyBrokenFields.toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesRequestSchema, responseExample)

        errors.keys.toList shouldBe List("validationError")
        errors("validationError") shouldBe List(
          "customerPostcodes.0.postcodeDate: '-05-22' does not respect pattern '^\\d{4}-[0-1][0-9]-[0-3][0-9]$'. (code: 1025)\n" +
            "From: customerPostcodes.0.<items>.<#/components/schemas/postCode>.postcodeDate.<pattern>",
          "customerPostcodes.0.addressPostcode: Type expected 'string', found 'integer'. (code: 1027)\n" +
            "From: customerPostcodes.0.<items>.<#/components/schemas/postCode>.addressPostcode.<type>",
          "paymentPlanMaxLength: Value '6.5' does not match format 'int32'. (code: 1007)\n" +
            "From: paymentPlanMaxLength.<format>",
          "paymentPlanMaxLength: Type expected 'integer', found 'number'. (code: 1027)\n" +
            "From: paymentPlanMaxLength.<type>",
          "paymentPlanStartDate: Value '2022--08' does not match format 'date'. (code: 1007)\n" +
            "From: paymentPlanStartDate.<format>",
          "channelIdentifier: Type expected 'string', found 'boolean'. (code: 1027)\n" +
            "From: channelIdentifier.<type>",
          "debtItemCharges.0.mainTrans: Type expected 'string', found 'integer'. (code: 1027)\n" +
            "From: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.mainTrans.<type>",
          "debtItemCharges.0.debtItemOriginalDueDate: Value '2022-05-' does not match format 'date'. (code: 1007)\n" +
            "From: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.debtItemOriginalDueDate.<format>",
          "debtItemCharges.0.subTrans: Type expected 'string', found 'integer'. (code: 1027)\n" +
            "From: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.subTrans.<type>",
          "debtItemCharges.0.outstandingDebtAmount: Type expected 'number', found 'string'. (code: 1027)\n" +
            "From: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.outstandingDebtAmount.<type>",
          "debtItemCharges.0.useChargeReference: Type expected 'boolean', found 'string'. (code: 1027)\n" +
            "From: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.useChargeReference.<type>",
          "debtItemCharges.0.debtItemChargeId: Type expected 'string', found 'boolean'. (code: 1027)\n" +
            "From: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.debtItemChargeId.<type>",
          "debtItemCharges.0.isInterestBearingCharge: Type expected 'boolean', found 'string'. (code: 1027)\n" +
            "From: debtItemCharges.0.<items>.<#/components/schemas/DebtItem>.isInterestBearingCharge.<type>",
          "accruedDebtInterest: Type expected 'number', found 'string'. (code: 1027)\n" +
            "From: accruedDebtInterest.<type>",
          "paymentPlanFrequency: Value 'monthly' is not defined in the schema. (code: 1006)\n" +
            "From: paymentPlanFrequency.<#/components/schemas/paymentPlanFrequency>.<enum>",
          "paymentPlanMinLength: Value '1.3' does not match format 'int32'. (code: 1007)\n" +
            "From: paymentPlanMinLength.<format>",
          "paymentPlanMinLength: Type expected 'integer', found 'number'. (code: 1027)\n" +
            "From: paymentPlanMinLength.<type>",
          "paymentPlanAffordableAmount: Value '1310' does not match format 'int32'. (code: 1007)\n" +
            "From: paymentPlanAffordableAmount.<format>",
          "paymentPlanAffordableAmount: Type expected 'integer', found 'string'. (code: 1027)\n" +
            "From: paymentPlanAffordableAmount.<type>"
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
              ((JsPath \ "debtItemCharges")(0) \ "mainTrans", List(JsonValidationError("error.path.missing")))
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
                debtItemOriginalDueDate = LocalDate.parse("2022-05-22"),
                expectedPayment = 1000
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
                   |          "debtItemOriginalDueDate": "2022-05-22",
                   |          "expectedPayment": 1000
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

    def jsonBodyContainingManyBrokenFields: JsValue =
      Json.parse("""{
                   |  "processingDateTime": "2022-03-23T13:49:51.141",
                   |  "paymentPlans": [
                   |    {
                   |      "numberOfInstalments": "foo",
                   |      "planDuration": true,
                   |      "totalDebt": false,
                   |      "totalDebtIncInt": null,
                   |      "planInterest": "48.05",
                   |      "collections": {
                   |        "regularCollections": [
                   |          {
                   |            "dueDate": "Some date",
                   |            "amountDue": "1628.21"
                   |          }
                   |        ]
                   |      },
                   |      "instalments": [
                   |        {
                   |          "instalmentNumber": 1.34,
                   |          "dueDate": "Another date",
                   |          "instalmentInterestAccrued": "4.59",
                   |          "instalmentBalance": "4808.96",
                   |          "debtItemChargeId": 654,
                   |          "amountDue": "1000",
                   |          "debtItemOriginalDueDate": "not a date",
                   |          "expectedPayment": "1000"
                   |        }
                   |      ]
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

        errors.keys.toList shouldBe List("validationError")
        errors("validationError") shouldBe
          List(
            "paymentPlans.0: Field 'planDuration' is required. (code: 1026)\nFrom: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.<required>",
            "paymentPlans.0: Field 'totalDebt' is required. (code: 1026)\nFrom: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.<required>",
            "paymentPlans.0: Field 'totalDebtIncInt' is required. (code: 1026)\nFrom: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.<required>",
            "paymentPlans.0: Field 'planInterest' is required. (code: 1026)\nFrom: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.<required>",
            "paymentPlans.0: Field 'numberOfInstalments' is required. (code: 1026)\nFrom: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.<required>",
            "paymentPlans.0: Field 'instalments' is required. (code: 1026)\nFrom: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.<required>",
            "paymentPlans.0.collections: Field 'regularCollections' is required. (code: 1026)\nFrom: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.collections.<required>",
            "Field 'processingDateTime' is required. (code: 1026)\nFrom: <required>"
          )
      }

      "a field is broken" in new TestBase {
        val responseExample: String =
          jsonBodyContainingOnlyMandatoryFields.strictDeepMerge(jsonBodyContainingABrokenField).toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesResponseSchema, responseExample)

        errors.keys.toList shouldBe List("validationError")
        errors("validationError") shouldBe List(
          "paymentPlans.0.collections.initialCollection.dueDate: Value 'Not A Date' does not match format 'date'. (code: 1007)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.collections.initialCollection.<#/components/schemas/InitialCollection>.dueDate.<format>"
        )
      }

      "many fields are broken" in new TestBase {
        val responseExample: String =
          jsonBodyContainingManyBrokenFields.toString()
        val errors: Map[String, List[String]] = validate(AffordableQuotesResponseSchema, responseExample)

        errors.keys.toList shouldBe List("validationError")
        errors("validationError") shouldBe List(
          "paymentPlans.0.planDuration: Value 'true' does not match format 'int32'. (code: 1007)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.planDuration.<format>",
          "paymentPlans.0.planDuration: Type expected 'integer', found 'boolean'. (code: 1027)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.planDuration.<type>",
          "paymentPlans.0.collections.regularCollections.0.amountDue: Type expected 'number', found 'string'. (code: 1027)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.collections.regularCollections.0.<items>.<#/components/schemas/RegularCollection>.amountDue.<type>",
          "paymentPlans.0.collections.regularCollections.0.dueDate: Value 'Some date' does not match format 'date'. (code: 1007)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.collections.regularCollections.0.<items>.<#/components/schemas/RegularCollection>.dueDate.<format>",
          "paymentPlans.0.totalDebt: Type expected 'number', found 'boolean'. (code: 1027)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.totalDebt.<type>",
          "paymentPlans.0.planInterest: Type expected 'number', found 'string'. (code: 1027)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.planInterest.<type>",
          "paymentPlans.0.instalments.0.amountDue: Type expected 'number', found 'string'. (code: 1027)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.instalments.0.<items>.<#/components/schemas/instalments>.amountDue.<type>",
          "paymentPlans.0.instalments.0.instalmentInterestAccrued: Type expected 'number', found 'string'. (code: 1027)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.instalments.0.<items>.<#/components/schemas/instalments>.instalmentInterestAccrued.<type>",
          "paymentPlans.0.instalments.0.debtItemOriginalDueDate: Value 'not a date' does not match format 'date'. (code: 1007)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.instalments.0.<items>.<#/components/schemas/instalments>.debtItemOriginalDueDate.<format>",
          "paymentPlans.0.instalments.0.instalmentNumber: Value '1.34' does not match format 'int32'. (code: 1007)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.instalments.0.<items>.<#/components/schemas/instalments>.instalmentNumber.<format>",
          "paymentPlans.0.instalments.0.instalmentNumber: Type expected 'integer', found 'number'. (code: 1027)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.instalments.0.<items>.<#/components/schemas/instalments>.instalmentNumber.<type>",
          "paymentPlans.0.instalments.0.instalmentBalance: Type expected 'number', found 'string'. (code: 1027)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.instalments.0.<items>.<#/components/schemas/instalments>.instalmentBalance.<type>",
          "paymentPlans.0.instalments.0.dueDate: Value 'Another date' does not match format 'date'. (code: 1007)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.instalments.0.<items>.<#/components/schemas/instalments>.dueDate.<format>",
          "paymentPlans.0.instalments.0.expectedPayment: Type expected 'integer', found 'string'. (code: 1027)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.instalments.0.<items>.<#/components/schemas/instalments>.expectedPayment.<type>",
          "paymentPlans.0.instalments.0.debtItemChargeId: Type expected 'string', found 'integer'. (code: 1027)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.instalments.0.<items>.<#/components/schemas/instalments>.debtItemChargeId.<type>",
          "paymentPlans.0.totalDebtIncInt: Null value is not allowed. (code: 1021)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.totalDebtIncInt.<nullable>",
          "paymentPlans.0.numberOfInstalments: Value 'foo' does not match format 'int32'. (code: 1007)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.numberOfInstalments.<format>",
          "paymentPlans.0.numberOfInstalments: Type expected 'integer', found 'string'. (code: 1027)\n" +
            "From: paymentPlans.0.<items>.<#/components/schemas/paymentPlans>.numberOfInstalments.<type>"
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
              ((JsPath \ "paymentPlans")(0) \ "numberOfInstalments", List(JsonValidationError("error.path.missing")))
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
