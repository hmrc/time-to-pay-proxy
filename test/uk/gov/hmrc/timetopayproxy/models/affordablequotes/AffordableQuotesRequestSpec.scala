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
import uk.gov.hmrc.timetopayproxy.models.{ CustomerPostCode, DebtItemChargeId, DebtItemChargeSelfServe, Duration, FrequencyCapitalised, IsInterestBearingCharge, PostCode, SsttpRegimeType, UseChargeReference }
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps._
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

import java.time.LocalDate

final class AffordableQuotesRequestSpec extends AnyFreeSpec {
  "AffordableQuotesRequest" - {

    object TestData {
      object WithOnlySomes {
        def obj: AffordableQuotesRequest =
          AffordableQuotesRequest(
            channelIdentifier = "eSSTTP",
            paymentPlanAffordableAmount = 1310,
            paymentPlanFrequency = FrequencyCapitalised.Single,
            paymentPlanMinLength = Duration(1),
            paymentPlanMaxLength = Duration(6),
            accruedDebtInterest = 13.26,
            paymentPlanStartDate = LocalDate.parse("2022-07-08"),
            initialPaymentDate = Some(LocalDate.parse("2022-06-18")),
            initialPaymentAmount = Some(BigInt(1650)),
            debtItemCharges = List(
              DebtItemChargeSelfServe(
                outstandingDebtAmount = BigDecimal(1487.81),
                mainTrans = "5330",
                subTrans = "1090",
                debtItemChargeId = DebtItemChargeId("XW006559808862"),
                interestStartDate = Some(LocalDate.parse("2022-05-21")),
                debtItemOriginalDueDate = LocalDate.parse("2022-05-22"),
                isInterestBearingCharge = IsInterestBearingCharge(true),
                useChargeReference = UseChargeReference(false)
              )
            ),
            customerPostcodes = List(
              CustomerPostCode(PostCode("BN127ER"), postcodeDate = LocalDate.parse("2022-05-22"))
            ),
            regimeType = Some(SsttpRegimeType.VATC)
          )

        def json: JsValue = Json.parse(
          """{
            |  "accruedDebtInterest" : 13.26,
            |  "channelIdentifier" : "eSSTTP",
            |  "customerPostcodes" : [
            |    {
            |      "addressPostcode" : "BN127ER",
            |      "postcodeDate" : "2022-05-22"
            |    }
            |  ],
            |  "debtItemCharges" : [
            |    {
            |      "debtItemChargeId" : "XW006559808862",
            |      "debtItemOriginalDueDate" : "2022-05-22",
            |      "interestStartDate" : "2022-05-21",
            |      "isInterestBearingCharge" : true,
            |      "mainTrans" : "5330",
            |      "outstandingDebtAmount" : 1487.81,
            |      "subTrans" : "1090",
            |      "useChargeReference" : false
            |    }
            |  ],
            |  "initialPaymentAmount" : 1650,
            |  "initialPaymentDate" : "2022-06-18",
            |  "paymentPlanAffordableAmount" : 1310,
            |  "paymentPlanFrequency" : "Single",
            |  "paymentPlanMaxLength" : 6,
            |  "paymentPlanMinLength" : 1,
            |  "paymentPlanStartDate" : "2022-07-08",
            |  "regimeType" : "VATC"
            |}
            |""".stripMargin
        )
      }

      object With0Somes {
        def obj: AffordableQuotesRequest =
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
            customerPostcodes = List(
              CustomerPostCode(PostCode("BN127ER"), postcodeDate = LocalDate.parse("2022-05-22"))
            ),
            regimeType = None
          )

        def json: JsValue = Json.parse(
          """{
            |  "accruedDebtInterest" : 13.26,
            |  "channelIdentifier" : "eSSTTP",
            |  "customerPostcodes" : [
            |    {
            |      "addressPostcode" : "BN127ER",
            |      "postcodeDate" : "2022-05-22"
            |    }
            |  ],
            |  "debtItemCharges" : [
            |    {
            |      "debtItemChargeId" : "XW006559808862",
            |      "debtItemOriginalDueDate" : "2022-05-22",
            |      "isInterestBearingCharge" : true,
            |      "mainTrans" : "5330",
            |      "outstandingDebtAmount" : 1487.81,
            |      "subTrans" : "1090",
            |      "useChargeReference" : false
            |    }
            |  ],
            |  "paymentPlanAffordableAmount" : 1310,
            |  "paymentPlanFrequency" : "Single",
            |  "paymentPlanMaxLength" : 6,
            |  "paymentPlanMinLength" : 1,
            |  "paymentPlanStartDate" : "2022-07-08"
            |}
            |""".stripMargin
        )
      }

    }

    "implicit JSON writer (data going to time-to-pay)" - {
      def writerToTtp: Writes[AffordableQuotesRequest] = implicitly[Writes[AffordableQuotesRequest]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: AffordableQuotesRequest = TestData.WithOnlySomes.obj

        "writes the correct JSON" in {
          writerToTtp.writes(obj) shouldBeEquivalentTo json
        }

        // One could also check that the writer is compatible with the time-to-pay schema, if it's ever added.
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0Somes.json
        def obj: AffordableQuotesRequest = TestData.With0Somes.obj

        "writes the correct JSON" in {
          writerToTtp.writes(obj) shouldBeEquivalentTo json
        }

        // One could also check that the writer is compatible with the time-to-pay schema, if it's ever added.
      }
    }

    "implicit JSON reader (data coming from our clients)" - {
      def readerFromClients: Reads[AffordableQuotesRequest] = implicitly[Reads[AffordableQuotesRequest]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: AffordableQuotesRequest = TestData.WithOnlySomes.obj

        "reads the JSON correctly" in {
          readerFromClients.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.AffordableQuotes.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0Somes.json
        def obj: AffordableQuotesRequest = TestData.With0Somes.obj

        "reads the JSON correctly" in {
          readerFromClients.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.AffordableQuotes.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }
    }

  }
}
