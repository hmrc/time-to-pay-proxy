/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi

import play.api.libs.json.{ JsObject, Json }
import uk.gov.hmrc.timetopayproxy.models.{ ChargeTypesExcluded, IdType, IdValue, Identification }

import java.time.{ LocalDate, LocalDateTime }

object ChargeInfoTestData {
  object TestData {
    object WithOnlySomes {
      def chargeInfoResponseR1: ChargeInfoResponse = ChargeInfoResponseR1(
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
                emailSource = Some(EmailSource("ETMP"))
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
          ChargeTypeAssessmentR1(
            debtTotalAmount = BigInt(1000),
            chargeReference = ChargeReference("CHARGE REFERENCE"),
            parentChargeReference = Some(ChargeInfoParentChargeReference("PARENT CHARGE REF")),
            mainTrans = MainTrans("2000"),
            charges = List(
              ChargeR1(
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

      def chargeInfoResponseR2: ChargeInfoResponse = ChargeInfoResponseR2(
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
                emailSource = Some(EmailSource("ETMP"))
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
        customerSignals = Some(
          List(
            Signal(SignalType("Rls"), SignalValue("signal value"), Some("description")),
            Signal(SignalType("Welsh Language Signal"), SignalValue("signal value"), Some("description"))
          )
        ),
        chargeTypeAssessment = List(
          ChargeTypeAssessmentR2(
            debtTotalAmount = BigInt(1000),
            chargeReference = ChargeReference("CHARGE REFERENCE"),
            parentChargeReference = Some(ChargeInfoParentChargeReference("PARENT CHARGE REF")),
            mainTrans = MainTrans("2000"),
            charges = List(
              ChargeR2(
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
                tieBreaker = Some(TieBreaker("Tie Breaker")),
                saTaxYearEnd = Some(SaTaxYearEnd(LocalDate.parse("2020-04-05"))),
                creationDate = Some(CreationDate(LocalDate.parse("2025-07-02"))),
                originalChargeType = Some(OriginalChargeType("Original Charge Type")),
                locks = Some(
                  List(
                    Lock(lockType = "Posting/Clearing", lockReason = "No Reallocation")
                  )
                )
              )
            ),
            isInsolvent = IsInsolvent(false)
          )
        ),
        chargeTypesExcluded = ChargeTypesExcluded(false)
      )

      def chargeInfoResponseR1JsonFromEligibility: JsObject = Json
        .parse(
          """{
            |  "processingDateTime" : "2025-07-02T15:00:41.689",
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
            |    "addresses" : [
            |    {
            |      "addressLine1" : "Address Line 1",
            |      "addressLine2" : "Address Line 2",
            |      "addressLine3" : "Address Line 3",
            |      "addressLine4" : "Address Line 4",
            |      "addressType" : "Address Type",
            |      "contactDetails" : {
            |        "emailAddress" : "email address",
            |        "emailSource" : "ETMP",
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
            |      "parentChargeReference" : "PARENT CHARGE REF",
            |      "isInsolvent": false
            |    }
            |  ],
            |  "chargeTypesExcluded": false
            |}
            |""".stripMargin
        )
        .as[JsObject]

      def chargeInfoResponseR2JsonFromEligibility: JsObject = chargeInfoResponseR1JsonFromEligibility ++ Json
        .parse("""{
                 |  "customerSignals": [
                 |    {
                 |      "signalType": "Rls",
                 |      "signalValue": "signal value",
                 |      "signalDescription": "description"
                 |    },
                 |    {
                 |      "signalType": "Welsh Language Signal",
                 |      "signalValue": "signal value",
                 |      "signalDescription": "description"
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
                 |          "outstandingAmount" : 500,
                 |          "parentMainTrans" : "Parent Main Transaction",
                 |          "saTaxYearEnd" : "2020-04-05",
                 |          "subTrans" : "1000",
                 |          "taxPeriodFrom" : "2020-01-02",
                 |          "taxPeriodTo" : "2020-12-31",
                 |          "tieBreaker" : "Tie Breaker",
                 |          "locks": [
                 |            {
                 |              "lockType": "Posting/Clearing",
                 |              "lockReason": "No Reallocation"
                 |            }
                 |          ]
                 |        }
                 |      ],
                 |      "debtTotalAmount" : 1000,
                 |      "mainTrans" : "2000",
                 |      "parentChargeReference" : "PARENT CHARGE REF",
                 |      "isInsolvent": false
                 |    }
                 |  ],
                 |  "chargeTypesExcluded" : false
                 |}
                 |""".stripMargin)
        .as[JsObject]

      def chargeInfoResponseR1JsonFromProxy: JsObject = Json
        .parse(
          """{
            |  "processingDateTime" : "2025-07-02T15:00:41.689",
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
            |    "addresses" : [
            |    {
            |      "addressLine1" : "Address Line 1",
            |      "addressLine2" : "Address Line 2",
            |      "addressLine3" : "Address Line 3",
            |      "addressLine4" : "Address Line 4",
            |      "addressType" : "Address Type",
            |      "contactDetails" : {
            |        "emailAddress" : "email address",
            |        "emailSource" : "ETMP",
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
            |  ]
            |}
            |""".stripMargin
        )
        .as[JsObject]

      def chargeInfoResponseR2JsonFromProxy: JsObject = chargeInfoResponseR1JsonFromProxy ++ Json
        .parse("""{
                 |  "customerSignals": [
                 |    {
                 |      "signalType": "Rls",
                 |      "signalValue": "signal value",
                 |      "signalDescription": "description"
                 |    },
                 |    {
                 |      "signalType": "Welsh Language Signal",
                 |      "signalValue": "signal value",
                 |      "signalDescription": "description"
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
                 |          "outstandingAmount" : 500,
                 |          "parentMainTrans" : "Parent Main Transaction",
                 |          "saTaxYearEnd" : "2020-04-05",
                 |          "subTrans" : "1000",
                 |          "taxPeriodFrom" : "2020-01-02",
                 |          "taxPeriodTo" : "2020-12-31",
                 |          "tieBreaker" : "Tie Breaker",
                 |          "locks": [
                 |            {
                 |              "lockType": "Posting/Clearing",
                 |              "lockReason": "No Reallocation"
                 |            }
                 |          ]
                 |        }
                 |      ],
                 |      "debtTotalAmount" : 1000,
                 |      "mainTrans" : "2000",
                 |      "parentChargeReference" : "PARENT CHARGE REF",
                 |      "isInsolvent": false
                 |    }
                 |  ],
                 |  "chargeTypesExcluded" : false
                 |}
                 |""".stripMargin)
        .as[JsObject]
    }

    object With1SomeOnEachPath {
      def chargeInfoResponseR1: ChargeInfoResponse = ChargeInfoResponseR1(
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
          ChargeTypeAssessmentR1(
            debtTotalAmount = BigInt(1000),
            chargeReference = ChargeReference("CHARGE REFERENCE"),
            parentChargeReference = Some(ChargeInfoParentChargeReference("PARENT CHARGE REF")),
            mainTrans = MainTrans("2000"),
            charges = List(
              ChargeR1(
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

      def chargeInfoResponseR2: ChargeInfoResponse = ChargeInfoResponseR2(
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
        customerSignals = Some(
          List(
            Signal(SignalType("Welsh Language Signal"), SignalValue("signal value"), None)
          )
        ),
        chargeTypeAssessment = List(
          ChargeTypeAssessmentR2(
            debtTotalAmount = BigInt(1000),
            chargeReference = ChargeReference("CHARGE REFERENCE"),
            parentChargeReference = Some(ChargeInfoParentChargeReference("PARENT CHARGE REF")),
            mainTrans = MainTrans("2000"),
            charges = List(
              ChargeR2(
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
                tieBreaker = Some(TieBreaker("Tie Breaker")),
                saTaxYearEnd = Some(SaTaxYearEnd(LocalDate.parse("2020-04-05"))),
                creationDate = Some(CreationDate(LocalDate.parse("2025-07-02"))),
                originalChargeType = Some(OriginalChargeType("Original Charge Type")),
                locks = Some(
                  List(
                    Lock(lockType = "Posting/Clearing", lockReason = "No Reallocation")
                  )
                )
              )
            ),
            isInsolvent = IsInsolvent(false)
          )
        ),
        chargeTypesExcluded = ChargeTypesExcluded(false)
      )

      def chargeInfoResponseR1JsonFromEligibility: JsObject = Json
        .parse(
          """{
            |  "processingDateTime" : "2025-07-02T15:00:41.689",
            |    "identification" : [
            |    {
            |      "idType" : "ID_TYPE",
            |      "idValue" : "ID_VALUE"
            |    }
            |  ],
            |    "individualDetails" : {
            |    "customerType" : "MTD(ITSA)",
            |    "transitionToCDCS" : true
            |  },
            |    "addresses" : [
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
            |    "chargeTypeAssessment" : [
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
            |      "parentChargeReference" : "PARENT CHARGE REF",
            |      "isInsolvent": false
            |    }
            |  ],
            |  "chargeTypesExcluded": false
            |}
            |""".stripMargin
        )
        .as[JsObject]

      def chargeInfoResponseR2JsonFromEligibility: JsObject = chargeInfoResponseR1JsonFromEligibility ++ Json
        .parse("""{
                 |  "customerSignals": [
                 |    {
                 |      "signalType": "Welsh Language Signal",
                 |      "signalValue": "signal value"
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
                 |          "outstandingAmount" : 500,
                 |          "parentMainTrans" : "Parent Main Transaction",
                 |          "saTaxYearEnd" : "2020-04-05",
                 |          "subTrans" : "1000",
                 |          "taxPeriodFrom" : "2020-01-02",
                 |          "taxPeriodTo" : "2020-12-31",
                 |          "tieBreaker" : "Tie Breaker",
                            "locks": [
                 |            {
                 |              "lockType": "Posting/Clearing",
                 |              "lockReason": "No Reallocation"
                 |            }
                 |          ]
                 |        }
                 |      ],
                 |      "debtTotalAmount" : 1000,
                 |      "mainTrans" : "2000",
                 |      "parentChargeReference" : "PARENT CHARGE REF",
                 |      "isInsolvent": false
                 |    }
                 |  ],
                 |  "chargeTypesExcluded": false
                 |}""".stripMargin)
        .as[JsObject]

      def chargeInfoResponseR1JsonFromProxy: JsObject = Json
        .parse(
          """{
            |  "processingDateTime" : "2025-07-02T15:00:41.689",
            |    "identification" : [
            |    {
            |      "idType" : "ID_TYPE",
            |      "idValue" : "ID_VALUE"
            |    }
            |  ],
            |    "individualDetails" : {
            |    "customerType" : "MTD(ITSA)",
            |    "transitionToCDCS" : true
            |  },
            |    "addresses" : [
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
            |    "chargeTypeAssessment" : [
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
            |  ]
            |}
            |""".stripMargin
        )
        .as[JsObject]

      def chargeInfoResponseR2JsonFromProxy: JsObject = chargeInfoResponseR1JsonFromProxy ++ Json
        .parse("""{
                 |  "customerSignals": [
                 |    {
                 |      "signalType": "Welsh Language Signal",
                 |      "signalValue": "signal value"
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
                 |          "outstandingAmount" : 500,
                 |          "parentMainTrans" : "Parent Main Transaction",
                 |          "saTaxYearEnd" : "2020-04-05",
                 |          "subTrans" : "1000",
                 |          "taxPeriodFrom" : "2020-01-02",
                 |          "taxPeriodTo" : "2020-12-31",
                 |          "tieBreaker" : "Tie Breaker",
                            "locks": [
                 |            {
                 |              "lockType": "Posting/Clearing",
                 |              "lockReason": "No Reallocation"
                 |            }
                 |          ]
                 |        }
                 |      ],
                 |      "debtTotalAmount" : 1000,
                 |      "mainTrans" : "2000",
                 |      "parentChargeReference" : "PARENT CHARGE REF",
                 |      "isInsolvent": false
                 |    }
                 |  ],
                 |  "chargeTypesExcluded": false
                 |}""".stripMargin)
        .as[JsObject]
    }

    object With0SomeOnEachPath {
      def chargeInfoResponseR1: ChargeInfoResponse = ChargeInfoResponseR1(
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
          ChargeTypeAssessmentR1(
            debtTotalAmount = BigInt(1000),
            chargeReference = ChargeReference("CHARGE REFERENCE"),
            parentChargeReference = None,
            mainTrans = MainTrans("2000"),
            charges = List(
              ChargeR1(
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

      def chargeInfoResponseR2: ChargeInfoResponse = ChargeInfoResponseR2(
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
        customerSignals = Some(
          List(
            Signal(SignalType("Welsh Language Signal"), SignalValue("signal value"), None)
          )
        ),
        chargeTypeAssessment = List(
          ChargeTypeAssessmentR2(
            debtTotalAmount = BigInt(1000),
            chargeReference = ChargeReference("CHARGE REFERENCE"),
            parentChargeReference = None,
            mainTrans = MainTrans("2000"),
            charges = List(
              ChargeR2(
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
                tieBreaker = None,
                saTaxYearEnd = None,
                creationDate = None,
                originalChargeType = None,
                locks = None
              )
            ),
            isInsolvent = IsInsolvent(false)
          )
        ),
        chargeTypesExcluded = ChargeTypesExcluded(false)
      )

      def chargeInfoResponseR1JsonFromEligibility: JsObject = Json
        .parse(
          """{
            |  "processingDateTime" : "2025-07-02T15:00:41.689",
            |    "identification" : [
            |    {
            |      "idType" : "ID_TYPE",
            |      "idValue" : "ID_VALUE"
            |    }
            |  ],
            |    "individualDetails" : {
            |    "customerType" : "MTD(ITSA)",
            |    "transitionToCDCS" : true
            |  },
            |    "addresses" : [
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
            |      "mainTrans" : "2000",
            |      "isInsolvent": false
            |    }
            |  ],
            |  "chargeTypesExcluded": false
            |}
            |""".stripMargin
        )
        .as[JsObject]

      def chargeInfoResponseR2JsonFromEligibility: JsObject = chargeInfoResponseR1JsonFromEligibility ++ Json
        .parse("""{
                 |  "customerSignals": [
                 |    {
                 |      "signalType": "Welsh Language Signal",
                 |      "signalValue": "signal value"
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
                 |      "mainTrans" : "2000",
                 |      "isInsolvent": false
                 |    }
                 |  ],
                 |  "chargeTypesExcluded" : false
                 |}""".stripMargin)
        .as[JsObject]

      def chargeInfoResponseR1JsonFromProxy: JsObject = Json
        .parse(
          """{
            |  "processingDateTime" : "2025-07-02T15:00:41.689",
            |    "identification" : [
            |    {
            |      "idType" : "ID_TYPE",
            |      "idValue" : "ID_VALUE"
            |    }
            |  ],
            |    "individualDetails" : {
            |    "customerType" : "MTD(ITSA)",
            |    "transitionToCDCS" : true
            |  },
            |    "addresses" : [
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
            |  ]
            |}
            |""".stripMargin
        )
        .as[JsObject]

      def chargeInfoResponseR2JsonFromProxy: JsObject = chargeInfoResponseR1JsonFromProxy ++ Json
        .parse("""{
                 |  "customerSignals": [
                 |    {
                 |      "signalType": "Welsh Language Signal",
                 |      "signalValue": "signal value"
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
                 |      "mainTrans" : "2000",
                 |      "isInsolvent": false
                 |    }
                 |  ],
                 |  "chargeTypesExcluded": false
                 |}""".stripMargin)
        .as[JsObject]
    }
  }
}
