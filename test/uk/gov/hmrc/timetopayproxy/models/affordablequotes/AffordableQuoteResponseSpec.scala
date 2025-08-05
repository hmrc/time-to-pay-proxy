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

package uk.gov.hmrc.timetopayproxy.models.affordablequotes

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsSuccess, JsValue, Json, Reads, Writes }
import uk.gov.hmrc.timetopayproxy.models.{ Collections, DebtItemChargeId, Duration, InitialCollection, RegularCollection }
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps._
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

import java.time.{ LocalDate, LocalDateTime }

final class AffordableQuoteResponseSpec extends AnyFreeSpec {

  "AffordableQuoteResponse" - {
    object TestData {
      object WithOnlySomes {
        def obj: AffordableQuoteResponse = AffordableQuoteResponse(
          processingDateTime = LocalDateTime.parse("2023-10-01T12:00:00"),
          paymentPlans = List(
            AffordableQuotePaymentPlan(
              numberOfInstalments = 3,
              planDuration = Duration(6),
              planInterest = BigDecimal(50.00),
              totalDebt = BigInt(1000),
              totalDebtIncInt = BigDecimal(1050.00),
              collections = Collections(
                initialCollection = Some(
                  InitialCollection(
                    dueDate = LocalDate.parse("2023-10-01"),
                    amountDue = BigDecimal(350.00)
                  )
                ),
                regularCollections = List(
                  RegularCollection(
                    dueDate = LocalDate.parse("2023-11-01"),
                    amountDue = BigDecimal(350.00)
                  ),
                  RegularCollection(
                    dueDate = LocalDate.parse("2023-12-01"),
                    amountDue = BigDecimal(350.00)
                  )
                )
              ),
              instalments = List(
                AffordableQuoteInstalment(
                  debtItemChargeId = DebtItemChargeId("XW123456789012"),
                  dueDate = LocalDate.parse("2023-10-01"),
                  amountDue = BigDecimal(350.00),
                  instalmentNumber = 1,
                  instalmentInterestAccrued = BigDecimal(10.00),
                  instalmentBalance = BigDecimal(350.00),
                  debtItemOriginalDueDate = LocalDate.parse("2023-09-30"),
                  expectedPayment = BigInt(350)
                )
              )
            )
          )
        )

        def json: JsValue = Json.parse(
          """{
            |  "paymentPlans" : [
            |    {
            |      "collections" : {
            |        "initialCollection" : {
            |          "amountDue" : 350,
            |          "dueDate" : "2023-10-01"
            |        },
            |        "regularCollections" : [
            |          {
            |            "amountDue" : 350,
            |            "dueDate" : "2023-11-01"
            |          },
            |          {
            |            "amountDue" : 350,
            |            "dueDate" : "2023-12-01"
            |          }
            |        ]
            |      },
            |      "instalments" : [
            |        {
            |          "amountDue" : 350,
            |          "debtItemChargeId" : "XW123456789012",
            |          "debtItemOriginalDueDate" : "2023-09-30",
            |          "dueDate" : "2023-10-01",
            |          "expectedPayment" : 350,
            |          "instalmentBalance" : 350,
            |          "instalmentInterestAccrued" : 10,
            |          "instalmentNumber" : 1
            |        }
            |      ],
            |      "numberOfInstalments" : 3,
            |      "planDuration" : 6,
            |      "planInterest" : 50,
            |      "totalDebt" : 1000,
            |      "totalDebtIncInt" : 1050
            |    }
            |  ],
            |  "processingDateTime" : "2023-10-01T12:00:00"
            |}
            |""".stripMargin
        )
      }

      object With0Somes {
        def obj: AffordableQuoteResponse = AffordableQuoteResponse(
          processingDateTime = LocalDateTime.parse("2023-10-01T12:00:00"),
          paymentPlans = List(
            AffordableQuotePaymentPlan(
              numberOfInstalments = 3,
              planDuration = Duration(6),
              planInterest = BigDecimal(50.00),
              totalDebt = BigInt(1000),
              totalDebtIncInt = BigDecimal(1050.00),
              collections = Collections(
                initialCollection = None,
                regularCollections = List(
                  RegularCollection(
                    dueDate = LocalDate.parse("2023-11-01"),
                    amountDue = BigDecimal(350.00)
                  ),
                  RegularCollection(
                    dueDate = LocalDate.parse("2023-12-01"),
                    amountDue = BigDecimal(350.00)
                  )
                )
              ),
              instalments = List(
                AffordableQuoteInstalment(
                  debtItemChargeId = DebtItemChargeId("XW123456789012"),
                  dueDate = LocalDate.parse("2023-10-01"),
                  amountDue = BigDecimal(350.00),
                  instalmentNumber = 1,
                  instalmentInterestAccrued = BigDecimal(10.00),
                  instalmentBalance = BigDecimal(350.00),
                  debtItemOriginalDueDate = LocalDate.parse("2023-09-30"),
                  expectedPayment = BigInt(350)
                )
              )
            )
          )
        )

        def json: JsValue = Json.parse(
          """{
            |  "paymentPlans" : [
            |    {
            |      "collections" : {
            |        "regularCollections" : [
            |          {
            |            "amountDue" : 350,
            |            "dueDate" : "2023-11-01"
            |          },
            |          {
            |            "amountDue" : 350,
            |            "dueDate" : "2023-12-01"
            |          }
            |        ]
            |      },
            |      "instalments" : [
            |        {
            |          "amountDue" : 350,
            |          "debtItemChargeId" : "XW123456789012",
            |          "debtItemOriginalDueDate" : "2023-09-30",
            |          "dueDate" : "2023-10-01",
            |          "expectedPayment" : 350,
            |          "instalmentBalance" : 350,
            |          "instalmentInterestAccrued" : 10,
            |          "instalmentNumber" : 1
            |        }
            |      ],
            |      "numberOfInstalments" : 3,
            |      "planDuration" : 6,
            |      "planInterest" : 50,
            |      "totalDebt" : 1000,
            |      "totalDebtIncInt" : 1050
            |    }
            |  ],
            |  "processingDateTime" : "2023-10-01T12:00:00"
            |}
            |""".stripMargin
        )
      }
    }

    "implicit JSON writer (data going to our clients)" - {
      def writerToClients: Writes[AffordableQuoteResponse] = implicitly[Writes[AffordableQuoteResponse]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json

        def obj: AffordableQuoteResponse = TestData.WithOnlySomes.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.AffordableQuotes.openApiResponseSuccessfulSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0Somes.json
        def obj: AffordableQuoteResponse = TestData.With0Somes.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.AffordableQuotes.openApiResponseSuccessfulSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }
    }

    "implicit JSON reader (data coming from time-to-pay)" - {
      def readerFromTtp: Reads[AffordableQuoteResponse] = implicitly[Reads[AffordableQuoteResponse]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: AffordableQuoteResponse = TestData.WithOnlySomes.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        // One could also check that the reader was given JSON compatible with the time-to-pay schema, if it's ever added.
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0Somes.json
        def obj: AffordableQuoteResponse = TestData.With0Somes.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        // One could also check that the reader was given JSON compatible with the time-to-pay schema, if it's ever added.
      }
    }
  }

}
