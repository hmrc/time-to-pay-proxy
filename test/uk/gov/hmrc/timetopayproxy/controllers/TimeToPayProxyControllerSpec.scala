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
import uk.gov.hmrc.timetopayproxy.models.MainTransType.TPSSContractSettlementINT
import uk.gov.hmrc.timetopayproxy.models.SubTransType.TGPEN

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
      Some(1),
      Some(Frequency.Annually),
      Some(Duration(12)),
      Some(1),
      Some(LocalDate.now()),
      PaymentPlanType.TimeToPay
    ),
    List(),
    List()
  )

  val queryParameterNotMatchingPayload = "customerReference and planId in the query parameters should match the ones in the request payload"

  private val updatePlanRequest =
    UpdatePlanRequest(
      CustomerReference("customerReference"),
      PlanId("planId"),
      UpdateType("updateType"),
      PlanStatus.Success,
      None,
      Some(CancellationReason("reason")),
      Some(true),
      Some(
        List(
          PaymentInformation(PaymentMethod.Bacs, Some(PaymentReference("reference")))
        )
      )
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
        Some(100),
        PaymentPlanType.TimeToPay,
        false,
        2,
        Some(Frequency.Single),
        Some(Duration(2)),
        Some(LocalDate.now()),
        Some(100),
        100,
        10,
        10,
        10
      ),
      List(
        DebtItemCharge(
          DebtItemChargeId("debtItemChargeId"),
          MainTransType.TPSSAccTaxAssessment,
          SubTransType.IT,
          100,
          Some(LocalDate.now()),
          Some(List(Payment(LocalDate.parse("2020-01-01"), 100)))
        )
      ),
      List(PaymentInformation(PaymentMethod.Bacs, Some(PaymentReference("ref123")))),
      List(CustomerPostCode(PostCode("NW1 AB1"), LocalDate.now())),
      List(
        Instalment(
          DebtItemChargeId("id1"),
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

  val viewPlanResponse: ViewPlanResponse = ViewPlanResponse(
    CustomerReference("customerRef1234"),
    ChannelIdentifier.Advisor,
    Plan(
      PlanId("planId123"),
      CaseId("caseId123"),
      QuoteId("quoteId"),
      LocalDate.now(),
      QuoteType.InstalmentAmount,
      PaymentPlanType.TimeToPay,
      thirdPartyBank = true,
      0,
      0.0,
      0.0,
      0.0,
      0.0
    ),
    Seq(
      DebtItemCharge(
        DebtItemChargeId("debtItemChargeId1"),
        TPSSContractSettlementINT,
        TGPEN,
        100,
        Some(LocalDate.parse("2021-05-13")),
        Some(List(Payment(LocalDate.parse("2021-05-13"), 100)))
      )
    ),
    Seq.empty[PaymentInformation],
    Seq.empty[CustomerPostCode],
    Seq(
      Instalment(
        DebtItemChargeId("debtItemChargeId"),
        LocalDate.parse("2021-05-01"),
        100,
        100,
        0.26,
        1,
        10.20,
        100
      ),
      Instalment(
        DebtItemChargeId("debtItemChargeId"),
        LocalDate.parse("2021-06-01"),
        100,
        100,
        0.26,
        2,
        10.20,
        100
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
          0.9,
          List(
            Instalment(
              DebtItemChargeId("dutyId"),
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
        contentAsJson(response) shouldBe Json.toJson[GenerateQuoteResponse](
          responseFromTtp
        )
      }
    }
    val wrongFormattedBody = """{
                                  "customerReference": "uniqRef1234",
                                  "quoteReference": "quoteRef1234",
                                  "channelIdentifier": "advisor",
                                  "plan": {
                                    "quoteType": "instalmentAmount",
                                    "quoteDate": "2021-09-08",
                                    "instalmentStartDate": "2021-05-13",
                                    "instalmentAmount": true,
                                    "initialPaymentDate": "2021-05-13",
                                    "paymentPlanType": "timeToPay",
                                    "thirdPartyBank": true,
                                    "numberOfInstalments": 1,
                                    "frequency": "annually",
                                    "duration": 12,
                                    "initialPaymentAmount": 100,
                                    "totalDebtincInt": 10,
                                    "totalInterest": 0.14,
                                    "interestAccrued": 10,
                                    "planInterest": 0.24
                                  },
                                  "debtItemCharges": [],
                                  "customerPostCodes": []
                                }"""
    "return 400" when {
      "request body is in wrong format" in {
        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest("POST", "/individuals/time-to-pay/quote")
            .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.parse(wrongFormattedBody))

        val response: Future[Result] = controller.generateQuote()(fakeRequest)

        status(response) shouldBe Status.BAD_REQUEST
        (contentAsJson(response) \ "errorMessage")
          .as[String] shouldBe "Invalid GenerateQuoteRequest payload: Payload has a missing field or an invalid format. Field name: instalmentAmount. "
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
        (contentAsJson(response) \ "errorMessage")
          .as[String] shouldBe ("Internal Service Error")

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
        .returning(TtppEnvelope(viewPlanResponse))

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
          PlanStatus.Success,
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

        val errorResponse = Status.INTERNAL_SERVER_ERROR
        status(response) shouldBe errorResponse.intValue()
        Json.fromJson[TtppErrorResponse](contentAsJson(response)) shouldBe JsSuccess(
          TtppErrorResponse(errorResponse.intValue(), "Internal Service Error")
        )
      }
    }

    "return 400" when {
      "customerReference on query parameters do not match customer reference in payload" in {
        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
          _: HeaderCarrier,
          _: ExecutionContext
        ))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val wrongCustomerReferenceInQueryParameters = s"${updatePlanRequest.customerReference.value}-wrong"
        val fakeRequest: FakeRequest[JsValue] = FakeRequest(
          "POST",
          s"/individuals/time-to-pay/quote/$wrongCustomerReferenceInQueryParameters/${updatePlanRequest.planId.value}"
        ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
          .withBody(Json.toJson[UpdatePlanRequest](updatePlanRequest))
        val response: Future[Result] = controller.updatePlan(
          wrongCustomerReferenceInQueryParameters,
          updatePlanRequest.planId.value
        )(fakeRequest)

        val errorResponse = Status.BAD_REQUEST
        status(response) shouldBe errorResponse.intValue()
        Json.fromJson[TtppErrorResponse](contentAsJson(response)) shouldBe JsSuccess(
          TtppErrorResponse(errorResponse.intValue(), queryParameterNotMatchingPayload)
        )
      }
      "planId on query parameters do not match customer reference in payload" in {
        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
          _: HeaderCarrier,
          _: ExecutionContext
        ))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val wrongPlanIdInQueryParameters = s"${updatePlanRequest.planId.value}-wrong"
        val fakeRequest: FakeRequest[JsValue] = FakeRequest(
          "POST",
          s"/individuals/time-to-pay/quote/${updatePlanRequest.customerReference.value}/$wrongPlanIdInQueryParameters"
        ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
          .withBody(Json.toJson[UpdatePlanRequest](updatePlanRequest))
        val response: Future[Result] = controller.updatePlan(
          wrongPlanIdInQueryParameters,
          updatePlanRequest.planId.value
        )(fakeRequest)

        val errorResponse = Status.BAD_REQUEST
        status(response) shouldBe errorResponse.intValue()
        Json.fromJson[TtppErrorResponse](contentAsJson(response)) shouldBe JsSuccess(
          TtppErrorResponse(errorResponse.intValue(), queryParameterNotMatchingPayload)
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
        contentAsJson(response) shouldBe Json.toJson[CreatePlanResponse](
          createPlanResponse
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
