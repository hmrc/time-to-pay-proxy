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

package uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform

import cats.data.NonEmptyList
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json._
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps._
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

import java.time.LocalDate

final class TtpInformRequestSpec extends AnyFreeSpec {

  "TtpInformRequest" - {
    object TestData {
      object WithOnlySomes {
        def obj: TtpInformRequest = TtpInformRequest(
          identifications = NonEmptyList.of(
            Identification(
              idType = IdType("idtype"),
              idValue = IdValue("idvalue")
            )
          ),
          paymentPlan = SaOnlyPaymentPlan(
            arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2020-01-02")),
            ttpEndDate = TtpEndDate(LocalDate.parse("2020-02-04")),
            frequency = FrequencyLowercase.Weekly,
            initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2020-04-06"))),
            initialPaymentAmount = Some(GbpPounds.createOrThrow(100.12)),
            ddiReference = Some(DdiReference("TestDDIReference"))
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
            |    "frequency" : "weekly",
            |    "initialPaymentAmount" : 100.12,
            |    "initialPaymentDate" : "2020-04-06",
            |    "ttpEndDate" : "2020-02-04",
            |    "ddiReference" : "TestDDIReference"
            |  },
            |  "transitioned" : true
            |}
            |""".stripMargin
        )
      }

      object With0SomeOnEachPath {
        def obj: TtpInformRequest = TtpInformRequest(
          identifications = NonEmptyList.of(
            Identification(
              idType = IdType("idtype"),
              idValue = IdValue("idvalue")
            )
          ),
          paymentPlan = SaOnlyPaymentPlan(
            arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2020-01-02")),
            ttpEndDate = TtpEndDate(LocalDate.parse("2020-02-04")),
            frequency = FrequencyLowercase.Weekly,
            initialPaymentDate = None,
            initialPaymentAmount = None,
            ddiReference = None
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
            |    "arrangementAgreedDate" : "2020-01-02",
            |    "frequency" : "weekly",
            |    "ttpEndDate" : "2020-02-04"
            |  }
            |}
            |""".stripMargin
        )
      }

    }

    "implicit JSON writer (data going to time-to-pay)" - {
      def writerToTtp: Writes[TtpInformRequest] = implicitly[Writes[TtpInformRequest]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpInformRequest = TestData.WithOnlySomes.obj

        "writes the correct JSON" in {
          writerToTtp.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpInform.Live.openApiRequestSchema
          val writtenJson: JsValue = writerToTtp.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: TtpInformRequest = TestData.With0SomeOnEachPath.obj

        "writes the correct JSON" in {
          writerToTtp.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpInform.Live.openApiRequestSchema
          val writtenJson: JsValue = writerToTtp.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }
    }

    "implicit JSON reader (data coming from our clients)" - {
      def readerFromClients: Reads[TtpInformRequest] = implicitly[Reads[TtpInformRequest]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpInformRequest = TestData.WithOnlySomes.obj

        "reads the JSON correctly" in {
          readerFromClients.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpInform.Live.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: TtpInformRequest = TestData.With0SomeOnEachPath.obj

        "reads the JSON correctly" in {
          readerFromClients.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpInform.Live.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }
    }

  }

  "TtpInformRequestR2" - {
    object TestData {
      object WithOnlySomes {
        def obj: TtpInformRequestR2 = TtpInformRequestR2(
          identifications = NonEmptyList.of(
            Identification(
              idType = IdType("idtype"),
              idValue = IdValue("idvalue")
            )
          ),
          paymentPlan = SaOnlyPaymentPlanR2(
            arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2020-01-02")),
            ttpEndDate = TtpEndDate(LocalDate.parse("2020-02-04")),
            frequency = FrequencyLowercase.Weekly,
            initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2020-04-06"))),
            initialPaymentAmount = Some(GbpPounds.createOrThrow(100.12)),
            ddiReference = Some(DdiReference("TestDDIReference")),
            debtItemCharges =
              NonEmptyList.of(DebtItemChargeReference(DebtItemChargeId("ID-OF-DOOM"), ChargeSourceSAOnly.CESA))
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
            |    "frequency" : "weekly",
            |    "initialPaymentAmount" : 100.12,
            |    "initialPaymentDate" : "2020-04-06",
            |    "ttpEndDate" : "2020-02-04",
            |    "ddiReference" : "TestDDIReference",
            |    "debtItemCharges": [
            |     {
            |       "debtItemChargeId": "ID-OF-DOOM",
            |       "chargeSource": "CESA"
            |     }
            |    ]
            |  },
            |  "transitioned" : true
            |}
            |""".stripMargin
        )
      }

      object With0SomeOnEachPath {
        def obj: TtpInformRequestR2 = TtpInformRequestR2(
          identifications = NonEmptyList.of(
            Identification(
              idType = IdType("idtype"),
              idValue = IdValue("idvalue")
            )
          ),
          paymentPlan = SaOnlyPaymentPlanR2(
            arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2020-01-02")),
            ttpEndDate = TtpEndDate(LocalDate.parse("2020-02-04")),
            frequency = FrequencyLowercase.Weekly,
            initialPaymentDate = None,
            initialPaymentAmount = None,
            ddiReference = None,
            debtItemCharges =
              NonEmptyList.of(DebtItemChargeReference(DebtItemChargeId("ID-OF-DOOM"), ChargeSourceSAOnly.CESA))
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
            |    "arrangementAgreedDate" : "2020-01-02",
            |    "frequency" : "weekly",
            |    "ttpEndDate" : "2020-02-04",
            |    "debtItemCharges": [
            |     {
            |       "debtItemChargeId": "ID-OF-DOOM",
            |       "chargeSource": "CESA"
            |     }
            |    ]
            |  }
            |}
            |""".stripMargin
        )
      }

      object WithEmptyNel {
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
            |    "frequency" : "weekly",
            |    "ttpEndDate" : "2020-02-04",
            |    "debtItemCharges": []
            |  }
            |}
            |""".stripMargin
        )
      }
    }

    "implicit JSON writer (data going to time-to-pay)" - {
      def writerToTtp: Writes[TtpInformRequestR2] = implicitly[Writes[TtpInformRequestR2]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpInformRequestR2 = TestData.WithOnlySomes.obj

        "writes the correct JSON" in {
          writerToTtp.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpInform.Proposed.openApiRequestSchema
          val writtenJson: JsValue = writerToTtp.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: TtpInformRequestR2 = TestData.With0SomeOnEachPath.obj

        "writes the correct JSON" in {
          writerToTtp.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpInform.Proposed.openApiRequestSchema
          val writtenJson: JsValue = writerToTtp.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }
    }

    "implicit JSON reader (data coming from our clients)" - {
      def readerFromClients: Reads[TtpInformRequestR2] = implicitly[Reads[TtpInformRequestR2]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpInformRequestR2 = TestData.WithOnlySomes.obj

        "reads the JSON correctly" in {
          readerFromClients.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpInform.Proposed.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: TtpInformRequestR2 = TestData.With0SomeOnEachPath.obj

        "reads the JSON correctly" in {
          readerFromClients.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpInform.Proposed.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }

      "when non-empty lists are empty" - {

        "fails to read the JSON" in {
          def json: JsValue = TestData.WithEmptyNel.json

          assertThrows[JsResultException](json.as[TtpInformRequestR2])
        }
      }
    }

  }

}
