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

class GenerateQuoteResponseSpec extends AnyWordSpec with Matchers {
  val generateQuoteResponse = GenerateQuoteResponse(
    QuoteReference("quoteRef1234"),
    CustomerReference("custRef1234"),
    QuoteType.InstalmentAmount,
    LocalDate.parse("2021-05-13"),
    1,
    10,
    10,
    0.25,
    0.25,
    List(
      Instalment(
        DebtItemChargeId("debtItemChargeId1"),
        LocalDate.parse("2021-05-13"),
        100,
        100,
        0.24,
        1,
        10,
        10
      )
    )
  )

  val json = """{
               |  "quoteReference": "quoteRef1234",
               |  "customerReference": "custRef1234",
               |  "quoteType": "instalmentAmount",
               |  "quoteDate": "2021-05-13",
               |  "numberOfInstalments": 1,
               |  "totalDebtIncInt": 10,
               |  "interestAccrued": 10,
               |  "planInterest": 0.25,
               |  "totalInterest": 0.25,
               |  "instalments": [
               |    {
               |      "debtItemChargeId": "debtItemChargeId1",
               |      "dueDate": "2021-05-13",
               |      "amountDue": 100,
               |      "expectedPayment": 100,
               |      "interestRate": 0.24,
               |      "instalmentNumber": 1,
               |      "instalmentInterestAccrued": 10,
               |      "instalmentBalance": 10
               |    }
               |  ]
               |
               |}""".stripMargin

  "GenerateQuoteResponse" should {
    "be correctly encoded and decoded" in {
      Json.toJson(generateQuoteResponse) shouldEqual (Json.parse(json))
    }
  }
}
