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

package uk.gov.hmrc.timetopayproxy.models.saopled.ttpcancel

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.{ JsSuccess, JsValue, Json, Reads, Writes }
import uk.gov.hmrc.timetopayproxy.models.saopled.common.ProcessingDateTimeInstant
import uk.gov.hmrc.timetopayproxy.models.saopled.common.apistatus.{ ApiErrorResponse, ApiName, ApiStatus, ApiStatusCode }
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps.RichJsValueWithAssertions
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

import java.time.Instant

final class TtpCancelErrorInformativeResponseSpec extends AnyFreeSpec {
  object TestData {
    object WithOnlySomes {
      def obj: TtpCancelErrorInformativeResponse = TtpCancelErrorInformativeResponse(
        apisCalled = Some(
          List(
            ApiStatus(
              name = ApiName("api name"),
              statusCode = ApiStatusCode("400"),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2000-01-02T14:35:00.788998Z")),
              errorResponse = Some(ApiErrorResponse("api error response"))
            )
          )
        ),
        internalErrors = Some(
          List(
            TtpCancelInternalError(message = "internal error message")
          )
        ),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2222-02-24T14:35:00.788998Z"))
      )

      def json: JsValue = Json.parse(
        """{
          |  "apisCalled" : [
          |    {
          |      "errorResponse" : "api error response",
          |      "name" : "api name",
          |      "processingDateTime" : "2000-01-02T14:35:00.788998Z",
          |      "statusCode" : "400"
          |    }
          |  ],
          |  "internalErrors" : [
          |    {
          |      "message" : "internal error message"
          |    }
          |  ],
          |  "processingDateTime" : "2222-02-24T14:35:00.788998Z"
          |}
          |""".stripMargin
      )
    }

    object With1SomeOnEachPath {
      def obj: TtpCancelErrorInformativeResponse = TtpCancelErrorInformativeResponse(
        apisCalled = Some(
          List(
            ApiStatus(
              name = ApiName("api name"),
              statusCode = ApiStatusCode("400"),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2000-01-02T14:35:00.788998Z")),
              errorResponse = None
            )
          )
        ),
        internalErrors = Some(
          List(
            TtpCancelInternalError(message = "internal error message")
          )
        ),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2222-02-24T14:35:00.788998Z"))
      )

      def json: JsValue = Json.parse(
        """{
          |  "apisCalled" : [
          |    {
          |      "name" : "api name",
          |      "processingDateTime" : "2000-01-02T14:35:00.788998Z",
          |      "statusCode" : "400"
          |    }
          |  ],
          |  "internalErrors" : [
          |    {
          |      "message" : "internal error message"
          |    }
          |  ],
          |  "processingDateTime" : "2222-02-24T14:35:00.788998Z"
          |}
          |""".stripMargin
      )
    }

    object With0SomesOnEachPath {
      def obj: TtpCancelErrorInformativeResponse = TtpCancelErrorInformativeResponse(
        apisCalled = None,
        internalErrors = None,
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2222-02-24T14:35:00.788998Z"))
      )

      def json: JsValue = Json.parse(
        """{
          |  "processingDateTime" : "2222-02-24T14:35:00.788998Z"
          |}
          |""".stripMargin
      )
    }
  }

  "TtpCancelErrorInformativeResponse" - {

    "implicit JSON writer (data going to our clients)" - {
      def writerToClients: Writes[TtpCancelErrorInformativeResponse] =
        implicitly[Writes[TtpCancelErrorInformativeResponse]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpCancelErrorInformativeResponse = TestData.WithOnlySomes.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpCancel.openApiResponseInformativeErrorSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }

      "when each path only has 1 optional field" - {
        def json: JsValue = TestData.With1SomeOnEachPath.json
        def obj: TtpCancelErrorInformativeResponse = TestData.With1SomeOnEachPath.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpCancel.openApiResponseInformativeErrorSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomesOnEachPath.json
        def obj: TtpCancelErrorInformativeResponse = TestData.With0SomesOnEachPath.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpCancel.openApiResponseInformativeErrorSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }
    }

    "implicit JSON reader (data coming from time-to-pay)" - {
      def readerFromTtp: Reads[TtpCancelErrorInformativeResponse] = implicitly[Reads[TtpCancelErrorInformativeResponse]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpCancelErrorInformativeResponse = TestData.WithOnlySomes.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpCancel.openApiInformativeResponseSchema

          schema.validateAndGetErrors(json) shouldBe List(
            // TODO: Fix the behaviour of time-to-pay
            """Additional property 'internalErrors' is not allowed. (code: 1000)
              |From: <additionalProperties>""".stripMargin
          )
        }
      }

      "when each path only has 1 optional field" - {
        def json: JsValue = TestData.With1SomeOnEachPath.json
        def obj: TtpCancelErrorInformativeResponse = TestData.With1SomeOnEachPath.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpCancel.openApiInformativeResponseSchema

          schema.validateAndGetErrors(json) shouldBe List(
            // TODO: Fix the behaviour of time-to-pay
            """Additional property 'internalErrors' is not allowed. (code: 1000)
              |From: <additionalProperties>""".stripMargin
          )
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomesOnEachPath.json
        def obj: TtpCancelErrorInformativeResponse = TestData.With0SomesOnEachPath.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpCancel.openApiInformativeResponseSchema

          schema.validateAndGetErrors(json) shouldBe List(
            // TODO: Fix the behaviour of time-to-pay
            """Field 'apisCalled' is required. (code: 1026)
              |From: <required>""".stripMargin
          )
        }
      }
    }
  }
}
