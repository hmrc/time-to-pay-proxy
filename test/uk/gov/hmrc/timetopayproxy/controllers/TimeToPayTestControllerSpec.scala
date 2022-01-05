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

package uk.gov.hmrc.timetopayproxy.controllers

import cats.implicits.catsSyntaxEitherId
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers.{CONTENT_TYPE, status}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.models.{ConnectorError, RequestDetails, TtppEnvelope}
import uk.gov.hmrc.timetopayproxy.services.TTPTestService
import uk.gov.hmrc.timetopayproxy.support.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class TimeToPayTestControllerSpec extends UnitSpec {
  private val cc: ControllerComponents = Helpers.stubControllerComponents()
  private val ttpTestService = mock[TTPTestService]

  private val controller =
    new TimeToPayTestController(cc, ttpTestService)

  val requestDetails = Seq(
    RequestDetails("someId", "content", Some("www.uri.com"), false),
    RequestDetails("someId", "content", Some("www.uri.com"), true)
  )

  val responseDetails = RequestDetails("someId", "content", Some("www.uri.com"), true)

  "GET /test-only/requests" should {
    "return a 200 given a successful response" in {

      (ttpTestService
        .retrieveRequestDetails()(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *)
        .returning(
          TtppEnvelope(requestDetails)
        )

      val fakeRequest = FakeRequest(
        "GET",
        "/test-only/requests"
      )
      val response: Future[Result] =
        controller.requests()(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "return a 404 if the quote is not found" in {
      val errorFromTtpConnector = ConnectorError(404, "Not Found")
      (ttpTestService
        .retrieveRequestDetails()(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *)
        .returning(
          TtppEnvelope(errorFromTtpConnector.asLeft[Seq[RequestDetails]])
        )

      val fakeRequest = FakeRequest(
        "GET",
        "/test-only/requests"
      )
      val response: Future[Result] =
        controller.requests()(fakeRequest)

      status(response) shouldBe Status.NOT_FOUND
    }

    "return 500 if the underlying service fails" in {
      val errorFromTtpConnector = ConnectorError(500, "Internal Server Error")

      (ttpTestService
        .retrieveRequestDetails()(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *)
        .returning(
          TtppEnvelope(errorFromTtpConnector.asLeft[Seq[RequestDetails]])
        )

      val fakeRequest = FakeRequest(
        "GET",
        "/test-only/requests"
      )
      val response: Future[Result] =
        controller.requests()(fakeRequest)

      status(response) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "POST /test-only/response" should {
    "return 200" when {
      "service returns success" in {

        (ttpTestService
          .saveResponseDetails(_: RequestDetails)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(responseDetails, *, *)
          .returning(
            TtppEnvelope(())
          )

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest("POST", "/test-only/response")
            .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson[RequestDetails](responseDetails))

        val response: Future[Result] = controller.response()(fakeRequest)
        status(response) shouldBe Status.OK
      }
    }

    "return 500" when {
      "service returns failure" in {
        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")

        (ttpTestService
          .saveResponseDetails(_: RequestDetails)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(responseDetails, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[Unit])
          )

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest("POST", "/test-only/response")
            .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson[RequestDetails](responseDetails))

        val response: Future[Result] = controller.response()(fakeRequest)

        status(response) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "GET /test-only/errors" should {
    "return a 200 given a successful response" in {

      (ttpTestService
        .getErrors()(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *)
        .returning(
          TtppEnvelope(requestDetails)
        )

      val fakeRequest = FakeRequest(
        "GET",
        "/test-only/errors"
      )
      val response: Future[Result] =
        controller.getErrors()(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "return a 404 if the quote is not found" in {
      val errorFromTtpConnector = ConnectorError(404, "Not Found")
      (ttpTestService
        .getErrors()(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *)
        .returning(
          TtppEnvelope(errorFromTtpConnector.asLeft[Seq[RequestDetails]])
        )

      val fakeRequest = FakeRequest(
        "GET",
        "/test-only/errors"
      )
      val response: Future[Result] =
        controller.getErrors()(fakeRequest)

      status(response) shouldBe Status.NOT_FOUND
    }

    "return 500 if the underlying service fails" in {
      val errorFromTtpConnector = ConnectorError(500, "Internal Server Error")

      (ttpTestService
        .getErrors()(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *)
        .returning(
          TtppEnvelope(errorFromTtpConnector.asLeft[Seq[RequestDetails]])
        )

      val fakeRequest = FakeRequest(
        "GET",
        "/test-only/errors"
      )
      val response: Future[Result] =
        controller.getErrors()(fakeRequest)

      status(response) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "POST /test-only/errors" should {
    "return 200" when {
      "service returns success" in {

        (ttpTestService
          .saveError(_: RequestDetails)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(responseDetails, *, *)
          .returning(
            TtppEnvelope(())
          )

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest("POST", "/test-only/errors")
            .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson[RequestDetails](responseDetails))

        val response: Future[Result] = controller.saveError()(fakeRequest)
        status(response) shouldBe Status.OK
      }
    }

    "return 500" when {
      "service returns failure" in {
        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")

        (ttpTestService
          .saveError(_: RequestDetails)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(responseDetails, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[Unit])
          )

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest("POST", "/test-only/errors")
            .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson[RequestDetails](responseDetails))

        val response: Future[Result] = controller.saveError()(fakeRequest)

        status(response) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "DELETE /test-only/request/:requestId" should {
    "return a 200 given a successful response" in {

      (ttpTestService
        .deleteRequestDetails(_: String)(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *, *)
        .returning(
          TtppEnvelope(Unit)
        )

      val fakeRequest = FakeRequest(
        "DELETE",
        "/test-only/request/1"
      )
      val response: Future[Result] =
        controller.deleteRequest("1")(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "return 500 if the underlying service fails" in {
      val errorFromTtpConnector = ConnectorError(500, "Internal Server Error")

      (ttpTestService
        .deleteRequestDetails(_: String)(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *, *)
        .returning(
          TtppEnvelope(errorFromTtpConnector.asLeft)
        )

      val fakeRequest = FakeRequest(
        "DELETE",
        "/test-only/request/2"
      )
      val response: Future[Result] =
        controller.deleteRequest("2")(fakeRequest)

      status(response) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

}
