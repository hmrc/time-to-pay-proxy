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
import play.api.libs.json.Json
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }
import play.api.{ ConfigLoader, Configuration }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.timetopayproxy.config.AppConfig
import uk.gov.hmrc.timetopayproxy.models.currency.GbpPounds
import uk.gov.hmrc.timetopayproxy.models.error.ConnectorError
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.saonly.common.apistatus.{ ApiName, ApiStatus, ApiStatusCode }
import uk.gov.hmrc.timetopayproxy.models.saonly.common.{ ArrangementAgreedDate, InitialPaymentDate, ProcessingDateTimeInstant, SaOnlyInstalment, TransitionedIndicator, TtpEndDate }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel.{ CancellationDate, TtpCancelInformativeError, TtpCancelPaymentPlan, TtpCancelRequest, TtpCancelSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform.{ DdiReference, TtpInformInformativeError, TtpInformPaymentPlan, TtpInformRequest, TtpInformSuccessfulResponse }
import uk.gov.hmrc.timetopayproxy.models.{ ChannelIdentifier, FrequencyLowercase, IdType, IdValue, Identification, InstalmentDueDate }
import uk.gov.hmrc.timetopayproxy.support.WireMockUtils

import java.time.{ Instant, LocalDate }
import scala.concurrent.ExecutionContext

final class DefaultTtpFeedbackLoopConnectorSpec
    extends PlaySpec with DefaultAwaitTimeout with FutureAwaits with MockFactory with WireMockUtils {

  val config = mock[Configuration]
  val servicesConfig = mock[ServicesConfig]

  val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  class Setup(ifImpl: Boolean) {
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
      .expects("microservice.services.ttp.useIf", *)
      .once()
      .returns(ifImpl)
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

    val mockConfiguration: AppConfig = new MockAppConfig(config, servicesConfig, ifImpl)

    val connector: TtpFeedbackLoopConnector = new TtpFeedbackLoopConnector(mockConfiguration, httpClient)
  }

  "DefaultTtpFeedbackLoopConnector" should {
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
            statusCode = ApiStatusCode("SUCCESS"),
            processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
            errorResponse = None
          )
        ),
        processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
      )

      "using IF" should {
        "return a successful response" in new Setup(ifImpl = true) {
          stubPostWithResponseBodyEnsuringRequest(
            "/individuals/debts/time-to-pay/cancel",
            Json.toJson(ttpCancelRequest).toString(),
            200,
            Json.toJson(ttpCancelResponse).toString()
          )

          val result = connector.cancelTtp(ttpCancelRequest)

          await(result.value) mustBe Right(ttpCancelResponse: TtpCancelSuccessfulResponse)
        }

        "parse an error response from an upstream service" in new Setup(ifImpl = true) {
          stubPostWithResponseBodyEnsuringRequest(
            "/individuals/debts/time-to-pay/cancel",
            Json.toJson(ttpCancelRequest).toString(),
            400,
            """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
          )

          val result = connector.cancelTtp(ttpCancelRequest)

          await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
        }

        "handle 500 responses" in new Setup(ifImpl = true) {
          stubPostWithResponseBodyEnsuringRequest(
            "/individuals/debts/time-to-pay/cancel",
            Json.toJson(ttpCancelRequest).toString(),
            500,
            Json.toJson(ttpCancelResponse).toString()
          )

          val result = connector.cancelTtp(ttpCancelRequest)

          await(result.value) mustBe
            Left(
              TtpCancelInformativeError(
                List(
                  ApiStatus(
                    ApiName("API1"),
                    ApiStatusCode("SUCCESS"),
                    ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
                    errorResponse = None
                  )
                ),
                ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
              )
            )
        }
      }

      "using TTP" should {
        "return a successful response" in new Setup(ifImpl = false) {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/cancel",
            Json.toJson(ttpCancelRequest).toString(),
            200,
            Json.toJson(ttpCancelResponse).toString()
          )

          val result = connector.cancelTtp(ttpCancelRequest)

          await(result.value) mustBe Right(ttpCancelResponse: TtpCancelSuccessfulResponse)
        }

        "parse an error response from an upstream service" in new Setup(ifImpl = false) {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/cancel",
            Json.toJson(ttpCancelRequest).toString(),
            400,
            """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
          )

          val result = connector.cancelTtp(ttpCancelRequest)

          await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
        }

        "handle 500 responses" in new Setup(ifImpl = false) {
          stubPostWithResponseBodyEnsuringRequest(
            "/debts/time-to-pay/cancel",
            Json.toJson(ttpCancelRequest).toString(),
            500,
            Json.toJson(ttpCancelResponse).toString()
          )

          val result = connector.cancelTtp(ttpCancelRequest)

          await(result.value) mustBe
            Left(
              TtpCancelInformativeError(
                List(
                  ApiStatus(
                    ApiName("API1"),
                    ApiStatusCode("SUCCESS"),
                    ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
                    errorResponse = None
                  )
                ),
                ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
              )
            )
        }
      }
    }
  }

  ".informTtp" should {

    val ttpInformRequest: TtpInformRequest = TtpInformRequest(
      identifications = NonEmptyList.of(
        Identification(idType = IdType("NINO"), idValue = IdValue("AB123456C"))
      ),
      paymentPlan = TtpInformPaymentPlan(
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

    val ttpInformResponse: TtpInformSuccessfulResponse = TtpInformSuccessfulResponse(
      apisCalled = List(
        ApiStatus(
          name = ApiName("API1"),
          statusCode = ApiStatusCode("SUCCESS"),
          processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z")),
          errorResponse = None
        )
      ),
      processingDateTime = ProcessingDateTimeInstant(Instant.parse("2025-01-01T12:00:00Z"))
    )

    "using IF" should {
      "return a successful response" in new Setup(ifImpl = true) {
        stubPostWithResponseBodyEnsuringRequest(
          "/individuals/debts/time-to-pay/inform",
          Json.toJson(ttpInformRequest).toString(),
          200,
          Json.toJson(ttpInformResponse).toString()
        )

        val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(ttpInformRequest)

        result.value.futureValue shouldBe Right(ttpInformResponse)
      }

      "parse an error response from an upstream service" in new Setup(ifImpl = true) {
        stubPostWithResponseBodyEnsuringRequest(
          "/individuals/debts/time-to-pay/inform",
          Json.toJson(ttpInformRequest).toString(),
          400,
          """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
        )

        val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(ttpInformRequest)

        result.value.futureValue shouldBe Left(ConnectorError(400, "Invalid request body"))
      }

      "handle 500 responses" in new Setup(ifImpl = true) {
        stubPostWithResponseBodyEnsuringRequest(
          "/individuals/debts/time-to-pay/inform",
          Json.toJson(ttpInformRequest).toString(),
          500,
          Json.toJson(ttpInformResponse).toString()
        )

        val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(ttpInformRequest)

        val informativeError = TtpInformInformativeError(
          apisCalled = ttpInformResponse.apisCalled,
          processingDateTime = ttpInformResponse.processingDateTime
        )

        result.value.futureValue shouldBe Left(informativeError)
      }
    }

    "using TTP" should {
      "return a successful response" in new Setup(ifImpl = false) {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/inform",
          Json.toJson(ttpInformRequest).toString(),
          200,
          Json.toJson(ttpInformResponse).toString()
        )

        val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(ttpInformRequest)

        result.value.futureValue shouldBe Right(ttpInformResponse)
      }

      "parse an error response from an upstream service" in new Setup(ifImpl = false) {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/inform",
          Json.toJson(ttpInformRequest).toString(),
          400,
          """{"failures": [{"code": "400", "reason": "Invalid request body"}]}"""
        )

        val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(ttpInformRequest)

        result.value.futureValue shouldBe Left(ConnectorError(400, "Invalid request body"))
      }

      "handle 500 responses" in new Setup(ifImpl = false) {
        stubPostWithResponseBodyEnsuringRequest(
          "/debts/time-to-pay/inform",
          Json.toJson(ttpInformRequest).toString(),
          500,
          Json.toJson(ttpInformResponse).toString()
        )

        val result: TtppEnvelope[TtpInformSuccessfulResponse] = connector.informTtp(ttpInformRequest)

        val informativeError = TtpInformInformativeError(
          apisCalled = ttpInformResponse.apisCalled,
          processingDateTime = ttpInformResponse.processingDateTime
        )

        result.value.futureValue shouldBe Left(informativeError)
      }
    }
  }
}
