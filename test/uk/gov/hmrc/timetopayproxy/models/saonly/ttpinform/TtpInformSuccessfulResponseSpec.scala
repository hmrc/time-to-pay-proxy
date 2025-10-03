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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json._
import uk.gov.hmrc.timetopayproxy.models.saonly.common.ProcessingDateTimeInstant
import uk.gov.hmrc.timetopayproxy.models.saonly.common.apistatus.{ApiErrorResponse, ApiName, ApiStatus, ApiStatusCode}
import uk.gov.hmrc.timetopayproxy.testutils.JsonAssertionOps._
import uk.gov.hmrc.timetopayproxy.testutils.schematestutils.Validators

import java.time.Instant

final class TtpInformSuccessfulResponseSpec extends AnyFreeSpec {

  object TestData {
    object WithOnlySomes {
      def obj: TtpInformSuccessfulResponse = TtpInformSuccessfulResponse(
        apisCalled = List(
          ApiStatus(
            name = ApiName("api name"),
            statusCode = ApiStatusCode("400"),
            processingDateTime = ProcessingDateTimeInstant(Instant.parse("2000-01-02T14:35:00.788998Z")),
            errorResponse = Some(ApiErrorResponse("api error response"))
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
          |  "processingDateTime" : "2222-02-24T14:35:00.788998Z"
          |}
          |""".stripMargin
      )
    }

    object With0SomeOnEachPath {
      def obj: TtpInformSuccessfulResponse = TtpInformSuccessfulResponse(
        apisCalled = List(
          ApiStatus(
            name = ApiName("api name"),
            statusCode = ApiStatusCode("400"),
            processingDateTime = ProcessingDateTimeInstant(Instant.parse("2000-01-02T14:35:00.788998Z")),
            errorResponse = None
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
          |  "processingDateTime" : "2222-02-24T14:35:00.788998Z"
          |}
          |""".stripMargin
      )
    }
  }

  "TtpInformSuccessfulResponse" - {

    "implicit JSON writer (data going to our clients)" - {
      def writerToClients: Writes[TtpInformSuccessfulResponse] = implicitly[Writes[TtpInformSuccessfulResponse]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpInformSuccessfulResponse = TestData.WithOnlySomes.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpInform.openApiInformativeResponseSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          // TODO DTD-3779: These errors should be resolved.
          schema.validateAndGetErrors(writtenJson) shouldBe List(
            """apisCalled.0.statusCode: Type expected 'integer', found 'string'. (code: 1027)
              |From: apisCalled.0.<items>.<#/components/schemas/InformAPIStatus>.statusCode.<type>""".stripMargin
          )
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: TtpInformSuccessfulResponse = TestData.With0SomeOnEachPath.obj

        "writes the correct JSON" in {
          writerToClients.writes(obj) shouldBeEquivalentTo json
        }

        "writes JSON compatible with our schema" in {
          val schema = Validators.TimeToPayProxy.TtpInform.openApiInformativeResponseSchema
          val writtenJson: JsValue = writerToClients.writes(obj)

          // TODO DTD-3779: These errors should be resolved.
          schema.validateAndGetErrors(writtenJson) shouldBe List(
            """apisCalled.0.statusCode: Type expected 'integer', found 'string'. (code: 1027)
              |From: apisCalled.0.<items>.<#/components/schemas/InformAPIStatus>.statusCode.<type>""".stripMargin
          )
        }
      }
    }

    "implicit JSON reader (data coming from time-to-pay)" - {
      def readerFromTtp: Reads[TtpInformSuccessfulResponse] = implicitly[Reads[TtpInformSuccessfulResponse]]

      "when all the optional fields are fully populated" - {
        def json: JsValue = TestData.WithOnlySomes.json
        def obj: TtpInformSuccessfulResponse = TestData.WithOnlySomes.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpInform.openApiInformativeResponseSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }

      "when none of the optional fields are populated" - {
        def json: JsValue = TestData.With0SomeOnEachPath.json
        def obj: TtpInformSuccessfulResponse = TestData.With0SomeOnEachPath.obj

        "reads the JSON correctly" in {
          readerFromTtp.reads(json) shouldBe JsSuccess(obj)
        }

        "was tested against JSON compatible with the time-to-pay schema" in {
          val schema = Validators.TimeToPay.TtpInform.openApiInformativeResponseSchema

          schema.validateAndGetErrors(json) shouldBe Nil
        }
      }
    }
  }
}
