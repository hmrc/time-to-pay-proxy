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
    CustomerReference("customerReference"),
    ChannelIdentifier.Advisor,
    PlanToGenerateQuote(
      QuoteType.Duration,
      LocalDate.of(2021, 1, 1),
      LocalDate.of(2021, 1, 1),
      1,
      Frequency.Annually,
      Duration(12),
      Some(1),
      Some(LocalDate.now()),
      PaymentPlanType.TimeToPay
    ),
    List(),
    List()
  )

  private val updatePlanRequest =
    UpdatePlanRequest(
      CustomerReference("customerReference"),
      PlanId("planId"),
      UpdateType("updateType"),
      CancellationReason("reason"),
      PaymentMethod.Bacs,
      PaymentReference("reference"),
      true
    )

  private val createPlanRequest =
    CreatePlanRequest(
      CustomerReference("customerReference"),
      QuoteReference("quoteReference"),
      ChannelIdentifier.Advisor,
      PlanToCreatePlan(
        QuoteId("quoteId"),
        QuoteType.Duration,
        LocalDate.now(),
        LocalDate.now(),
        PaymentPlanType.TimeToPay,
        None,
        false,
        2,
        Frequency.Single,
        Duration(2),
        100,
        10,
        10,
        10
      ),
      List(
        DebtItem(
          DebtItemId("debtItemId"),
          DebtItemChargeId("debtItemChargeId"),
          MainTransType.TPSSAccTaxAssessment,
          SubTransType.IT,
          100,
          LocalDate.now(),
          List(Payment(LocalDate.parse("2020-01-01"), 100))
        )
      ),
      List(PaymentInformation(PaymentMethod.Bacs, PaymentReference("ref123"))),
      List(CustomerPostCode(PostCode("NW1 AB1"), LocalDate.now())),
      List(
        Instalment(
          DebtItemChargeId("id1"),
          DebtItemId("id2"),
          LocalDate.now(),
          100,
          100,
          0.24,
          1,
          10,
          90
        )
      )
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
          QuoteType.Duration,
          LocalDate.now(),
          1,
          100,
          0.6,
          0.9,
          List(
            Instalment(
              DebtItemChargeId("dutyId"),
              DebtItemId("debtId"),
              LocalDate.parse("2022-01-01"),
              100,
              100,
              0.1,
              1,
              0.5,
              10
            )
          )
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

  "GET /individuals/time-to-pay/quote/:customerReference/:planId" should {
    "return a 200 given a successful response" in {

      (authConnector
        .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
          _: HeaderCarrier,
          _: ExecutionContext
        ))
        .expects(*, *, *, *)
        .returning(Future.successful())

      (ttpQuoteService
        .getExistingPlan(_: CustomerReference, _: PlanId)(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *, *, *)
        .returning(
          TtppEnvelope(
            ViewPlanResponse(
              CustomerReference("someCustomerRef"),
              PlanId("somePlanId"),
              QuoteType.Duration,
              "xyz",
              "xyz",
              Nil,
              "2",
              100,
              0.26
            )
          )
        )

      val fakeRequest = FakeRequest(
        "GET",
        "/individuals/time-to-pay/quote/customerReference/planId"
      )
      val response: Future[Result] =
        controller.viewPlan("customerReference", "planId")(fakeRequest)

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
        .getExistingPlan(_: CustomerReference, _: PlanId)(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *, *, *)
        .returning(TtppEnvelope(errorFromTtpConnector.asLeft[ViewPlanResponse]))

      val fakeRequest = FakeRequest(
        "GET",
        "/individuals/time-to-pay/quote/customerReference/planId"
      )
      val response: Future[Result] =
        controller.viewPlan("customerReference", "planId")(fakeRequest)

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
        .getExistingPlan(_: CustomerReference, _: PlanId)(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(*, *, *, *)
        .returning(TtppEnvelope(errorFromTtpConnector.asLeft[ViewPlanResponse]))

      val fakeRequest = FakeRequest(
        "GET",
        "/individuals/time-to-pay/quote/customerReference/planId"
      )
      val response: Future[Result] =
        controller.viewPlan("customerReference", "planId")(fakeRequest)

      status(response) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

  "PUT /individuals/time-to-pay/quote/:customerReference/:planId" should {
    "return 200" when {
      "service returns success" in {

        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val responseFromTtp = UpdatePlanResponse(
          CustomerReference("customerReference"),
          PlanId("pageId"),
          QuoteStatus("quoteStatus"),
          LocalDate.now
        )
        (ttpQuoteService
          .updatePlan(_: UpdatePlanRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(updatePlanRequest, *, *)
          .returning(TtppEnvelope(responseFromTtp))

        val fakeRequest: FakeRequest[JsValue] = FakeRequest(
          "POST",
          s"/individuals/time-to-pay/quote/${updatePlanRequest.customerReference.value}/${updatePlanRequest.planId.value}"
        ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
          .withBody(Json.toJson[UpdatePlanRequest](updatePlanRequest))
        val response: Future[Result] = controller.updatePlan(
          updatePlanRequest.customerReference.value,
          updatePlanRequest.planId.value
        )(fakeRequest)
        status(response) shouldBe Status.OK
        Json.fromJson[UpdatePlanResponse](contentAsJson(response)) shouldBe JsSuccess(
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
          .updatePlan(_: UpdatePlanRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(updatePlanRequest, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[UpdatePlanResponse])
          )

        val fakeRequest: FakeRequest[JsValue] = FakeRequest(
          "POST",
          s"/individuals/time-to-pay/quote/${updatePlanRequest.customerReference.value}/${updatePlanRequest.planId.value}"
        ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
          .withBody(Json.toJson[UpdatePlanRequest](updatePlanRequest))
        val response: Future[Result] = controller.updatePlan(
          updatePlanRequest.customerReference.value,
          updatePlanRequest.planId.value
        )(fakeRequest)

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
          CustomerReference("customerReference"),
          PlanId("planId"),
          CaseId("caseId"),
          PlanStatus.Success
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
