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
import play.api.libs.functional.syntax._

class GenerateQuoteRequestSpec extends AnyWordSpec with Matchers {
  val generateQuoteRequest = GenerateQuoteRequest(
    CustomerReference("uniqRef1234"),
    ChannelIdentifier.SelfService,
    PlanToGenerateQuote(
      QuoteType.InstalmentAmount,
      LocalDate.of(2021, 5, 13),
      LocalDate.of(2021, 5, 13),
      100,
      Frequency.Annually,
      Duration(12),
      Some(100),
      Some(LocalDate.of(2021, 5, 13)),
      PaymentPlanType.TimeToPay
    ),
    List(CustomerPostCode(PostCode("NW9 5XW"), LocalDate.of(2021, 5, 13))),
    List(
      DebtItem(
        DebtItemId("debtItemId1"),
        DebtItemChargeId("debtItemChargeId1"),
        MainTransType.ChBDebt,
        SubTransType.HIPG,
        100,
        LocalDate.of(2021, 5, 13),
        List(Payment(LocalDate.of(2021, 5, 13), 100))
      )
    )
  )
  val generateQuoteRequestWithMissingOptionalValues = GenerateQuoteRequest(
    CustomerReference("uniqRef1234"),
    ChannelIdentifier.SelfService,
    PlanToGenerateQuote(
      QuoteType.InstalmentAmount,
      LocalDate.of(2021, 5, 13),
      LocalDate.of(2021, 5, 13),
      100,
      Frequency.Annually,
      Duration(12),
      None,
      None,
      PaymentPlanType.TimeToPay
    ),
    List(CustomerPostCode(PostCode("NW9 5XW"), LocalDate.of(2021, 5, 13))),
    List(
      DebtItem(
        DebtItemId("debtItemId1"),
        DebtItemChargeId("debtItemChargeId1"),
        MainTransType.ChBDebt,
        SubTransType.HIPG,
        100,
        LocalDate.of(2021, 5, 13),
        List(Payment(LocalDate.of(2021, 5, 13), 100))
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
               |  "debtItems": [
               |    {
               |      "debtItemId": "debtItemId1",
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
  val jsonWithMissingOptionalValues =
    """{
               |  "customerReference": "uniqRef1234",
               |  "channelIdentifier": "selfService",
               |  "plan": {
               |    "quoteType": "instalmentAmount",
               |    "quoteDate": "2021-05-13",
               |    "instalmentStartDate": "2021-05-13",
               |    "instalmentAmount": 100,
               |    "frequency": "annually",
               |    "duration": 12,
               |    "paymentPlanType": "timeToPay"
               |  },
               |  "customerPostCodes": [
               |    {
               |      "addressPostcode": "NW9 5XW",
               |      "postcodeDate": "2021-05-13"
               |    }
               |  ],
               |  "debtItems": [
               |    {
               |      "debtItemId": "debtItemId1",
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

  "GenerateQuoteRequest" should {
    "be correctly encoded and decoded" in {
      Json.toJson(generateQuoteRequest) shouldEqual (Json.parse(json))
    }
    "be correctly encoded and decoded for missing optional values" in {
      Json.toJson(generateQuoteRequestWithMissingOptionalValues) shouldEqual (Json.parse(jsonWithMissingOptionalValues))
    }
  }
}
