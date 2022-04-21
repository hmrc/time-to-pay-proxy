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

package uk.gov.hmrc.timetopayproxy.models

import java.time.LocalDate

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._

import scala.util.{ Failure, Try }

class CreatePlanRequestSpec extends AnyWordSpec with Matchers with CreatePlanRequestFixture {

  "CreatePlanRequest" should {
    "be correctly encoded and decoded" in {
      Json.toJson(createPlanRequest) shouldEqual (Json.parse(json))
      (Json.parse(json)).as[CreatePlanRequest] shouldEqual createPlanRequest

    }

    "be correctly encoded and decoded with payment reference None for methods except direct debit" in {
      (Json.parse(jsonWithEmptyReference)).as[CreatePlanRequest] shouldEqual createPlanRequestWithEmptyReference
      Json.toJson(createPlanRequestWithEmptyReference) shouldEqual (Json.parse(jsonWithEmptyReference))
    }

    "fail decoding if paymentReference is empty" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(
            getJsonWithInvalidReference(paymentReference = PaymentReference(""))
          )
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: paymentReference should not be empty"
        case _ => fail()
      }
    }

    "fail decoding if customerReference is empty" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(
            getJsonWithInvalidReference(
              customerReference = CustomerReference("")
            )
          )
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: customerReference should not be empty"
        case _ => fail()
      }
    }

    "fail decoding if quoteReference is empty" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(
            getJsonWithInvalidReference(quoteReference = QuoteReference(""))
          )
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: quoteReference should not be empty"
        case _ => fail()
      }
    }

    "fail decoding if instalmentAmount is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(instalmentAmount = -10))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: instalmentAmount should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }
    "fail decoding if numberOfInstalments is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(numberOfInstalments = -1))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: numberOfInstalments should be positive."
        case _ => fail("Response should be a validation error")
      }
    }

    "fail decoding if initialPaymentAmount is zero" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(initialPaymentAmount = 0))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: initialPaymentAmount should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }

    "fail decoding if totalDebtIncInt is zero" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(totalDebtIncInt = 0))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: totalDebtincInt should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }

    "fail decoding if totalInterest is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(totalInterest = -15))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: totalInterest should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }

    "fail decoding if interestAccrued is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(interestAccrued = -25))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: interestAccrued should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }

    "fail decoding if planInterest is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(planInterest = -5))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: planInterest should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }

    "fail decoding if originalDebtAmount is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(originalDebtAmount = -5))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: originalDebtAmount should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }
    "fail decoding if paymentAmount is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(paymentAmount = -5))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: paymentAmount should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }
    "fail decoding if amountDue is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(amountDue = -5))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: amountDue should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }
    "fail decoding if expectedPayment is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(expectedPayment = -5))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: expectedPayment should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }
    "fail decoding if interestRate is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(interestRate = -5))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: interestRate should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }
    "fail decoding if instalmentNumber is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(instalmentNumber = -5))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: instalmentNumber should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }

    "fail decoding if instalmentInterestAccrued is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(instalmentInterestAccrued = -5))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: instalmentInterestAccrued should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }
    "fail decoding if instalmentBalance is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(instalmentBalance = -5))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: instalmentBalance should be a positive amount."
        case _ => fail("Response should be a validation error")
      }
    }

  }
}

trait CreatePlanRequestFixture {
  protected val createPlanRequest =
    CreatePlanRequest(
      CustomerReference("customerReference"),
      QuoteReference("quoteReference"),
      ChannelIdentifier.Advisor,
      PlanToCreatePlan(
        QuoteId("quoteId1"),
        QuoteType.InstalmentAmount,
        LocalDate.parse("2021-05-13"),
        LocalDate.parse("2021-05-13"),
        Some(100),
        PaymentPlanType.TimeToPay,
        true,
        1,
        Some(Frequency.Annually),
        Some(Duration(12)),
        Some(LocalDate.parse("2021-05-13")),
        Some(100),
        100,
        10,
        10,
        10
      ),
      List(
        DebtItemCharge(
          DebtItemChargeId("debtItemChargeId1"),
          MainTransType.TPSSAccTaxAssessment,
          SubTransType.IT,
          100,
          Some(LocalDate.parse("2021-05-13")),
          List(Payment(LocalDate.parse("2021-05-13"), 100))
        )
      ),
      List(PaymentInformation(PaymentMethod.Bacs, Some(PaymentReference("ref123")))),
      List(
        CustomerPostCode(PostCode("NW9 5XW"), LocalDate.parse("2021-05-13"))
      ),
      List(
        Instalment(
          DebtItemChargeId("debtItemChargeId1"),
          LocalDate.parse("2021-05-13"),
          100,
          100,
          0.25,
          1,
          10,
          90
        )
      )
    )
  protected val json = """{
                         |  "customerReference": "customerReference",
                         |  "quoteReference":"quoteReference",
                         |  "channelIdentifier": "advisor",
                         |  "plan": {
                         |    "quoteId": "quoteId1",
                         |    "quoteType": "instalmentAmount",
                         |    "quoteDate": "2021-05-13",
                         |    "instalmentStartDate": "2021-05-13",
                         |    "instalmentAmount": 100,
                         |    "paymentPlanType": "timeToPay",
                         |    "thirdPartyBank": true,
                         |    "numberOfInstalments": 1,
                         |    "frequency": "annually",
                         |    "duration": 12,
                         |    "initialPaymentDate": "2021-05-13",
                         |    "initialPaymentAmount": 100,
                         |    "totalDebtIncInt": 100,
                         |    "totalInterest": 10,
                         |    "interestAccrued": 10,
                         |    "planInterest": 10
                         |  },
                         |  "debtItemCharges": [
                         |    {
                         |      "debtItemChargeId": "debtItemChargeId1",
                         |      "mainTrans": "1525",
                         |      "subTrans": "1000",
                         |      "originalDebtAmount": 100,
                         |      "interestStartDate": "2021-05-13",
                         |      "paymentHistory": [
                         |        {
                         |          "paymentDate": "2021-05-13",
                         |          "paymentAmount": 100
                         |        }
                         |      ]
                         |    }
                         |  ],
                         |  "payments": [
                         |    {
                         |      "paymentMethod": "BACS",
                         |      "paymentReference": "ref123"
                         |    }
                         |  ],
                         |  "customerPostCodes": [
                         |    {
                         |      "addressPostcode": "NW9 5XW",
                         |      "postcodeDate": "2021-05-13"
                         |    }
                         |  ],
                         |  "instalments": [
                         |  {
                         |    "debtItemChargeId": "debtItemChargeId1",
                         |    "dueDate": "2021-05-13",
                         |    "amountDue": 100,
                         |    "expectedPayment": 100,
                         |    "interestRate": 0.25,
                         |    "instalmentNumber": 1,
                         |    "instalmentInterestAccrued": 10,
                         |    "instalmentBalance": 90
                         |  }
                         |  ]
                         |}
               """.stripMargin
  protected val jsonWithEmptyReference = """{
                                           |  "customerReference": "customerReference",
                                           |  "quoteReference":"quoteReference",
                                           |  "channelIdentifier": "advisor",
                                           |  "plan": {
                                           |    "quoteId": "quoteId1",
                                           |    "quoteType": "instalmentAmount",
                                           |    "quoteDate": "2021-05-13",
                                           |    "instalmentStartDate": "2021-05-13",
                                           |    "instalmentAmount": 100,
                                           |    "paymentPlanType": "timeToPay",
                                           |    "thirdPartyBank": true,
                                           |    "numberOfInstalments": 1,
                                           |    "frequency": "annually",
                                           |    "duration": 12,
                                           |    "initialPaymentDate": "2021-05-13",
                                           |    "initialPaymentAmount": 100,
                                           |    "totalDebtIncInt": 100,
                                           |    "totalInterest": 10,
                                           |    "interestAccrued": 10,
                                           |    "planInterest": 10
                                           |  },
                                           |  "debtItemCharges": [
                                           |    {
                                           |      "debtItemChargeId": "debtItemChargeId1",
                                           |      "mainTrans": "1525",
                                           |      "subTrans": "1000",
                                           |      "originalDebtAmount": 100,
                                           |      "interestStartDate": "2021-05-13",
                                           |      "paymentHistory": [
                                           |        {
                                           |          "paymentDate": "2021-05-13",
                                           |          "paymentAmount": 100
                                           |        }
                                           |      ]
                                           |    }
                                           |  ],
                                           |  "payments": [
                                           |    {
                                           |      "paymentMethod": "BACS"
                                           |    }
                                           |  ],
                                           |  "customerPostCodes": [
                                           |    {
                                           |      "addressPostcode": "NW9 5XW",
                                           |      "postcodeDate": "2021-05-13"
                                           |    }
                                           |  ],
                                           |  "instalments": [
                                           |  {
                                           |    "debtItemChargeId": "debtItemChargeId1",
                                           |    "dueDate": "2021-05-13",
                                           |    "amountDue": 100,
                                           |    "expectedPayment": 100,
                                           |    "interestRate": 0.25,
                                           |    "instalmentNumber": 1,
                                           |    "instalmentInterestAccrued": 10,
                                           |    "instalmentBalance": 90
                                           |  }
                                           |  ]
                                           |}
               """.stripMargin
  protected val jsonWithDirectDebitAndEmptyReference = """{
                                                         |  "customerReference": "customerReference",
                                                         |  "quoteReference":"quoteReference",
                                                         |  "channelIdentifier": "advisor",
                                                         |  "plan": {
                                                         |    "quoteId": "quoteId1",
                                                         |    "quoteType": "instalmentAmount",
                                                         |    "quoteDate": "2021-05-13",
                                                         |    "instalmentStartDate": "2021-05-13",
                                                         |    "instalmentAmount": 100,
                                                         |    "paymentPlanType": "timeToPay",
                                                         |    "thirdPartyBank": true,
                                                         |    "numberOfInstalments": 1,
                                                         |    "frequency": "annually",
                                                         |    "duration": 12,
                                                         |    "initialPaymentDate": "2021-05-13",
                                                         |    "initialPaymentAmount": 100,
                                                         |    "totalDebtIncInt": 100,
                                                         |    "totalInterest": 10,
                                                         |    "interestAccrued": 10,
                                                         |    "planInterest": 10
                                                         |  },
                                                         |  "debtItemCharges": [
                                                         |    {
                                                         |      "debtItemChargeId": "debtItemChargeId1",
                                                         |      "mainTrans": "1525",
                                                         |      "subTrans": "1000",
                                                         |      "originalDebtAmount": 100,
                                                         |      "interestStartDate": "2021-05-13",
                                                         |      "paymentHistory": [
                                                         |        {
                                                         |          "paymentDate": "2021-05-13",
                                                         |          "paymentAmount": 100
                                                         |        }
                                                         |      ]
                                                         |    }
                                                         |  ],
                                                         |  "payments": [
                                                         |    {
                                                         |      "paymentMethod": "directDebit"
                                                         |    }
                                                         |  ],
                                                         |  "customerPostCodes": [
                                                         |    {
                                                         |      "addressPostcode": "NW9 5XW",
                                                         |      "postcodeDate": "2021-05-13"
                                                         |    }
                                                         |  ],
                                                         |  "instalments": [
                                                         |  {
                                                         |    "debtItemChargeId": "debtItemChargeId1",
                                                         |    "dueDate": "2021-05-13",
                                                         |    "amountDue": 100,
                                                         |    "expectedPayment": 100,
                                                         |    "interestRate": 0.25,
                                                         |    "instalmentNumber": 1,
                                                         |    "instalmentInterestAccrued": 10,
                                                         |    "instalmentBalance": 90
                                                         |  }
                                                         |  ]
                                                         |}
               """.stripMargin
  protected def getJsonWithInvalidReference(
    quoteReference: QuoteReference = QuoteReference("quoteReference"),
    customerReference: CustomerReference = CustomerReference("customerReference"),
    paymentReference: PaymentReference = PaymentReference("ref123"),
    instalmentAmount: BigDecimal = 100,
    numberOfInstalments: Int = 1,
    initialPaymentAmount: BigDecimal = 100,
    totalDebtIncInt: BigDecimal = 100,
    totalInterest: BigDecimal = 10,
    interestAccrued: BigDecimal = 10,
    planInterest: BigDecimal = 10,
    originalDebtAmount: BigDecimal = 100,
    paymentAmount: BigDecimal = 100,
    amountDue: BigDecimal = 100,
    expectedPayment: BigDecimal = 100,
    interestRate: Double = 0.25,
    instalmentNumber: Int = 1,
    instalmentInterestAccrued: BigDecimal = 10,
    instalmentBalance: BigDecimal = 90
  ) =
    s"""{
       |  "customerReference": "${customerReference.value}",
       |  "quoteReference":"${quoteReference.value}",
       |  "channelIdentifier": "advisor",
       |  "plan": {
       |    "quoteId": "quoteId1",
       |    "quoteType": "instalmentAmount",
       |    "quoteDate": "2021-05-13",
       |    "instalmentStartDate": "2021-05-13",
       |    "instalmentAmount": $instalmentAmount,
       |    "paymentPlanType": "timeToPay",
       |    "thirdPartyBank": true,
       |    "numberOfInstalments": $numberOfInstalments,
       |    "frequency": "annually",
       |    "duration": 12,
       |    "initialPaymentDate": "2021-05-13",
       |    "initialPaymentAmount": $initialPaymentAmount,
       |    "totalDebtIncInt": $totalDebtIncInt,
       |    "totalInterest": $totalInterest,
       |    "interestAccrued": $interestAccrued,
       |    "planInterest": $planInterest
       |  },
       |  "debtItemCharges": [
       |    {
       |      "debtItemChargeId": "debtItemChargeId1",
       |      "mainTrans": "1525",
       |      "subTrans": "1000",
       |      "originalDebtAmount": $originalDebtAmount,
       |      "interestStartDate": "2021-05-13",
       |      "paymentHistory": [
       |        {
       |          "paymentDate": "2021-05-13",
       |          "paymentAmount": $paymentAmount
       |        }
       |      ]
       |    }
       |  ],
       |  "payments": [
       |    {
       |      "paymentMethod": "BACS",
       |      "paymentReference": "${paymentReference.value}"
       |    }
       |  ],
       |  "customerPostCodes": [
       |    {
       |      "addressPostcode": "NW9 5XW",
       |      "postcodeDate": "2021-05-13"
       |    }
       |  ],
       |  "instalments": [
       |  {
       |    "debtItemChargeId": "debtItemChargeId1",
       |    "dueDate": "2021-05-13",
       |    "amountDue": $amountDue,
       |    "expectedPayment": $expectedPayment,
       |    "interestRate": $interestRate,
       |    "instalmentNumber": $instalmentNumber,
       |    "instalmentInterestAccrued": $instalmentInterestAccrued,
       |    "instalmentBalance": $instalmentBalance
       |  }
       |  ]
       |}
    """.stripMargin

  protected val createPlanRequestWithEmptyReference =
    createPlanRequest.copy(payments = List(createPlanRequest.payments.head.copy(paymentReference = None)))

}
