/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.{JsSuccess, JsValue, Json}
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers.status
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.actions.auth.{
  AuthoriseAction,
  AuthoriseActionImpl
}
import uk.gov.hmrc.timetopayproxy.services.TTPQuoteService
import uk.gov.hmrc.timetopayproxy.models._
import play.api.test.{FakeRequest, Helpers}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._
import cats.syntax.either._

class TimeToPayProxyControllerSpec
    extends AnyWordSpec
    with Matchers
    with MockFactory {

  private val authConnector: PlayAuthConnector = mock[PlayAuthConnector]

  private val cc: ControllerComponents = Helpers.stubControllerComponents()
  private val authoriseAction: AuthoriseAction =
    new AuthoriseActionImpl(authConnector, cc)

  private val ttpQuoteService = mock[TTPQuoteService]
  private val controller =
    new TimeToPayProxyController(authoriseAction, cc, ttpQuoteService)

  private val generateQuoteRequest = GenerateQuoteRequest(
    "customerReference",
    10,
    List(
      Customer(
        QuoteType("quoteType"),
        LocalDate.of(2021, 1, 1),
        1,
        Frequency("some frequency"),
        Duration("some duration"),
        1,
        LocalDate.now(),
        PaymentPlanType("paymentPlanType")
      )
    ),
    List()
  )

  private val updateQuoteRequest =
    UpdateQuoteRequest(
      CustomerReference("customerReference"),
      PegaPlanId("pegaId"),
      UpdateType("updateType"),
      CancellationReason("reason"),
      PaymentMethod("method"),
      PaymentReference("reference"),
      true
    )

  private val createPlanRequest =
    CreatePlanRequest(
      CustomerReference("customerReference"),
      PegaPlanId("pegaPlanId"),
      "xyz",
      "paymentRed",
      false,
      Nil,
      "2",
      10000,
      0.26
    )

  "POST /individuals/time-to-pay/quote" should {
    "return 200" when {
      "service returns success" in {

        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val responseFromTtp = GenerateQuoteResponse(
          QuoteReference("quoteReference"),
          CustomerReference("customerReference"),
          QuoteType("quoteType"),
          List(Instalment(DutyId("dutyId"), DebtId("debtId"), LocalDate.parse("2022-01-01"), 100, 0.1, 1)),
          "1",
          100,
          0.1
        )
        (ttpQuoteService
          .generateQuote(_: GenerateQuoteRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(generateQuoteRequest, *, *)
          .returning(TtppEnvelope(responseFromTtp))

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest("POST", "/individuals/time-to-pay/quote")
            .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson[GenerateQuoteRequest](generateQuoteRequest))
        val response: Future[Result] = controller.generateQuote()(fakeRequest)
        status(response) shouldBe Status.OK
      }
    }
    "return 500" when {
      "service returns failure" in {

        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")
        (ttpQuoteService
          .generateQuote(_: GenerateQuoteRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(generateQuoteRequest, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[GenerateQuoteResponse])
          )

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest("POST", "/individuals/time-to-pay/quote")
            .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson[GenerateQuoteRequest](generateQuoteRequest))
        val response: Future[Result] = controller.generateQuote()(fakeRequest)

        status(response) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "GET /individuals/time-to-pay/quote/:customerReference/:pegaId" should {
    "return a 200 given a successful response" in {

      (authConnector
        .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(Future.successful())

      (ttpQuoteService
        .getExistingPlan(_: CustomerReference, _: PegaPlanId)(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *, *, *)
        .returning(
          TtppEnvelope(
            RetrievePlanResponse(
              "someCustomerRef",
              "somePegaId",
              "someQuoateStatus",
              "xyz",
              "ref",
              "info",
              "info",
              Nil,
              Nil,
              "2",
              100,
              0.26
            )
          )
        )

      val fakeRequest = FakeRequest(
        "GET",
        "/individuals/time-to-pay/quote/customerReference/pegaId"
      )
      val response: Future[Result] =
        controller.getExistingPlan("customerReference", "pegaId")(fakeRequest)

      status(response) shouldBe Status.OK
    }

    "return a 404 if the quote is not found" in {
      (authConnector
        .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(Future.successful())

      val errorFromTtpConnector = ConnectorError(404, "Not Found")
      (ttpQuoteService
        .getExistingPlan(_: CustomerReference, _: PegaPlanId)(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *, *, *)
        .returning(
          TtppEnvelope(errorFromTtpConnector.asLeft[RetrievePlanResponse])
        )

      val fakeRequest = FakeRequest(
        "GET",
        "/individuals/time-to-pay/quote/customerReference/pegaId"
      )
      val response: Future[Result] =
        controller.getExistingPlan("customerReference", "pegaId")(fakeRequest)

      status(response) shouldBe Status.NOT_FOUND
    }

    "return 500 if the underlying service fails" in {

      (authConnector
        .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(Future.successful())

      val errorFromTtpConnector = ConnectorError(500, "Internal Service Error")
      (ttpQuoteService
        .getExistingPlan(_: CustomerReference, _: PegaPlanId)(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *, *, *)
        .returning(
          TtppEnvelope(errorFromTtpConnector.asLeft[RetrievePlanResponse])
        )

      val fakeRequest = FakeRequest(
        "GET",
        "/individuals/time-to-pay/quote/customerReference/pegaId"
      )
      val response: Future[Result] =
        controller.getExistingPlan("customerReference", "pegaId")(fakeRequest)

      status(response) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "PUT /individuals/time-to-pay/quote/:customerReference/:pegaId" should {
    "return 200" when {
      "service returns success" in {

        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val responseFromTtp = UpdateQuoteResponse(
          CustomerReference("customerReference"),
          PegaPlanId("pageId"),
          QuoteStatus("quoteStatus"),
          LocalDate.now
        )
        (ttpQuoteService
          .updateQuote(_: UpdateQuoteRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(updateQuoteRequest, *, *)
          .returning(TtppEnvelope(responseFromTtp))

        val fakeRequest: FakeRequest[JsValue] = FakeRequest(
          "POST",
          s"/individuals/time-to-pay/quote/${updateQuoteRequest.customerReference.value}/${updateQuoteRequest.pegaPlanId.value}"
        ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
          .withBody(Json.toJson[UpdateQuoteRequest](updateQuoteRequest))
        val response: Future[Result] = controller.updateQuote(updateQuoteRequest.customerReference.value, updateQuoteRequest.pegaPlanId.value)(fakeRequest)
        status(response) shouldBe Status.OK
        Json.fromJson[UpdateQuoteResponse](contentAsJson(response)) shouldBe JsSuccess(
          responseFromTtp
        )
      }
    }

    "return 500" when {
      "service returns failure" in {

        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")
        (ttpQuoteService
          .updateQuote(_: UpdateQuoteRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(updateQuoteRequest, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[UpdateQuoteResponse])
          )

        val fakeRequest: FakeRequest[JsValue] = FakeRequest(
          "POST",
          s"/individuals/time-to-pay/quote/${updateQuoteRequest.customerReference.value}/${updateQuoteRequest.pegaPlanId.value}"
        ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
          .withBody(Json.toJson[UpdateQuoteRequest](updateQuoteRequest))
        val response: Future[Result] = controller.updateQuote(updateQuoteRequest.customerReference.value, updateQuoteRequest.pegaPlanId.value)(fakeRequest)

        status(response) shouldBe Status.INTERNAL_SERVER_ERROR
        Json.fromJson[TimeToPayErrorResponse](contentAsJson(response)) shouldBe JsSuccess(
          TimeToPayErrorResponse(500, "Internal Service Error")
        )
      }
    }
  }

  "POST /individuals/time-to-pay/quote/arrangement" should {
    "return 200" when {
      "service returns success" in {

        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val createPlanResponse = CreatePlanResponse(
          "customerReference",
          "pegaPlanId",
          "xyz"
        )
        (ttpQuoteService
          .createPlan(_: CreatePlanRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(createPlanRequest, *, *)
          .returning(TtppEnvelope(createPlanResponse))

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest("POST", "/individuals/time-to-pay/quote/arrangement")
            .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson[CreatePlanRequest](createPlanRequest))
        val response: Future[Result] = controller.createPlan()(fakeRequest)
        status(response) shouldBe Status.OK
      }
    }
    "return 500" when {
      "service returns failure" in {

        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")
        (ttpQuoteService
          .createPlan(_: CreatePlanRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(createPlanRequest, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[CreatePlanResponse])
          )

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest("POST", "/individuals/time-to-pay/quote/arrangement")
            .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson[CreatePlanRequest](createPlanRequest))
        val response: Future[Result] = controller.createPlan()(fakeRequest)

        status(response) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
