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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.{ JsSuccess, JsValue, Json, Reads, Writes }
import uk.gov.hmrc.timetopayproxy.models.saonly.common.ProcessingDateTimeInstant
import uk.gov.hmrc.timetopayproxy.models.saonly.common.apistatus.{ ApiErrorResponse, ApiName, ApiStatus, ApiStatusCode }
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps._
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators.TimeToPayProxy.TtpCancel.Live

import java.time.Instant

final class TtpCancelInformativeErrorSpec extends AnyFreeSpec {

  object TestData {
    object WithOnlySomes {
      def obj: TtpCancelInformativeError = TtpCancelInformativeError(
        apisCalled = Some(
          List(
            ApiStatus(
              name = ApiName("api name"),
              statusCode = ApiStatusCode(400),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2000-01-02T14:35:00.788998Z")),
              errorResponse = Some(ApiErrorResponse("api error response"))
            )
          )
        ),
        internalErrors = List(
          TtpCancelInternalError("some error that ttp is responsible for"),
          TtpCancelInternalError("another error that ttp is responsible for")
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
          |      "statusCode" : 400
          |    }
          |  ],
          |  "internalErrors": [
          |     {"message": "some error that ttp is responsible for"},
          |     {"message": "another error that ttp is responsible for"}
          |  ],
          |  "processingDateTime" : "2222-02-24T14:35:00.788998Z"
          |}
          |""".stripMargin
      )
    }

    object With0SomeOnEachPath {
      def obj: TtpCancelInformativeError = TtpCancelInformativeError(
        apisCalled = Some(
          List(
            ApiStatus(
              name = ApiName("api name"),
              statusCode = ApiStatusCode(400),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2000-01-02T14:35:00.788998Z")),
              errorResponse = None
            )
          )
        ),
        internalErrors = List(
          TtpCancelInternalError("some error that ttp is responsible for"),
          TtpCancelInternalError("another error that ttp is responsible for")
        ),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2222-02-24T14:35:00.788998Z"))
      )

      def json: JsValue = Json.parse(
        """{
          |  "apisCalled" : [
          |    {
          |      "name" : "api name",
          |      "processingDateTime" : "2000-01-02T14:35:00.788998Z",
          |      "statusCode" : 400
          |    }
          |  ],
          |  "internalErrors": [
          |     {"message": "some error that ttp is responsible for"},
          |     {"message": "another error that ttp is responsible for"}
          |  ],
          |  "processingDateTime" : "2222-02-24T14:35:00.788998Z"
          |}
          |""".stripMargin
      )
    }
  }

  "TtpCancelInformativeError" - {

    "implicit JSON writer (data going to our clients)" - {
      def writerToClients: Writes[TtpCancelInformativeError] = implicitly[Writes[TtpCancelInformativeError]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpCancelInformativeError = TestData.WithOnlySomes.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Live.openApiCancelErrorResponseSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: TtpCancelInformativeError = TestData.With0SomeOnEachPath.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Live.openApiCancelErrorResponseSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          schema.validateAndGetErrors(writtenJson) shouldBe Nil
        }
      }
    }

    "implicit JSON reader (data coming from time-to-pay)" - {
      def readerFromTtp: Reads[TtpCancelInformativeError] = implicitly[Reads[TtpCancelInformativeError]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpCancelInformativeError = TestData.WithOnlySomes.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpCancel.Live.openApiCancelErrorResponseSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: TtpCancelInformativeError = TestData.With0SomeOnEachPath.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpCancel.Live.openApiCancelErrorResponseSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }
    }
  }
}
