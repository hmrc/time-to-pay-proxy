/*
 * Copyright 2021 HM Revenue & Customs
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

import scala.util.{Failure, Try}

class CreatePlanRequestSpec extends AnyWordSpec with Matchers {
  private val createPlanRequest =
    CreatePlanRequest(
      CustomerReference("customerReference"),
      QuoteReference("quoteReference"),
      ChannelIdentifier.Advisor,
      PlanToCreatePlan(
        QuoteId("quoteId1"),
        QuoteType.InstalmentAmount,
        LocalDate.parse("2021-05-13"),
        LocalDate.parse("2021-05-13"),
        100,
        PaymentPlanType.TimeToPay,
        true,
        1,
        Frequency.Annually,
        Duration(12),
        LocalDate.parse("2021-05-13"),
        100,
        100,
        10,
        10,
        10
      ),
      List(
        DebtItem(
          DebtItemId("debtItemId1"),
          DebtItemChargeId("debtItemChargeId1"),
          MainTransType.TPSSAccTaxAssessment,
          SubTransType.IT,
          100,
          LocalDate.parse("2021-05-13"),
          List(Payment(LocalDate.parse("2021-05-13"), 100))
        )
      ),
      List(PaymentInformation(PaymentMethod.Bacs, PaymentReference("ref123"))),
      List(CustomerPostCode(PostCode("NW9 5XW"), LocalDate.parse("2021-05-13"))),
      List(
        Instalment(
          DebtItemChargeId("debtItemChargeId1"),
          DebtItemId("debtItemId1"),
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

  val json = """{
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
               |    "totalDebtincInt": 100,
               |    "totalInterest": 10,
               |    "interestAccrued": 10,
               |    "planInterest": 10
               |  },
               |  "debtItems": [
               |    {
               |      "debtItemId": "debtItemId1",
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
               |    "debtItemId": "debtItemId1",
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

  private def getJsonWithEmptyReference(
                                         quoteReference: QuoteReference = QuoteReference("quoteReference"),
                                         customerReference: CustomerReference = CustomerReference("customerReference"),
                                         paymentReference: PaymentReference = PaymentReference("ref123")
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
      |    "instalmentAmount": 100,
      |    "paymentPlanType": "timeToPay",
      |    "thirdPartyBank": true,
      |    "numberOfInstalments": 1,
      |    "frequency": "annually",
      |    "duration": 12,
      |    "initialPaymentDate": "2021-05-13",
      |    "initialPaymentAmount": 100,
      |    "totalDebtincInt": 100,
      |    "totalInterest": 10,
      |    "interestAccrued": 10,
      |    "planInterest": 10
      |  },
      |  "debtItems": [
      |    {
      |      "debtItemId": "debtItemId1",
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
      |    "debtItemId": "debtItemId1",
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

  "CreatePlanRequest" should {
    "be correctly encoded and decoded" in {
      Json.toJson(createPlanRequest) shouldEqual (Json.parse(json))
    }

    "fail decoding if paymentReference is empty" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithEmptyReference(paymentReference = PaymentReference("")))
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
          .parse(getJsonWithEmptyReference(customerReference = CustomerReference("")))
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
          .parse(getJsonWithEmptyReference(quoteReference = QuoteReference("")))
          .validate[CreatePlanRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: quoteReference should not be empty"
        case _ => fail()
      }
    }

  }
}
