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
import java.lang.IllegalArgumentException
import java.time.LocalDate

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.{Failure, Try}

class GenerateQuoteRequestSpec extends AnyWordSpec with Matchers {
  val generateQuoteRequest = GenerateQuoteRequest(
    CustomerReference("uniqRef1234"),
    ChannelIdentifier.SelfService,
    PlanToGenerateQuote(
      QuoteType.InstalmentAmount,
      LocalDate.of(2021, 5, 13),
      LocalDate.of(2021, 5, 13),
      Some(100),
      Some(Frequency.Annually),
      Some(Duration(12)),
      Some(100),
      Some(LocalDate.of(2021, 5, 13)),
      PaymentPlanType.TimeToPay
    ),
    List(CustomerPostCode(PostCode("NW9 5XW"), LocalDate.of(2021, 5, 13))),
    List(
      DebtItemCharge(
        DebtItemChargeId("debtItemChargeId1"),
        MainTransType.ChBDebt,
        SubTransType.HIPG,
        100,
        Some(LocalDate.of(2021, 5, 13)),
        Some(List(Payment(LocalDate.of(2021, 5, 13), 100)))
      )
    )
  )

  val json = """{
               |  "customerReference": "uniqRef1234",
               |  "channelIdentifier": "selfService",
               |  "plan": {
               |    "quoteType": "instalmentAmount",
               |    "quoteDate": "2021-05-13",
               |    "instalmentStartDate": "2021-05-13",
               |    "instalmentAmount": 100,
               |    "frequency": "annually",
               |    "duration": 12,
               |    "initialPaymentAmount": 100,
               |    "initialPaymentDate": "2021-05-13",
               |    "paymentPlanType": "timeToPay"
               |  },
               |  "customerPostCodes": [
               |    {
               |      "addressPostcode": "NW9 5XW",
               |      "postcodeDate": "2021-05-13"
               |    }
               |  ],
               |  "debtItemCharges": [
               |    {
               |      "debtItemChargeId": "debtItemChargeId1",
               |      "mainTrans": "5330",
               |      "subTrans": "1180",
               |      "originalDebtAmount": 100,
               |      "interestStartDate": "2021-05-13",
               |      "paymentHistory": [
               |        {
               |          "paymentDate": "2021-05-13",
               |          "paymentAmount": 100
               |        }
               |      ]
               |    }
               |  ]
               |}
               """.stripMargin

  def getJsonWithInvalidReference(customerReference: String = "uniqRef1234",
                                  instalmentAmount: BigDecimal = 100,
                                  initialPaymentAmount: BigDecimal = 100,
                                  originalDebtAmount: BigDecimal = 100,
                                  paymentAmount: BigDecimal = 100) = s"""{
               |  "customerReference": "$customerReference",
               |  "channelIdentifier": "selfService",
               |  "plan": {
               |    "quoteType": "instalmentAmount",
               |    "quoteDate": "2021-05-13",
               |    "instalmentStartDate": "2021-05-13",
               |    "instalmentAmount": $instalmentAmount,
               |    "frequency": "annually",
               |    "duration": 12,
               |    "initialPaymentAmount": $initialPaymentAmount,
               |    "initialPaymentDate": "2021-05-13",
               |    "paymentPlanType": "timeToPay"
               |  },
               |  "customerPostCodes": [
               |    {
               |      "addressPostcode": "NW9 5XW",
               |      "postcodeDate": "2021-05-13"
               |    }
               |  ],
               |  "debtItemCharges": [
               |    {
               |      "debtItemChargeId": "debtItemChargeId1",
               |      "mainTrans": "5330",
               |      "subTrans": "1180",
               |      "originalDebtAmount": $originalDebtAmount,
               |      "interestStartDate": "2021-05-13",
               |      "paymentHistory": [
               |        {
               |          "paymentDate": "2021-05-13",
               |          "paymentAmount": $paymentAmount
               |        }
               |      ]
               |    }
               |  ]
               |}
             """.stripMargin

  "GenerateQuoteRequest" should {
    "be correctly encoded and decoded" in {
      Json.toJson(generateQuoteRequest) shouldEqual (Json.parse(json))
    }

    "fail decoding if customerReference is empty" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(customerReference = ""))
          .validate[GenerateQuoteRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: customerReference should not be empty"
        case _ => fail()
      }
    }

    "fail decoding if instalmentAmount is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(instalmentAmount = -10))
          .validate[GenerateQuoteRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: instalmentAmount should be a positive amount."
        case _ => fail()
      }
    }

    "fail decoding if initialPaymentAmount is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(initialPaymentAmount = -200))
          .validate[GenerateQuoteRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: initialPaymentAmount should be a positive amount."
        case _ => fail()
      }
    }

    "fail decoding if originalDebtAmount is negative" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(originalDebtAmount = -200))
          .validate[GenerateQuoteRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: originalDebtAmount should be a positive amount."
        case _ => fail()
      }
    }

    "fail decoding if paymentAmount is zero" in {
      import play.api.libs.json._

      Try(
        Json
          .parse(getJsonWithInvalidReference(paymentAmount = 0))
          .validate[GenerateQuoteRequest]
      ) match {
        case Failure(t) =>
          t.toString() shouldBe "java.lang.IllegalArgumentException: requirement failed: paymentAmount should be a positive amount."
        case _ => fail()
      }
    }

  }
}
