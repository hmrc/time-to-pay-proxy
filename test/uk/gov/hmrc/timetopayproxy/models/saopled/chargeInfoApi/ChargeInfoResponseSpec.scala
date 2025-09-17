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

package uk.gov.hmrc.timetopayproxy.models.saopled.chargeInfoApi

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsSuccess, JsValue, Json, Reads, Writes }
import uk.gov.hmrc.timetopayproxy.models.{ IdType, IdValue, Identification }
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps.RichJsValueWithAssertions
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

import java.time.{ LocalDate, LocalDateTime }

class ChargeInfoResponseSpec extends AnyFreeSpec {

  object TestData {
    object WithOnlySomes {
      def obj: ChargeInfoResponse = ChargeInfoResponse(
        processingDateTime = LocalDateTime.parse("2025-07-02T15:00:41.689"),
        identification = List(
          Identification(idType = IdType("ID_TYPE"), idValue = IdValue("ID_VALUE"))
        ),
        individualDetails = IndividualDetails(
          title = Some(Title("Mr")),
          firstName = Some(FirstName("John")),
          lastName = Some(LastName("Doe")),
          dateOfBirth = Some(DateOfBirth(LocalDate.parse("1980-01-01"))),
          districtNumber = Some(DistrictNumber("1234")),
          customerType = CustomerType.ItsaMigtrated,
          transitionToCDCS = TransitionToCdcs(value = true)
        ),
        addresses = List(
          Address(
            addressType = AddressType("Address Type"),
            addressLine1 = AddressLine1("Address Line 1"),
            addressLine2 = Some(AddressLine2("Address Line 2")),
            addressLine3 = Some(AddressLine3("Address Line 3")),
            addressLine4 = Some(AddressLine4("Address Line 4")),
            rls = Some(Rls(true)),
            contactDetails = Some(
              ContactDetails(
                telephoneNumber = Some(TelephoneNumber("telephone-number")),
                fax = Some(Fax("fax-number")),
                mobile = Some(Mobile("mobile-number")),
                emailAddress = Some(Email("email address")),
                emailSource = Some(EmailSource("email source"))
              )
            ),
            postCode = Some(ChargeInfoPostCode("AB12 3CD")),
            postcodeHistory = List(
              PostCodeInfo(
                addressPostcode = ChargeInfoPostCode("AB12 3CD"),
                postcodeDate = LocalDate.parse("2020-01-01")
              )
            )
          )
        ),
        chargeTypeAssessment = List(
          ChargeTypeAssessment(
            debtTotalAmount = BigInt(1000),
            chargeReference = ChargeReference("CHARGE REFERENCE"),
            parentChargeReference = Some(ChargeInfoParentChargeReference("PARENT CHARGE REF")),
            mainTrans = MainTrans("2000"),
            charges = List(
              Charge(
                taxPeriodFrom = TaxPeriodFrom(LocalDate.parse("2020-01-02")),
                taxPeriodTo = TaxPeriodTo(LocalDate.parse("2020-12-31")),
                chargeType = ChargeType("charge type"),
                mainType = MainType("main type"),
                subTrans = SubTrans("1000"),
                outstandingAmount = OutstandingAmount(BigInt(500)),
                dueDate = DueDate(LocalDate.parse("2021-01-31")),
                isInterestBearingCharge = Some(ChargeInfoIsInterestBearingCharge(true)),
                interestStartDate = Some(InterestStartDate(LocalDate.parse("2020-01-03"))),
                accruedInterest = AccruedInterest(BigInt(50)),
                chargeSource = ChargeInfoChargeSource("Source"),
                parentMainTrans = Some(ChargeInfoParentMainTrans("Parent Main Transaction")),
                originalCreationDate = Some(OriginalCreationDate(LocalDate.parse("2025-07-02"))),
                tieBreaker = Some(TieBreaker("Tie Breaker")),
                originalTieBreaker = Some(OriginalTieBreaker("Original Tie Breaker")),
                saTaxYearEnd = Some(SaTaxYearEnd(LocalDate.parse("2020-04-05"))),
                creationDate = Some(CreationDate(LocalDate.parse("2025-07-02"))),
                originalChargeType = Some(OriginalChargeType("Original Charge Type"))
              )
            )
          )
        )
      )

      def json: JsValue = Json.parse(
        """{
          |  "addresses" : [
          |    {
          |      "addressLine1" : "Address Line 1",
          |      "addressLine2" : "Address Line 2",
          |      "addressLine3" : "Address Line 3",
          |      "addressLine4" : "Address Line 4",
          |      "addressType" : "Address Type",
          |      "contactDetails" : {
          |        "emailAddress" : "email address",
          |        "emailSource" : "email source",
          |        "fax" : "fax-number",
          |        "mobile" : "mobile-number",
          |        "telephoneNumber" : "telephone-number"
          |      },
          |      "postCode" : "AB12 3CD",
          |      "postcodeHistory" : [
          |        {
          |          "addressPostcode" : "AB12 3CD",
          |          "postcodeDate" : "2020-01-01"
          |        }
          |      ],
          |      "rls" : true
          |    }
          |  ],
          |  "chargeTypeAssessment" : [
          |    {
          |      "chargeReference" : "CHARGE REFERENCE",
          |      "charges" : [
          |        {
          |          "accruedInterest" : 50,
          |          "chargeSource" : "Source",
          |          "chargeType" : "charge type",
          |          "creationDate" : "2025-07-02",
          |          "dueDate" : "2021-01-31",
          |          "interestStartDate" : "2020-01-03",
          |          "isInterestBearingCharge" : true,
          |          "mainType" : "main type",
          |          "originalChargeType" : "Original Charge Type",
          |          "originalCreationDate" : "2025-07-02",
          |          "originalTieBreaker" : "Original Tie Breaker",
          |          "outstandingAmount" : 500,
          |          "parentMainTrans" : "Parent Main Transaction",
          |          "saTaxYearEnd" : "2020-04-05",
          |          "subTrans" : "1000",
          |          "taxPeriodFrom" : "2020-01-02",
          |          "taxPeriodTo" : "2020-12-31",
          |          "tieBreaker" : "Tie Breaker"
          |        }
          |      ],
          |      "debtTotalAmount" : 1000,
          |      "mainTrans" : "2000",
          |      "parentChargeReference" : "PARENT CHARGE REF"
          |    }
          |  ],
          |  "identification" : [
          |    {
          |      "idType" : "ID_TYPE",
          |      "idValue" : "ID_VALUE"
          |    }
          |  ],
          |  "individualDetails" : {
          |    "customerType" : "MTD(ITSA)",
          |    "dateOfBirth" : "1980-01-01",
          |    "districtNumber" : "1234",
          |    "firstName" : "John",
          |    "lastName" : "Doe",
          |    "title" : "Mr",
          |    "transitionToCDCS" : true
          |  },
          |  "processingDateTime" : "2025-07-02T15:00:41.689"
          |}
          |""".stripMargin
      )
    }

    object With1SomeOnEachPath {
      def obj: ChargeInfoResponse = ChargeInfoResponse(
        processingDateTime = LocalDateTime.parse("2025-07-02T15:00:41.689"),
        identification = List(
          Identification(idType = IdType("ID_TYPE"), idValue = IdValue("ID_VALUE"))
        ),
        individualDetails = IndividualDetails(
          title = None,
          firstName = None,
          lastName = None,
          dateOfBirth = None,
          districtNumber = None,
          customerType = CustomerType.ItsaMigtrated,
          transitionToCDCS = TransitionToCdcs(value = true)
        ),
        addresses = List(
          Address(
            addressType = AddressType("Address Type"),
            addressLine1 = AddressLine1("Address Line 1"),
            addressLine2 = Some(AddressLine2("Address Line 2")),
            addressLine3 = Some(AddressLine3("Address Line 3")),
            addressLine4 = Some(AddressLine4("Address Line 4")),
            rls = Some(Rls(true)),
            contactDetails = Some(
              ContactDetails(
                telephoneNumber = None,
                fax = None,
                mobile = None,
                emailAddress = None,
                emailSource = None
              )
            ),
            postCode = Some(ChargeInfoPostCode("AB12 3CD")),
            postcodeHistory = List(
              PostCodeInfo(
                addressPostcode = ChargeInfoPostCode("AB12 3CD"),
                postcodeDate = LocalDate.parse("2020-01-01")
              )
            )
          )
        ),
        chargeTypeAssessment = List(
          ChargeTypeAssessment(
            debtTotalAmount = BigInt(1000),
            chargeReference = ChargeReference("CHARGE REFERENCE"),
            parentChargeReference = Some(ChargeInfoParentChargeReference("PARENT CHARGE REF")),
            mainTrans = MainTrans("2000"),
            charges = List(
              Charge(
                taxPeriodFrom = TaxPeriodFrom(LocalDate.parse("2020-01-02")),
                taxPeriodTo = TaxPeriodTo(LocalDate.parse("2020-12-31")),
                chargeType = ChargeType("charge type"),
                mainType = MainType("main type"),
                subTrans = SubTrans("1000"),
                outstandingAmount = OutstandingAmount(BigInt(500)),
                dueDate = DueDate(LocalDate.parse("2021-01-31")),
                isInterestBearingCharge = Some(ChargeInfoIsInterestBearingCharge(true)),
                interestStartDate = Some(InterestStartDate(LocalDate.parse("2020-01-03"))),
                accruedInterest = AccruedInterest(BigInt(50)),
                chargeSource = ChargeInfoChargeSource("Source"),
                parentMainTrans = Some(ChargeInfoParentMainTrans("Parent Main Transaction")),
                originalCreationDate = Some(OriginalCreationDate(LocalDate.parse("2025-07-02"))),
                tieBreaker = Some(TieBreaker("Tie Breaker")),
                originalTieBreaker = Some(OriginalTieBreaker("Original Tie Breaker")),
                saTaxYearEnd = Some(SaTaxYearEnd(LocalDate.parse("2020-04-05"))),
                creationDate = Some(CreationDate(LocalDate.parse("2025-07-02"))),
                originalChargeType = Some(OriginalChargeType("Original Charge Type"))
              )
            )
          )
        )
      )

      def json: JsValue = Json.parse(
        """{
          |  "addresses" : [
          |    {
          |      "addressLine1" : "Address Line 1",
          |      "addressLine2" : "Address Line 2",
          |      "addressLine3" : "Address Line 3",
          |      "addressLine4" : "Address Line 4",
          |      "addressType" : "Address Type",
          |      "contactDetails" : { },
          |      "postCode" : "AB12 3CD",
          |      "postcodeHistory" : [
          |        {
          |          "addressPostcode" : "AB12 3CD",
          |          "postcodeDate" : "2020-01-01"
          |        }
          |      ],
          |      "rls" : true
          |    }
          |  ],
          |  "chargeTypeAssessment" : [
          |    {
          |      "chargeReference" : "CHARGE REFERENCE",
          |      "charges" : [
          |        {
          |          "accruedInterest" : 50,
          |          "chargeSource" : "Source",
          |          "chargeType" : "charge type",
          |          "creationDate" : "2025-07-02",
          |          "dueDate" : "2021-01-31",
          |          "interestStartDate" : "2020-01-03",
          |          "isInterestBearingCharge" : true,
          |          "mainType" : "main type",
          |          "originalChargeType" : "Original Charge Type",
          |          "originalCreationDate" : "2025-07-02",
          |          "originalTieBreaker" : "Original Tie Breaker",
          |          "outstandingAmount" : 500,
          |          "parentMainTrans" : "Parent Main Transaction",
          |          "saTaxYearEnd" : "2020-04-05",
          |          "subTrans" : "1000",
          |          "taxPeriodFrom" : "2020-01-02",
          |          "taxPeriodTo" : "2020-12-31",
          |          "tieBreaker" : "Tie Breaker"
          |        }
          |      ],
          |      "debtTotalAmount" : 1000,
          |      "mainTrans" : "2000",
          |      "parentChargeReference" : "PARENT CHARGE REF"
          |    }
          |  ],
          |  "identification" : [
          |    {
          |      "idType" : "ID_TYPE",
          |      "idValue" : "ID_VALUE"
          |    }
          |  ],
          |  "individualDetails" : {
          |    "customerType" : "MTD(ITSA)",
          |    "transitionToCDCS" : true
          |  },
          |  "processingDateTime" : "2025-07-02T15:00:41.689"
          |}
          |""".stripMargin
      )
    }

    object With0SomeOnEachPath {
      def obj: ChargeInfoResponse = ChargeInfoResponse(
        processingDateTime = LocalDateTime.parse("2025-07-02T15:00:41.689"),
        identification = List(
          Identification(idType = IdType("ID_TYPE"), idValue = IdValue("ID_VALUE"))
        ),
        individualDetails = IndividualDetails(
          title = None,
          firstName = None,
          lastName = None,
          dateOfBirth = None,
          districtNumber = None,
          customerType = CustomerType.ItsaMigtrated,
          transitionToCDCS = TransitionToCdcs(value = true)
        ),
        addresses = List(
          Address(
            addressType = AddressType("Address Type"),
            addressLine1 = AddressLine1("Address Line 1"),
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            rls = None,
            contactDetails = None,
            postCode = None,
            postcodeHistory = List(
              PostCodeInfo(
                addressPostcode = ChargeInfoPostCode("AB12 3CD"),
                postcodeDate = LocalDate.parse("2020-01-01")
              )
            )
          )
        ),
        chargeTypeAssessment = List(
          ChargeTypeAssessment(
            debtTotalAmount = BigInt(1000),
            chargeReference = ChargeReference("CHARGE REFERENCE"),
            parentChargeReference = None,
            mainTrans = MainTrans("2000"),
            charges = List(
              Charge(
                taxPeriodFrom = TaxPeriodFrom(LocalDate.parse("2020-01-02")),
                taxPeriodTo = TaxPeriodTo(LocalDate.parse("2020-12-31")),
                chargeType = ChargeType("charge type"),
                mainType = MainType("main type"),
                subTrans = SubTrans("1000"),
                outstandingAmount = OutstandingAmount(BigInt(500)),
                dueDate = DueDate(LocalDate.parse("2021-01-31")),
                isInterestBearingCharge = None,
                interestStartDate = None,
                accruedInterest = AccruedInterest(BigInt(50)),
                chargeSource = ChargeInfoChargeSource("Source"),
                parentMainTrans = None,
                originalCreationDate = None,
                tieBreaker = None,
                originalTieBreaker = None,
                saTaxYearEnd = None,
                creationDate = None,
                originalChargeType = None
              )
            )
          )
        )
      )

      def json: JsValue = Json.parse(
        """{
          |  "addresses" : [
          |    {
          |      "addressLine1" : "Address Line 1",
          |      "addressType" : "Address Type",
          |      "postcodeHistory" : [
          |        {
          |          "addressPostcode" : "AB12 3CD",
          |          "postcodeDate" : "2020-01-01"
          |        }
          |      ]
          |    }
          |  ],
          |  "chargeTypeAssessment" : [
          |    {
          |      "chargeReference" : "CHARGE REFERENCE",
          |      "charges" : [
          |        {
          |          "accruedInterest" : 50,
          |          "chargeSource" : "Source",
          |          "chargeType" : "charge type",
          |          "dueDate" : "2021-01-31",
          |          "mainType" : "main type",
          |          "outstandingAmount" : 500,
          |          "subTrans" : "1000",
          |          "taxPeriodFrom" : "2020-01-02",
          |          "taxPeriodTo" : "2020-12-31"
          |        }
          |      ],
          |      "debtTotalAmount" : 1000,
          |      "mainTrans" : "2000"
          |    }
          |  ],
          |  "identification" : [
          |    {
          |      "idType" : "ID_TYPE",
          |      "idValue" : "ID_VALUE"
          |    }
          |  ],
          |  "individualDetails" : {
          |    "customerType" : "MTD(ITSA)",
          |    "transitionToCDCS" : true
          |  },
          |  "processingDateTime" : "2025-07-02T15:00:41.689"
          |}
          |""".stripMargin
      )
    }
  }

  "ChargeInfoResponse" - {

    "implicit JSON writer (data going to our clients)" - {
      def writerToClients: Writes[ChargeInfoResponse] = implicitly[Writes[ChargeInfoResponse]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: ChargeInfoResponse = TestData.WithOnlySomes.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.ChargeInfo.openApiResponseSuccessfulSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }

      "when only one optional field on each path is populated" - {
        def json: JsValue = TestData.With1SomeOnEachPath.json
        def obj: ChargeInfoResponse = TestData.With1SomeOnEachPath.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.ChargeInfo.openApiResponseSuccessfulSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: ChargeInfoResponse = TestData.With0SomeOnEachPath.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.ChargeInfo.openApiResponseSuccessfulSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }
    }

    "implicit JSON reader (data coming from time-to-pay-eligibility)" - {
      def readerFromTtp: Reads[ChargeInfoResponse] = implicitly[Reads[ChargeInfoResponse]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: ChargeInfoResponse = TestData.WithOnlySomes.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay-eligibility schema" in {
          val schema = Validators.TimeToPayEligibility.ChargeInfo.openApiResponseSuccessfulSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }

      "when only one optional field on each path is populated" - {
        def json: JsValue = TestData.With1SomeOnEachPath.json
        def obj: ChargeInfoResponse = TestData.With1SomeOnEachPath.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay-eligibility schema" in {
          val schema = Validators.TimeToPayEligibility.ChargeInfo.openApiResponseSuccessfulSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: ChargeInfoResponse = TestData.With0SomeOnEachPath.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay-eligibility schema" in {
          val schema = Validators.TimeToPayEligibility.ChargeInfo.openApiResponseSuccessfulSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }
    }
  }
}
