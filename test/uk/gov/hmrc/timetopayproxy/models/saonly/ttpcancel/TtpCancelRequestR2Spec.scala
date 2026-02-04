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

package uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel

import cats.data.NonEmptyList
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json._
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators.TimeToPayProxy.TtpCancel.Proposed

import java.time.LocalDate

// TODO DTD-4258: This R2 class should be removed when the R2 feature switch is ready to be removed
class TtpCancelRequestR2Spec extends AnyFreeSpec {
  "TtpCancelRequestR2" - {
    object TestData {
      object WithAllFieldsPopulated {
        def obj: TtpCancelRequestR2 = TtpCancelRequestR2(
          identifications = NonEmptyList.of(
            Identification(
              idType = IdType("idtype"),
              idValue = IdValue("idvalue")
            )
          ),
          paymentPlan = TtpCancelPaymentPlanR2(
            arrangementAgreedDate = Some(ArrangementAgreedDate(LocalDate.parse("2020-01-02"))),
            ttpEndDate = Some(TtpEndDate(LocalDate.parse("2020-02-04"))),
            frequency = Some(FrequencyLowercase.Weekly),
            cancellationDate = CancellationDate(LocalDate.parse("2020-03-05")),
            initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2020-04-06"))),
            initialPaymentAmount = Some(GbpPounds.createOrThrow(100.12)),
            debtItemCharges = NonEmptyList.of(
              DebtItemChargeReference(
                debtItemChargeId = DebtItemChargeId("K0000000000001"),
                chargeSource = ChargeSourceSAOnly.CESA
              )
            )
          ),
          instalments = NonEmptyList.of(
            SaOnlyInstalment(
              dueDate = InstalmentDueDate(LocalDate.parse("2020-05-07")),
              amountDue = GbpPounds.createOrThrow(200.34)
            )
          ),
          channelIdentifier = ChannelIdentifier.SelfService,
          transitioned = Some(TransitionedIndicator(true))
        )

        def json: JsValue = Json.parse(
          """{
            |  "channelIdentifier" : "selfService",
            |  "identifications" : [
            |    {
            |      "idType" : "idtype",
            |      "idValue" : "idvalue"
            |    }
            |  ],
            |  "instalments" : [
            |    {
            |      "amountDue" : 200.34,
            |      "dueDate" : "2020-05-07"
            |    }
            |  ],
            |  "paymentPlan" : {
            |    "arrangementAgreedDate" : "2020-01-02",
            |    "cancellationDate" : "2020-03-05",
            |    "frequency" : "weekly",
            |    "initialPaymentAmount" : 100.12,
            |    "initialPaymentDate" : "2020-04-06",
            |    "ttpEndDate" : "2020-02-04",
            |    "debtItemCharges": [
            |      {
            |        "debtItemChargeId": "K0000000000001",
            |        "chargeSource": "CESA"
            |      }
            |    ]
            |  },
            |  "transitioned" : true
            |}
            |""".stripMargin
        )
      }

      object WithAllOptionalFieldsAsNone {
        def obj: TtpCancelRequestR2 = TtpCancelRequestR2(
          identifications = NonEmptyList.of(
            Identification(
              idType = IdType("idtype"),
              idValue = IdValue("idvalue")
            )
          ),
          paymentPlan = TtpCancelPaymentPlanR2(
            arrangementAgreedDate = None,
            ttpEndDate = None,
            frequency = None,
            cancellationDate = CancellationDate(LocalDate.parse("2020-03-05")),
            initialPaymentDate = None,
            initialPaymentAmount = None,
            debtItemCharges = NonEmptyList.of(
              DebtItemChargeReference(
                debtItemChargeId = DebtItemChargeId("K0000000000001"),
                chargeSource = ChargeSourceSAOnly.CESA
              )
            )
          ),
          instalments = NonEmptyList.of(
            SaOnlyInstalment(
              dueDate = InstalmentDueDate(LocalDate.parse("2020-05-07")),
              amountDue = GbpPounds.createOrThrow(200.34)
            )
          ),
          channelIdentifier = ChannelIdentifier.SelfService,
          transitioned = None
        )

        def json: JsValue = Json.parse(
          """{
            |  "channelIdentifier" : "selfService",
            |  "identifications" : [
            |    {
            |      "idType" : "idtype",
            |      "idValue" : "idvalue"
            |    }
            |  ],
            |  "instalments" : [
            |    {
            |      "amountDue" : 200.34,
            |      "dueDate" : "2020-05-07"
            |    }
            |  ],
            |  "paymentPlan" : {
            |    "cancellationDate" : "2020-03-05",
            |    "debtItemCharges": [
            |      {
            |        "debtItemChargeId": "K0000000000001",
            |        "chargeSource": "CESA"
            |      }
            |    ]
            |  }
            |}
            |""".stripMargin
        )
      }

      object WithOnlySomesAndAllCurrencyAmountsInvalid {
        def json: JsValue = Json.parse(
          """{
            |  "channelIdentifier" : "selfService",
            |  "identifications" : [
            |    {
            |      "idType" : "idtype",
            |      "idValue" : "idvalue"
            |    }
            |  ],
            |  "instalments" : [
            |    {
            |      "amountDue" : 200.349,
            |      "dueDate" : "2020-05-07"
            |    }
            |  ],
            |  "paymentPlan" : {
            |    "arrangementAgreedDate" : "2020-01-02",
            |    "cancellationDate" : "2020-03-05",
            |    "frequency" : "weekly",
            |    "initialPaymentAmount" : 100.129,
            |    "initialPaymentDate" : "2020-04-06",
            |    "ttpEndDate" : "2020-02-04",
            |    "debtItemCharges": [
            |      {
            |        "debtItemChargeId": "K0000000000001",
            |        "chargeSource": "CESA"
            |      }
            |    ]
            |  },
            |  "transitioned" : true
            |}
            |""".stripMargin
        )
      }
    }

    "implicit JSON reader (data coming from our clients)" - {
      def readerFromClients: Reads[TtpCancelRequestR2] = implicitly[Reads[TtpCancelRequestR2]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithAllFieldsPopulated.json
        def obj: TtpCancelRequestR2 = TestData.WithAllFieldsPopulated.obj

        "reads the JSON correctly" in {
          readerFromClients.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with our schema" in {
          val schema = Proposed.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.WithAllOptionalFieldsAsNone.json
        def obj: TtpCancelRequestR2 = TestData.WithAllOptionalFieldsAsNone.obj

        "reads the JSON correctly" in {
          readerFromClients.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with our schema" in {
          val schema = Proposed.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }

      "when all the optional fields are fully populated AND all the currency amounts are invalid" - {
        def json: JsValue = TestData.WithOnlySomesAndAllCurrencyAmountsInvalid.json

        "cannot read the JSON due to decimal precision issues" in {
          readerFromClients.reads(json) shouldBe
            JsError(
              List(
                (
                  JsPath \ "instalments" \ 0 \ "amountDue",
                  List(
                    JsonValidationError(
                      List("Number of digits after decimal point should not exceed 2: 200.349")
                    )
                  )
                ),
                (
                  JsPath \ "paymentPlan" \ "initialPaymentAmount",
                  List(
                    JsonValidationError(
                      List("Number of digits after decimal point should not exceed 2: 100.129")
                    )
                  )
                )
              )
            )
        }

        "was tested against JSON incompatible with our schema" in {
          val schema = Proposed.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe List(
            """instalments.0.amountDue: Value '200.349' is not a multiple of '0.01'. (code: 1019)
              |From: instalments.0.<items>.<#/components/schemas/CancelInstalment>.amountDue.<multipleOf>""".stripMargin,
            """paymentPlan.initialPaymentAmount: Value '100.129' is not a multiple of '0.01'. (code: 1019)
              |From: paymentPlan.<#/components/schemas/CancelPaymentPlan>.initialPaymentAmount.<multipleOf>""".stripMargin
          )
        }
      }

    }

  }

}
