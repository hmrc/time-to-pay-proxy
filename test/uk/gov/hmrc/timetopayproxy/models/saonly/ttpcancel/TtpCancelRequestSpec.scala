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

package uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel

import cats.data.NonEmptyList
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsSuccess, JsValue, Json, Reads, Writes }
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPoundsUnchecked
import uk.gov.hmrc.timetopayproxy.models.saonly.common.{ ArrangementAgreedDate, InitialPaymentDate, SaOnlyInstalment, TransitionedIndicator, TtpEndDate }
import uk.gov.hmrc.timetopayproxy.models.{ ChannelIdentifier, FrequencyLowercase, IdType, IdValue, Identification, InstalmentDueDate }
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps._
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

import java.time.LocalDate

final class TtpCancelRequestSpec extends AnyFreeSpec {

  "TtpCancelRequest" - {
    object TestData {
      object WithOnlySomes {
        def obj: TtpCancelRequest = TtpCancelRequest(
          identifications = NonEmptyList.of(
            Identification(
              idType = IdType("idtype"),
              idValue = IdValue("idvalue")
            )
          ),
          paymentPlan = TtpCancelPaymentPlan(
            arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2020-01-02")),
            ttpEndDate = TtpEndDate(LocalDate.parse("2020-02-04")),
            frequency = FrequencyLowercase.Weekly,
            cancellationDate = CancellationDate(LocalDate.parse("2020-03-05")),
            initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2020-04-06"))),
            initialPaymentAmount = Some(GbpPoundsUnchecked(100.12))
          ),
          instalments = NonEmptyList.of(
            SaOnlyInstalment(
              dueDate = InstalmentDueDate(LocalDate.parse("2020-05-07")),
              amountDue = GbpPoundsUnchecked(200.34)
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
            |    "ttpEndDate" : "2020-02-04"
            |  },
            |  "transitioned" : true
            |}
            |""".stripMargin
        )
      }

      object With0SomeOnEachPath {
        def obj: TtpCancelRequest = TtpCancelRequest(
          identifications = NonEmptyList.of(
            Identification(
              idType = IdType("idtype"),
              idValue = IdValue("idvalue")
            )
          ),
          paymentPlan = TtpCancelPaymentPlan(
            arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2020-01-02")),
            ttpEndDate = TtpEndDate(LocalDate.parse("2020-02-04")),
            frequency = FrequencyLowercase.Weekly,
            cancellationDate = CancellationDate(LocalDate.parse("2020-03-05")),
            initialPaymentDate = None,
            initialPaymentAmount = None
          ),
          instalments = NonEmptyList.of(
            SaOnlyInstalment(
              dueDate = InstalmentDueDate(LocalDate.parse("2020-05-07")),
              amountDue = GbpPoundsUnchecked(200.34)
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
            |    "cancellationDate" : "2020-03-05",
            |    "frequency" : "weekly",
            |    "ttpEndDate" : "2020-02-04"
            |  }
            |}
            |""".stripMargin
        )
      }

    }

    "implicit JSON writer (data going to time-to-pay)" - {
      def writerToTtp: Writes[TtpCancelRequest] = implicitly[Writes[TtpCancelRequest]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpCancelRequest = TestData.WithOnlySomes.obj

        "writes the correct JSON" in {
          writerToTtp.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpCancel.openApiRequestSchema
          val writtenJson: JsValue = writerToTtp.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: TtpCancelRequest = TestData.With0SomeOnEachPath.obj

        "writes the correct JSON" in {
          writerToTtp.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpCancel.openApiRequestSchema
          val writtenJson: JsValue = writerToTtp.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }
    }

    "implicit JSON reader (data coming from our clients)" - {
      def readerFromClients: Reads[TtpCancelRequest] = implicitly[Reads[TtpCancelRequest]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpCancelRequest = TestData.WithOnlySomes.obj

        "reads the JSON correctly" in {
          readerFromClients.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpCancel.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: TtpCancelRequest = TestData.With0SomeOnEachPath.obj

        "reads the JSON correctly" in {
          readerFromClients.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpCancel.openApiRequestSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }
    }

  }
}
