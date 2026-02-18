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

package uk.gov.hmrc.timetopayproxy.connectors

import cats.data.NonEmptyList
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{ Json, Writes }
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }
import play.api.{ ConfigLoader, Configuration }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.timetopayproxy.config.{ AppConfig, FeatureSwitch }
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.error.ConnectorError
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.featureSwitches.{ InternalAuthEnabled, SaRelease2Enabled }
import uk.gov.hmrc.timetopayproxy.models.saonly.common._
import uk.gov.hmrc.timetopayproxy.models.saonly.common.apistatus.{ ApiName, ApiStatus, ApiStatusCode }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel._
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpfullamend.{ TtpFullAmendInformativeError, TtpFullAmendInternalError, TtpFullAmendRequest, TtpFullAmendSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform._
import uk.gov.hmrc.timetopayproxy.support.WireMockUtils

import java.time.{ Instant, LocalDate }
import scala.concurrent.ExecutionContext

final class TtpFeedbackLoopConnectorSpec
    extends PlaySpec with DefaultAwaitTimeout with FutureAwaits with MockFactory with WireMockUtils {

  val config = mock[Configuration]
  val servicesConfig = mock[ServicesConfig]
  val featureSwitch = mock[FeatureSwitch]

  val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  class Setup(internalAuthEnabled: Boolean = false) {
    implicit val ec: ExecutionContext = ExecutionContext.global
    implicit val hc: HeaderCarrier = HeaderCarrier()

    (servicesConfig
      .baseUrl(_: String))
      .expects("auth")
      .once()
      .returns("http://localhost:11111")
    (servicesConfig
      .baseUrl(_: String))
      .expects("ttp")
      .once()
      .returns("http://localhost:11111")
    (servicesConfig
      .baseUrl(_: String))
      .expects("ttpe")
      .once()
      .returns("unused")
    (servicesConfig
      .baseUrl(_: String))
      .expects("stub")
      .once()
      .returns("http://localhost:11111")
    (config
      .get(_: String)(_: ConfigLoader[String]))
      .expects("microservice.services.ttp.token", *)
      .once()
      .returns("TOKEN")
    (config
      .get(_: String)(_: ConfigLoader[Boolean]))
      .expects("auditing.enabled", *)
      .once()
      .returns(false)
    (config
      .get(_: String)(_: ConfigLoader[String]))
      .expects("microservice.metrics.graphite.host", *)
      .once()
      .returns("http://localhost:11111")
    (config
      .getOptional(_: String)(_: ConfigLoader[Option[Configuration]]))
      .expects("feature-switch", *)
      .once()
      .returns(None)
    (config
      .get(_: String)(_: ConfigLoader[String]))
      .expects("internal-auth.token", *)
      .returns("valid-auth-token")

    (() => featureSwitch.internalAuthEnabled)
      .expects()
      .returning(InternalAuthEnabled(internalAuthEnabled))

    val mockConfiguration: AppConfig = new MockAppConfig(config, servicesConfig, internalAuthEnabled)

    val connector: TtpFeedbackLoopConnector = new TtpFeedbackLoopConnector(mockConfiguration, httpClient, featureSwitch)
  }

  "TtpFeedbackLoopConnector" should {
    ".cancelTtp" should {

      val ttpCancelRequest: TtpCancelRequest = TtpCancelRequest(
        identifications = NonEmptyList.of(
          Identification(idType = IdType("NINO"), idValue = IdValue("AB123456C"))
        ),
        paymentPlan = TtpCancelPaymentPlan(
          arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2025-01-01")),
          ttpEndDate = TtpEndDate(LocalDate.parse("2025-02-01")),
          frequency = FrequencyLowercase.Monthly,
          cancellationDate = CancellationDate(LocalDate.parse("2025-01-15")),
          initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2025-01-05"))),
          initialPaymentAmount = Some(GbpPounds.createOrThrow(100.00))
        ),
        instalments = NonEmptyList.of(
          SaOnlyInstalment(
            dueDate = InstalmentDueDate(LocalDate.parse("2025-01-31")),
            amountDue = GbpPounds.createOrThrow(500.00)
          )
        ),
        channelIdentifier = ChannelIdentifier.Advisor,
        transitioned = Some(TransitionedIndicator(true))
      )

      val ttpCancelResponse: TtpCancelSuccessfulResponse = TtpCancelSuccessfulResponse(
        apisCalled = List(
          ApiStatus(
            name = ApiName("API1"),
            statusCode = ApiStatusCode(200),
            processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
            errorResponse = None
          )
        ),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
      )

      val ttpCancelInformativeErrorResponse: TtpCancelInformativeError = TtpCancelInformativeError(
        apisCalled = Some(
          List(
            ApiStatus(
              name = ApiName("API1"),
              statusCode = ApiStatusCode(200),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
              errorResponse = None
            )
          )
        ),
        internalErrors = List(TtpCancelInternalError("some error that ttp is responsible for")),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
      )

      "return a successful response" in new Setup() {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/cancel",
          Json.toJson(ttpCancelRequest).toString(),
          200,
          Json.toJson(ttpCancelResponse).toString()
        )

        val result = connector.cancelTtp(ttpCancelRequest)

        await(result.value) mustBe Right(ttpCancelResponse: TtpCancelSuccessfulResponse)
      }

      "parse an error response from an upstream service" in new Setup() {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/cancel",
          Json.toJson(ttpCancelRequest).toString(),
          400,
          """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
        )

        val result = connector.cancelTtp(ttpCancelRequest)

        await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
      }

      "handle 500 responses" in new Setup() {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/cancel",
          Json.toJson(ttpCancelRequest).toString(),
          500,
          Json.toJson(ttpCancelInformativeErrorResponse).toString()
        )

        val result = connector.cancelTtp(ttpCancelRequest)

        await(result.value) mustBe
          Left(
            TtpCancelInformativeError(
              apisCalled = Some(
                List(
                  ApiStatus(
                    ApiName("API1"),
                    ApiStatusCode(200),
                    ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
                    errorResponse = None
                  )
                )
              ),
              internalErrors = List(TtpCancelInternalError("some error that ttp is responsible for")),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
            )
          )
      }

      "using InternalAuth" should {
        "return a successful response" in new Setup(internalAuthEnabled = true) {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/cancel",
            Json.toJson(ttpCancelRequest).toString(),
            200,
            Json.toJson(ttpCancelResponse).toString()
          )

          val result = connector.cancelTtp(ttpCancelRequest)

          await(result.value) mustBe Right(ttpCancelResponse: TtpCancelSuccessfulResponse)
        }

        "return an unauthorized response from an upstream service" in new Setup(internalAuthEnabled = true) {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/cancel",
            Json.toJson(ttpCancelRequest).toString(),
            401,
            """{"failures": [{"code": "401", "reason": "Unauthorized"}]}"""
          )

          val result = connector.cancelTtp(ttpCancelRequest)

          await(result.value) mustBe Left(ConnectorError(401, "Unauthorized"))
        }

        "parse an error response from an upstream service" in new Setup(internalAuthEnabled = true) {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/cancel",
            Json.toJson(ttpCancelRequest).toString(),
            400,
            """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
          )

          val result = connector.cancelTtp(ttpCancelRequest)

          await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
        }

        "handle 500 responses" in new Setup(internalAuthEnabled = true) {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/cancel",
            Json.toJson(ttpCancelRequest).toString(),
            500,
            Json.toJson(ttpCancelInformativeErrorResponse).toString()
          )

          val result = connector.cancelTtp(ttpCancelRequest)

          await(result.value) mustBe
            Left(
              TtpCancelInformativeError(
                apisCalled = Some(
                  List(
                    ApiStatus(
                      ApiName("API1"),
                      ApiStatusCode(200),
                      ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
                      errorResponse = None
                    )
                  )
                ),
                internalErrors = List(TtpCancelInternalError("some error that ttp is responsible for")),
                processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
              )
            )
        }
      }
    }

    ".informTtp" when {

      val ttpInformResponse: TtpInformSuccessfulResponse = TtpInformSuccessfulResponse(
        apisCalled = List(
          ApiStatus(
            name = ApiName("API1"),
            statusCode = ApiStatusCode(200),
            processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
            errorResponse = None
          )
        ),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
      )

      val ttpInformErrorResponse: TtpInformInformativeError = TtpInformInformativeError(
        apisCalled = Some(
          List(
            ApiStatus(
              name = ApiName("API1"),
              statusCode = ApiStatusCode(200),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
              errorResponse = None
            )
          )
        ),
        internalErrors = List(
          TtpInformInternalError("some error that ttp is responsible for"),
          TtpInformInternalError("another error that ttp is responsible for")
        ),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
      )

      val r1ttpInformRequest: TtpInformRequest = TtpInformRequest(
        identifications = NonEmptyList.of(
          Identification(idType = IdType("NINO"), idValue = IdValue("AB123456C"))
        ),
        paymentPlan = SaOnlyPaymentPlan(
          arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2025-01-01")),
          ttpEndDate = TtpEndDate(LocalDate.parse("2025-02-01")),
          frequency = FrequencyLowercase.Monthly,
          initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2025-01-05"))),
          initialPaymentAmount = Some(GbpPounds.createOrThrow(100.00)),
          ddiReference = Some(DdiReference("TestDDIReference"))
        ),
        instalments = NonEmptyList.of(
          SaOnlyInstalment(
            dueDate = InstalmentDueDate(LocalDate.parse("2025-01-31")),
            amountDue = GbpPounds.createOrThrow(500.00)
          )
        ),
        channelIdentifier = ChannelIdentifier.Advisor,
        transitioned = Some(TransitionedIndicator(true))
      )

      val etmpChargeRef = DebtItemChargeReference(DebtItemChargeId("etmp-id"), ChargeSourceSAOnly.ETMP)
      val cesaChargeRef = DebtItemChargeReference(DebtItemChargeId("cesa-id"), ChargeSourceSAOnly.CESA)

      val r2ttpInformRequest: TtpInformRequestR2 = TtpInformRequestR2(
        r1ttpInformRequest.identifications,
        SaOnlyPaymentPlanR2(
          r1ttpInformRequest.paymentPlan.arrangementAgreedDate,
          r1ttpInformRequest.paymentPlan.ttpEndDate,
          r1ttpInformRequest.paymentPlan.frequency,
          r1ttpInformRequest.paymentPlan.initialPaymentDate,
          r1ttpInformRequest.paymentPlan.initialPaymentAmount,
          r1ttpInformRequest.paymentPlan.ddiReference,
          NonEmptyList.of(etmpChargeRef, cesaChargeRef)
        ),
        r1ttpInformRequest.instalments,
        r1ttpInformRequest.channelIdentifier,
        r1ttpInformRequest.transitioned
      )

      "r2 is disabled" should {
        (() => featureSwitch.saRelease2Enabled).expects().returns(SaRelease2Enabled(false)).anyNumberOfTimes()
//        val r2DisabledInformRequestFormat: OFormat[InformRequest] = {
//          val localFsMock = mock[FeatureSwitch]
//
//          InformRequest.format(localFsMock)
//        }
        implicit val r1Writes: Writes[TtpInformRequest] = InformRequest.format(featureSwitch).writes(_)

        "return a successful response" in new Setup() {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/inform",
            Json.toJson(r1ttpInformRequest).toString(),
            200,
            Json.toJson(ttpInformResponse).toString()
          )

          val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r1ttpInformRequest)

          result.value.futureValue shouldBe Right(ttpInformResponse)
        }

        "parse an error response from an upstream service" in new Setup() {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/inform",
            Json.toJson(r1ttpInformRequest).toString(),
            400,
            """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
          )

          val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r1ttpInformRequest)

          result.value.futureValue shouldBe Left(ConnectorError(400, "Invalid request body"))
        }

        "handle 500 responses" in new Setup() {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/inform",
            Json.toJson(r1ttpInformRequest).toString(),
            500,
            Json.toJson(ttpInformErrorResponse).toString()
          )

          val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r1ttpInformRequest)

          val informativeError = TtpInformInformativeError(
            apisCalled = Some(ttpInformResponse.apisCalled),
            internalErrors = List(
              TtpInformInternalError("some error that ttp is responsible for"),
              TtpInformInternalError("another error that ttp is responsible for")
            ),
            processingDateTime = ttpInformResponse.processingDateTime
          )

          result.value.futureValue shouldBe Left(informativeError)
        }

        "using Internal Auth" should {
          "return a successful response" in new Setup(internalAuthEnabled = true) {
            stubPostWithResponseBodyEnsuringRequest(
              "/debts/time-to-pay/inform",
              Json.toJson(r1ttpInformRequest).toString(),
              200,
              Json.toJson(ttpInformResponse).toString()
            )

            val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r1ttpInformRequest)

            result.value.futureValue shouldBe Right(ttpInformResponse)
          }

          "return an unauthorized response from an upstream service" in new Setup(internalAuthEnabled = true) {
            stubPostWithResponseBodyEnsuringRequest(
              "/debts/time-to-pay/inform",
              Json.toJson(r1ttpInformRequest).toString(),
              401,
              """{"failures": [{"code": "401", "reason": "Unauthorized"}]}"""
            )

            val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r1ttpInformRequest)

            result.value.futureValue shouldBe Left(ConnectorError(401, "Unauthorized"))
          }

          "parse an error response from an upstream service" in new Setup(internalAuthEnabled = true) {
            stubPostWithResponseBodyEnsuringRequest(
              "/debts/time-to-pay/inform",
              Json.toJson(r1ttpInformRequest).toString(),
              400,
              """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
            )

            val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r1ttpInformRequest)

            result.value.futureValue shouldBe Left(ConnectorError(400, "Invalid request body"))
          }

          "handle 500 responses" in new Setup(internalAuthEnabled = true) {
            stubPostWithResponseBodyEnsuringRequest(
              "/debts/time-to-pay/inform",
              Json.toJson(r1ttpInformRequest).toString(),
              500,
              Json.toJson(ttpInformErrorResponse).toString()
            )

            val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r1ttpInformRequest)

            val informativeError = TtpInformInformativeError(
              apisCalled = Some(ttpInformResponse.apisCalled),
              internalErrors = List(
                TtpInformInternalError("some error that ttp is responsible for"),
                TtpInformInternalError("another error that ttp is responsible for")
              ),
              processingDateTime = ttpInformResponse.processingDateTime
            )

            result.value.futureValue shouldBe Left(informativeError)
          }
        }
      }

      "r2 is enabled" should {
        (() => featureSwitch.saRelease2Enabled).expects().returns(SaRelease2Enabled(false)).anyNumberOfTimes()
//        val r2EnabledInformRequestFormat: OFormat[InformRequest] = {
//          val localFsMock = mock[FeatureSwitch]
//          (() => localFsMock.saRelease2Enabled).expects().returns(SaRelease2Enabled(true))
//          InformRequest.format(localFsMock)
//        }
        implicit val r2Writes: Writes[TtpInformRequestR2] = InformRequest.format(featureSwitch).writes(_)

        "return a successful response" in new Setup() {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/inform",
            Json.toJson(r2ttpInformRequest).toString(),
            200,
            Json.toJson(ttpInformResponse).toString()
          )

          val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r2ttpInformRequest)

          result.value.futureValue shouldBe Right(ttpInformResponse)
        }

        "parse an error response from an upstream service" in new Setup() {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/inform",
            Json.toJson(r2ttpInformRequest).toString(),
            400,
            """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
          )

          val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r2ttpInformRequest)

          result.value.futureValue shouldBe Left(ConnectorError(400, "Invalid request body"))
        }

        "handle 500 responses" in new Setup() {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/inform",
            Json.toJson(r2ttpInformRequest).toString(),
            500,
            Json.toJson(ttpInformErrorResponse).toString()
          )

          val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r2ttpInformRequest)

          val informativeError = TtpInformInformativeError(
            apisCalled = Some(ttpInformResponse.apisCalled),
            internalErrors = List(
              TtpInformInternalError("some error that ttp is responsible for"),
              TtpInformInternalError("another error that ttp is responsible for")
            ),
            processingDateTime = ttpInformResponse.processingDateTime
          )

          result.value.futureValue shouldBe Left(informativeError)
        }

        "using Internal Auth" should {
          "return a successful response" in new Setup(internalAuthEnabled = true) {
            stubPostWithResponseBodyEnsuringRequest(
              "/debts/time-to-pay/inform",
              Json.toJson(r2ttpInformRequest).toString(),
              200,
              Json.toJson(ttpInformResponse).toString()
            )

            val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r2ttpInformRequest)

            result.value.futureValue shouldBe Right(ttpInformResponse)
          }

          "return an unauthorized response from an upstream service" in new Setup(internalAuthEnabled = true) {
            stubPostWithResponseBodyEnsuringRequest(
              "/debts/time-to-pay/inform",
              Json.toJson(r2ttpInformRequest).toString(),
              401,
              """{"failures": [{"code": "401", "reason": "Unauthorized"}]}"""
            )

            val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r2ttpInformRequest)

            result.value.futureValue shouldBe Left(ConnectorError(401, "Unauthorized"))
          }

          "parse an error response from an upstream service" in new Setup(internalAuthEnabled = true) {
            stubPostWithResponseBodyEnsuringRequest(
              "/debts/time-to-pay/inform",
              Json.toJson(r2ttpInformRequest).toString(),
              400,
              """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
            )

            val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r2ttpInformRequest)

            result.value.futureValue shouldBe Left(ConnectorError(400, "Invalid request body"))
          }

          "handle 500 responses" in new Setup(internalAuthEnabled = true) {

            stubPostWithResponseBodyEnsuringRequest(
              "/debts/time-to-pay/inform",
              Json.toJson(r2ttpInformRequest).toString(),
              500,
              Json.toJson(ttpInformErrorResponse).toString()
            )

            val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(r2ttpInformRequest)

            val informativeError = TtpInformInformativeError(
              apisCalled = Some(ttpInformResponse.apisCalled),
              internalErrors = List(
                TtpInformInternalError("some error that ttp is responsible for"),
                TtpInformInternalError("another error that ttp is responsible for")
              ),
              processingDateTime = ttpInformResponse.processingDateTime
            )

            result.value.futureValue shouldBe Left(informativeError)
          }
        }
      }
    }

    ".fullAmendTtp" should {
      val fullAmendRequest: TtpFullAmendRequest = TtpFullAmendRequest(
        identifications = NonEmptyList.of(
          Identification(idType = IdType("NINO"), idValue = IdValue("AB123456C"))
        ),
        paymentPlan = SaOnlyPaymentPlan(
          arrangementAgreedDate = ArrangementAgreedDate(LocalDate.parse("2025-01-01")),
          ttpEndDate = TtpEndDate(LocalDate.parse("2025-02-01")),
          frequency = FrequencyLowercase.Monthly,
          initialPaymentDate = Some(InitialPaymentDate(LocalDate.parse("2025-01-05"))),
          initialPaymentAmount = Some(GbpPounds.createOrThrow(100.00)),
          ddiReference = Some(DdiReference("TestDDIReference"))
        ),
        instalments = NonEmptyList.of(
          SaOnlyInstalment(
            dueDate = InstalmentDueDate(LocalDate.parse("2025-01-31")),
            amountDue = GbpPounds.createOrThrow(500.00)
          )
        ),
        channelIdentifier = ChannelIdentifier.Advisor,
        transitioned = TransitionedIndicator(true)
      )

      val fullAmendResponse: TtpFullAmendSuccessfulResponse = TtpFullAmendSuccessfulResponse(
        apisCalled = List(
          ApiStatus(
            name = ApiName("API1"),
            statusCode = ApiStatusCode(200),
            processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
            errorResponse = None
          )
        ),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
      )

      val fullAmendInformativeError = TtpFullAmendInformativeError(
        apisCalled = Some(
          List(
            ApiStatus(
              name = ApiName("API1"),
              statusCode = ApiStatusCode(200),
              processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
              errorResponse = None
            )
          )
        ),
        internalErrors = List(
          TtpFullAmendInternalError("some error that ttp is responsible for"),
          TtpFullAmendInternalError("another error that ttp is responsible for")
        ),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
      )

      "return a successful response" in new Setup() {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/full-amend",
          Json.toJson(fullAmendRequest).toString(),
          200,
          Json.toJson(fullAmendResponse).toString()
        )

        val result: TtppEnvelope[TtpFullAmendSuccessfulResponse] = connector.fullAmendTtp(fullAmendRequest)

        result.value.futureValue shouldBe Right(fullAmendResponse)
      }

      "parse an error response from an upstream service" in new Setup() {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/full-amend",
          Json.toJson(fullAmendRequest).toString(),
          400,
          """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
        )

        val result: TtppEnvelope[TtpFullAmendSuccessfulResponse] = connector.fullAmendTtp(fullAmendRequest)

        result.value.futureValue shouldBe Left(ConnectorError(400, "Invalid request body"))
      }

      "handle 500 responses" in new Setup() {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/full-amend",
          Json.toJson(fullAmendRequest).toString(),
          500,
          Json.toJson(fullAmendInformativeError).toString()
        )

        val result: TtppEnvelope[TtpFullAmendSuccessfulResponse] = connector.fullAmendTtp(fullAmendRequest)

        result.value.futureValue shouldBe Left(fullAmendInformativeError)
      }

      "using Internal Auth" should {
        "return a successful response" in new Setup(internalAuthEnabled = true) {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/full-amend",
            Json.toJson(fullAmendRequest).toString(),
            200,
            Json.toJson(fullAmendResponse).toString()
          )

          val result: TtppEnvelope[TtpFullAmendSuccessfulResponse] = connector.fullAmendTtp(fullAmendRequest)

          result.value.futureValue shouldBe Right(fullAmendResponse)
        }

        "return an unauthorized response from an upstream service" in new Setup(internalAuthEnabled = true) {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/full-amend",
            Json.toJson(fullAmendRequest).toString(),
            401,
            """{"failures": [{"code": "401", "reason": "Unauthorized"}]}"""
          )

          val result: TtppEnvelope[TtpFullAmendSuccessfulResponse] = connector.fullAmendTtp(fullAmendRequest)

          result.value.futureValue shouldBe Left(ConnectorError(401, "Unauthorized"))
        }

        "parse an error response from an upstream service" in new Setup(internalAuthEnabled = true) {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/full-amend",
            Json.toJson(fullAmendRequest).toString(),
            400,
            """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
          )

          val result: TtppEnvelope[TtpFullAmendSuccessfulResponse] = connector.fullAmendTtp(fullAmendRequest)

          result.value.futureValue shouldBe Left(ConnectorError(400, "Invalid request body"))
        }

        "handle 500 responses" in new Setup(internalAuthEnabled = true) {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/full-amend",
            Json.toJson(fullAmendRequest).toString(),
            500,
            Json.toJson(fullAmendInformativeError).toString()
          )

          val result: TtppEnvelope[TtpFullAmendSuccessfulResponse] = connector.fullAmendTtp(fullAmendRequest)

          result.value.futureValue shouldBe Left(fullAmendInformativeError)
        }
      }
    }
  }
}
