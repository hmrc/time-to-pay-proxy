/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.syntax.either._
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.{ MimeTypes, Status }
import play.api.libs.json.{ JsArray, JsSuccess, JsValue, Json }
import play.api.mvc.{ ControllerComponents, Result }
import play.api.test.Helpers._
import play.api.test.{ FakeRequest, Helpers }
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.actions.auth.{ AuthoriseAction, AuthoriseActionImpl }
import uk.gov.hmrc.timetopayproxy.config.FeatureSwitch
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.affordablequotes._
import uk.gov.hmrc.timetopayproxy.services.TTPQuoteService

import java.time.{ LocalDate, LocalDateTime }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class TimeToPayProxyControllerSpec extends AnyWordSpec with Matchers with MockFactory {

  private val authConnector: PlayAuthConnector = mock[PlayAuthConnector]

  private val cc: ControllerComponents = Helpers.stubControllerComponents()
  private val authoriseAction: AuthoriseAction =
    new AuthoriseActionImpl(authConnector, cc)

  private val ttpQuoteService = mock[TTPQuoteService]
  private val fs: FeatureSwitch = mock[FeatureSwitch]
  private val controller =
    new TimeToPayProxyController(authoriseAction, cc, ttpQuoteService, fs)

  private val generateQuoteRequest = GenerateQuoteRequest(
    CustomerReference("customerReference"),
    ChannelIdentifier.Advisor,
    PlanToGenerateQuote(
      QuoteType.Duration,
      LocalDate.of(2021, 1, 1),
      LocalDate.of(2021, 1, 1),
      Some(1),
      Some(FrequencyLowercase.Annually),
      Some(Duration(12)),
      Some(1),
      Some(LocalDate.now()),
      PaymentPlanType.TimeToPay
    ),
    List(),
    List()
  )

  val queryParameterNotMatchingPayload =
    "customerReference and planId in the query parameters should match the ones in the request payload"

  private val updatePlanRequest =
    UpdatePlanRequest(
      CustomerReference("customerReference"),
      PlanId("planId"),
      UpdateType("updateType"),
      Some(PlanStatus.Success),
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
        Some(FrequencyLowercase.Single),
        Some(Duration(2)),
        Some(PaymentMethod.Bacs),
        Some(PaymentReference("ref123")),
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
          "1525",
          "1000",
          100,
          Some(LocalDate.now()),
          List(Payment(LocalDate.parse("2020-01-01"), 100))
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
    CustomerReference(value = "customerRef1234"),
    ChannelIdentifier.Advisor,
    ViewPlanResponsePlan(
      PlanId("planId123"),
      CaseId("caseId123"),
      QuoteId("quoteId"),
      LocalDate.now(),
      QuoteType.InstalmentAmount,
      PaymentPlanType.TimeToPay,
      thirdPartyBank = true,
      0,
      None,
      None,
      0,
      0.0,
      0,
      0.0
    ),
    Seq(
      DebtItemCharge(
        DebtItemChargeId("debtItemChargeId1"),
        "1546",
        "1090",
        100,
        Some(LocalDate.parse("2021-05-13")),
        List(Payment(LocalDate.parse("2021-05-13"), 100))
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
        debtItemChargeId = DebtItemChargeId("debtItemChargeId"),
        dueDate = LocalDate.parse("2021-06-01"),
        amountDue = 100,
        expectedPayment = 100,
        interestRate = 0.26,
        instalmentNumber = 2,
        instalmentInterestAccrued = 10.20,
        instalmentBalance = 100
      )
    ),
    collections = Collections(
      None,
      List(
        RegularCollection(dueDate = LocalDate.parse("2021-05-01"), amountDue = 100),
        RegularCollection(dueDate = LocalDate.parse("2021-06-01"), amountDue = 100)
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
          .returning(Future.successful(()))

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
              debtItemChargeId = DebtItemChargeId("dutyId"),
              dueDate = LocalDate.parse("2022-01-01"),
              amountDue = 100,
              expectedPayment = 100,
              interestRate = 0.1,
              instalmentNumber = 1,
              instalmentInterestAccrued = 0.5,
              instalmentBalance = 10
            )
          ),
          Collections(
            Some(InitialCollection(LocalDate.now(), 1)),
            List(RegularCollection(LocalDate.parse("2022-01-01"), 100))
          )
        )

        (ttpQuoteService
          .generateQuote(_: GenerateQuoteRequest, _: Map[String, Seq[String]])(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(generateQuoteRequest, *, *, *)
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
          .returning(Future.successful(()))

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
          .returning(Future.successful(()))

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")
        (ttpQuoteService
          .generateQuote(_: GenerateQuoteRequest, _: Map[String, Seq[String]])(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(generateQuoteRequest, *, *, *)
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
          .as[String] shouldBe "Internal Service Error"

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
        .returning(Future.successful(()))

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
        .returning(Future.successful(()))

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
        .returning(Future.successful(()))

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
          .returning(Future.successful(()))

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
          "PUT",
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
      "when paymentMethod is not directDebit and paymentReference is missing" in {

        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful(()))

        val updatePlanRequestMissingPaymentReference: UpdatePlanRequest =
          Json
            .obj(
              "customerReference" -> "customerRef1234",
              "planId"            -> "planId1234",
              "planStatus"        -> "success",
              "updateType"        -> "paymentDetails",
              "thirdPartyBank"    -> false,
              "payments" ->
                JsArray(
                  List(
                    Json.obj(
                      "paymentMethod" -> "cardPayment"
                    )
                  )
                )
            )
            .as[UpdatePlanRequest]

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest(
            "PUT",
            s"/individuals/time-to-pay/quote/${updatePlanRequestMissingPaymentReference.customerReference.value}/${updatePlanRequestMissingPaymentReference.planId.value}"
          ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson(updatePlanRequestMissingPaymentReference))

        val responseFromTtp = UpdatePlanResponse(
          CustomerReference("customerRef1234"),
          PlanId("planId1234"),
          PlanStatus.Success,
          LocalDate.now
        )
        (ttpQuoteService
          .updatePlan(_: UpdatePlanRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(updatePlanRequestMissingPaymentReference, *, *)
          .returning(TtppEnvelope(responseFromTtp))

        val response: Future[Result] =
          controller.updatePlan(
            updatePlanRequestMissingPaymentReference.customerReference.value,
            updatePlanRequestMissingPaymentReference.planId.value
          )(fakeRequest)

        status(response) shouldBe Status.OK
        Json.fromJson[UpdatePlanResponse](contentAsJson(response)) shouldBe JsSuccess(
          responseFromTtp
        )
      }

      "when planStatus is missing for a non 'planStatus' updateType" in {

        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful(()))

        val updatePlanRequestMissingPlanStatus: UpdatePlanRequest =
          Json
            .obj(
              "customerReference" -> "customerRef1234",
              "planId"            -> "planId1234",
              "updateType"        -> "paymentDetails",
              "thirdPartyBank"    -> false,
              "payments" ->
                JsArray(
                  List(
                    Json.obj(
                      "paymentMethod" -> "cardPayment"
                    )
                  )
                )
            )
            .as[UpdatePlanRequest]

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest(
            "PUT",
            s"/individuals/time-to-pay/quote/${updatePlanRequestMissingPlanStatus.customerReference.value}/${updatePlanRequestMissingPlanStatus.planId.value}"
          ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson(updatePlanRequestMissingPlanStatus))

        val responseFromTtp = UpdatePlanResponse(
          CustomerReference("customerRef1234"),
          PlanId("planId1234"),
          PlanStatus.Success,
          LocalDate.now
        )
        (ttpQuoteService
          .updatePlan(_: UpdatePlanRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(updatePlanRequestMissingPlanStatus, *, *)
          .returning(TtppEnvelope(responseFromTtp))

        val response: Future[Result] =
          controller.updatePlan(
            updatePlanRequestMissingPlanStatus.customerReference.value,
            updatePlanRequestMissingPlanStatus.planId.value
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
          .returning(Future.successful(()))

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
          "PUT",
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
          .returning(Future.successful(()))

        val wrongCustomerReferenceInQueryParameters = s"${updatePlanRequest.customerReference.value}-wrong"
        val fakeRequest: FakeRequest[JsValue] = FakeRequest(
          "PUT",
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
      "planId on query parameters do not match planId in payload" in {
        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful(()))

        val wrongPlanIdInQueryParameters = s"${updatePlanRequest.planId.value}-wrong"
        val fakeRequest: FakeRequest[JsValue] = FakeRequest(
          "PUT",
          s"/individuals/time-to-pay/quote/${updatePlanRequest.customerReference.value}/$wrongPlanIdInQueryParameters"
        ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
          .withBody(Json.toJson[UpdatePlanRequest](updatePlanRequest))
        val response: Future[Result] = controller.updatePlan(
          updatePlanRequest.customerReference.value,
          wrongPlanIdInQueryParameters
        )(fakeRequest)

        val errorResponse = Status.BAD_REQUEST
        status(response) shouldBe errorResponse.intValue()
        Json.fromJson[TtppErrorResponse](contentAsJson(response)) shouldBe JsSuccess(
          TtppErrorResponse(errorResponse.intValue(), queryParameterNotMatchingPayload)
        )
      }
      "missing paymentReference in payments and paymentMethod is directDebit" in {
        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful(()))

        val invalidJsonBody: JsValue =
          Json.obj(
            "customerReference" -> "customerRef1234",
            "planId"            -> "planId1234",
            "updateType"        -> "paymentDetails",
            "thirdPartyBank"    -> false,
            "payments" ->
              JsArray(
                List(
                  Json.obj(
                    "paymentMethod" -> "directDebit"
                  )
                )
              )
          )

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest(
            "PUT",
            s"/individuals/time-to-pay/quote/customerRef1234/planId1234"
          ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(invalidJsonBody)

        val response: Future[Result] =
          controller.updatePlan(
            "customerRef1234",
            "planId1234"
          )(fakeRequest)

        val errorResponse = Status.BAD_REQUEST
        status(response) shouldBe errorResponse.intValue()
        Json.fromJson[TtppErrorResponse](contentAsJson(response)) shouldBe JsSuccess(
          TtppErrorResponse(
            errorResponse.intValue(),
            "Could not parse body due to requirement failed: Direct Debit should always have payment reference"
          )
        )
      }
      "paymentReference is empty in payments when paymentMethod is directDebit" in {
        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful(()))

        val invalidJsonBody: JsValue =
          Json.obj(
            "customerReference" -> "customerRef1234",
            "planId"            -> "planId1234",
            "updateType"        -> "paymentDetails",
            "thirdPartyBank"    -> false,
            "payments" ->
              JsArray(
                List(
                  Json.obj(
                    "paymentMethod"    -> "directDebit",
                    "paymentReference" -> ""
                  )
                )
              )
          )

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest(
            "PUT",
            s"/individuals/time-to-pay/quote/customerRef1234/planId1234"
          ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(invalidJsonBody)

        val response: Future[Result] =
          controller.updatePlan(
            "customerRef1234",
            "planId1234"
          )(fakeRequest)

        val errorResponse = Status.BAD_REQUEST
        status(response) shouldBe errorResponse.intValue()
        Json.fromJson[TtppErrorResponse](contentAsJson(response)) shouldBe JsSuccess(
          TtppErrorResponse(
            errorResponse.intValue(),
            "Could not parse body due to requirement failed: Direct Debit should always have payment reference"
          )
        )
      }

      "paymentReference in payments is empty" in {
        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful(()))

        val invalidJsonBody: JsValue =
          Json.obj(
            "customerReference" -> "customerRef1234",
            "planId"            -> "planId1234",
            "updateType"        -> "paymentDetails",
            "thirdPartyBank"    -> false,
            "payments" ->
              JsArray(
                List(
                  Json.obj(
                    "paymentMethod"    -> "cardPayment",
                    "paymentReference" -> ""
                  )
                )
              )
          )

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest(
            "PUT",
            s"/individuals/time-to-pay/quote/customerRef1234/planId1234"
          ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(invalidJsonBody)

        val response: Future[Result] =
          controller.updatePlan(
            "customerRef1234",
            "planId1234"
          )(fakeRequest)

        val errorResponse = Status.BAD_REQUEST
        status(response) shouldBe errorResponse.intValue()
        Json.fromJson[TtppErrorResponse](contentAsJson(response)) shouldBe JsSuccess(
          TtppErrorResponse(
            errorResponse.intValue(),
            "Could not parse body due to requirement failed: paymentReference should not be empty"
          )
        )
      }

      "missing field planStatus when the updateType is planStatus" in {
        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful(()))

        val invalidJsonBody: JsValue =
          Json.obj(
            "customerReference" -> "custReference1234",
            "planId"            -> "planId1234",
            "updateType"        -> "planStatus",
            "thirdPartyBank"    -> false
          )

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest(
            "PUT",
            s"/individuals/time-to-pay/quote/custReference1234/planId1234"
          ).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(invalidJsonBody)

        val response: Future[Result] =
          controller.updatePlan(
            "custReference1234",
            "planId1234"
          )(fakeRequest)

        val errorResponse = Status.BAD_REQUEST
        status(response) shouldBe errorResponse.intValue()
        Json.fromJson[TtppErrorResponse](contentAsJson(response)) shouldBe JsSuccess(
          TtppErrorResponse(
            errorResponse.intValue(),
            "Could not parse body due to requirement failed: Invalid UpdatePlanRequest payload: Payload has a missing field or an invalid format. Field name: planStatus."
          )
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
          .returning(Future.successful(()))

        val createPlanResponse = CreatePlanResponse(
          CustomerReference("customerReference"),
          PlanId("planId"),
          CaseId("caseId"),
          PlanStatus.Success
        )
        (ttpQuoteService
          .createPlan(_: CreatePlanRequest, _: Map[String, Seq[String]])(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(createPlanRequest, *, *, *)
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
          .returning(Future.successful(()))

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")
        (ttpQuoteService
          .createPlan(_: CreatePlanRequest, _: Map[String, Seq[String]])(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(createPlanRequest, *, *, *)
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

  "POST /individuals/time-to-pay/self-serve/affordable-quotes" should {
    val affordableQuotesRequest = AffordableQuotesRequest(
      channelIdentifier = "eSSTTP",
      paymentPlanAffordableAmount = 500,
      paymentPlanFrequency = FrequencyCapitalised.Monthly,
      paymentPlanMaxLength = Duration(6),
      paymentPlanMinLength = Duration(1),
      accruedDebtInterest = 500,
      paymentPlanStartDate = LocalDate.parse("2022-02-02"),
      initialPaymentDate = Some(LocalDate.parse("2022-02-02")),
      initialPaymentAmount = Some(500),
      debtItemCharges = List(
        DebtItemChargeSelfServe(
          outstandingDebtAmount = 100000,
          mainTrans = "1525",
          subTrans = "1000",
          DebtItemChargeId("ChargeRef 0903_2"),
          interestStartDate = Some(LocalDate.parse("2021-09-03")),
          debtItemOriginalDueDate = LocalDate.now(),
          Some(IsInterestBearingCharge(true)),
          Some(UseChargeReference(false))
        )
      ),
      customerPostcodes = List(
        CustomerPostCode(
          PostCode("some postcode"),
          LocalDate.parse("2022-03-09")
        )
      )
    )
    val affordableQuoteResponse = AffordableQuoteResponse(
      LocalDateTime.parse("2025-01-13T10:15:30.975"),
      paymentPlans = List(
        AffordableQuotePaymentPlan(
          numberOfInstalments = 1,
          planDuration = Duration(1),
          planInterest = 1,
          totalDebt = 100,
          totalDebtIncInt = 100,
          collections = Collections(
            initialCollection = None,
            List(RegularCollection(dueDate = LocalDate.parse("2000-01-01"), amountDue = 1))
          ),
          instalments = List(
            AffordableQuoteInstalment(
              DebtItemChargeId("ChargeRef 0903_2"),
              dueDate = LocalDate.parse("2000-01-01"),
              amountDue = 1,
              instalmentNumber = 1,
              instalmentInterestAccrued = 100,
              instalmentBalance = 100,
              debtItemOriginalDueDate = LocalDate.parse("2000-01-01")
            )
          )
        )
      )
    )

    "return 200" when {
      "service returns success" in {
        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful(()))

        (ttpQuoteService
          .getAffordableQuotes(_: AffordableQuotesRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(affordableQuotesRequest, *, *)
          .returning(TtppEnvelope(affordableQuoteResponse))

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest("POST", "/individuals/time-to-pay-proxy/self-serve/affordable-quotes")
            .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson[AffordableQuotesRequest](affordableQuotesRequest))

        val response: Future[Result] = controller.getAffordableQuotes()(fakeRequest)

        status(response) shouldBe Status.OK
        contentAsJson(response) shouldBe Json.toJson[AffordableQuoteResponse](
          affordableQuoteResponse
        )

      }
    }
    "return 500" when {
      "TTP returns a failure" in {
        (authConnector
          .authorise[Unit](_: Predicate, _: Retrieval[Unit])(
            _: HeaderCarrier,
            _: ExecutionContext
          ))
          .expects(*, *, *, *)
          .returning(Future.successful(()))

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Server Error")

        (ttpQuoteService
          .getAffordableQuotes(_: AffordableQuotesRequest)(
            _: ExecutionContext,
            _: HeaderCarrier
          ))
          .expects(affordableQuotesRequest, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[AffordableQuoteResponse])
          )

        val fakeRequest: FakeRequest[JsValue] =
          FakeRequest("POST", "/individuals/time-to-pay-proxy/self-serve/affordable-quotes")
            .withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
            .withBody(Json.toJson[AffordableQuotesRequest](affordableQuotesRequest))

        val response: Future[Result] = controller.getAffordableQuotes()(fakeRequest)

        status(response) shouldBe Status.INTERNAL_SERVER_ERROR
        contentAsJson(response) shouldBe Json.toJson[TtppErrorResponse](
          TtppErrorResponse(statusCode = 500, errorMessage = "Internal Server Error")
        )
      }
    }
  }
}
