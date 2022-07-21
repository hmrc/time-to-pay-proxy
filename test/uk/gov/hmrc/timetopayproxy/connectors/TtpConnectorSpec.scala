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

package uk.gov.hmrc.timetopayproxy.connectors

import org.scalamock.scalatest.MockFactory
import org.scalatestplus.play.PlaySpec
import play.api.test.{ DefaultAwaitTimeout, FutureAwaits }
import play.api.{ ConfigLoader, Configuration }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.timetopayproxy.config.AppConfig
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.support.WireMockUtils

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class TtpConnectorSpec extends PlaySpec with DefaultAwaitTimeout with FutureAwaits with MockFactory with WireMockUtils {

  val config = mock[Configuration]
  val servicesConfig = mock[ServicesConfig]

  val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  class Setup(ifImpl: Boolean = true) {
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

    val mockConfiguration: AppConfig = new MockAppConfig(config, servicesConfig, ifImpl)

    val connector: TtpConnector = new DefaultTtpConnector(mockConfiguration, httpClient)
  }

  "Generate quote" when {
    "using IF" must {
      "parse an error response from an upstream service" in new Setup {
        stubPostWithResponseBody(
          "/individuals/debts/time-to-pay/quote",
          400,
          errorResponse("BAD_REQUEST", "Invalid request body")
        )
        val result = connector.generateQuote(
          GenerateQuoteRequest(
            CustomerReference("CustRef1234"),
            ChannelIdentifier.Advisor,
            PlanToGenerateQuote(
              QuoteType.Duration,
              LocalDate.now(),
              LocalDate.now(),
              Some(5),
              Some(Frequency.TwoWeekly),
              None,
              Some(1),
              None,
              PaymentPlanType.TimeToPay
            ),
            List.empty,
            List.empty
          )
        )

        await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
      }
    }

    "using TTP" must {
      "parse an error response from an upstream service" in new Setup(false) {
        stubPostWithResponseBody("/debts/time-to-pay/quote", 400, errorResponse("BAD_REQUEST", "Invalid request body"))
        val result = connector.generateQuote(
          GenerateQuoteRequest(
            CustomerReference("CustRef1234"),
            ChannelIdentifier.Advisor,
            PlanToGenerateQuote(
              QuoteType.Duration,
              LocalDate.now(),
              LocalDate.now(),
              Some(5),
              Some(Frequency.TwoWeekly),
              None,
              Some(1),
              None,
              PaymentPlanType.TimeToPay
            ),
            List.empty,
            List.empty
          )
        )

        await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
      }
    }
  }

  "Create plan" when {
    "using IF" must {
      "parse an error response from an upstream service" in new Setup {
        stubPostWithResponseBody(
          "/individuals/debts/time-to-pay/quote/arrangement",
          400,
          errorResponse("BAD_REQUEST", "Invalid request body")
        )
        val result = connector.createPlan(
          CreatePlanRequest(
            CustomerReference("CustRef1234"),
            QuoteReference("Quote1234"),
            ChannelIdentifier.Advisor,
            PlanToCreatePlan(
              QuoteId("Quote1234"),
              QuoteType.Duration,
              LocalDate.now(),
              LocalDate.now(),
              Some(5),
              PaymentPlanType.TimeToPay,
              false,
              1,
              Some(Frequency.TwoWeekly),
              Some(Duration(1)),
              Some(PaymentMethod.Bacs),
              Some(PaymentReference("ref123")),
              None,
              None,
              5,
              2,
              2,
              4
            ),
            Seq.empty,
            Seq.empty,
            Seq.empty,
            Seq.empty
          )
        )

        await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
      }
    }

    "using TTP" must {
      "parse an error response from an upstream service" in new Setup(false) {
        stubPostWithResponseBody(
          "/debts/time-to-pay/quote/arrangement",
          400,
          errorResponse("BAD_REQUEST", "Invalid request body")
        )
        val result = connector.createPlan(
          CreatePlanRequest(
            CustomerReference("CustRef1234"),
            QuoteReference("Quote1234"),
            ChannelIdentifier.Advisor,
            PlanToCreatePlan(
              QuoteId("Quote1234"),
              QuoteType.Duration,
              LocalDate.now(),
              LocalDate.now(),
              Some(5),
              PaymentPlanType.TimeToPay,
              false,
              1,
              Some(Frequency.TwoWeekly),
              Some(Duration(1)),
              Some(PaymentMethod.Bacs),
              Some(PaymentReference("ref123")),
              None,
              None,
              5,
              2,
              2,
              4
            ),
            Seq.empty,
            Seq.empty,
            Seq.empty,
            Seq.empty
          )
        )

        await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
      }
    }
  }

  "Receiving an unrecognised body from an upstream" when {
    "The status code is 200" must {
      "Return a TTP error" in new Setup {
        stubGetWithResponseBody(
          "/individuals/time-to-pay/quote/CustRef1234/Plan1234",
          200,
          """{ "unrecognised":"body" }"""
        )
        val result = connector.getExistingQuote(CustomerReference("CustRef1234"), PlanId("Plan1234"))

        await(result.value) mustBe Left(ConnectorError(503, "Couldn't parse body from upstream"))
      }
    }
    "The status code is 503" must {
      "Return a TTP error" in new Setup {
        stubGetWithResponseBody(
          "/individuals/time-to-pay/quote/CustRef1234/Plan1234",
          503,
          """{ "unrecognised":"body" }"""
        )
        val result = connector.getExistingQuote(CustomerReference("CustRef1234"), PlanId("Plan1234"))

        await(result.value) mustBe Left(ConnectorError(503, "Couldn't parse body from upstream"))
      }
    }
  }

  "Get plan" when {
    "using IF" must {
      "parse an error response from an upstream service" in new Setup {
        stubGetWithResponseBody(
          "/individuals/time-to-pay/quote/CustRef1234/Plan1234",
          400,
          errorResponse("BAD_REQUEST", "Invalid request body")
        )
        val result = connector.getExistingQuote(CustomerReference("CustRef1234"), PlanId("Plan1234"))

        await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
      }
    }

    "using TTP" must {
      "parse an error response from an upstream service" in new Setup(false) {
        stubGetWithResponseBody(
          "/debts/time-to-pay/quote/CustRef1234/Plan1234",
          400,
          errorResponse("BAD_REQUEST", "Invalid request body")
        )
        val result = connector.getExistingQuote(CustomerReference("CustRef1234"), PlanId("Plan1234"))

        await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
      }
    }
  }

  "Update plan" when {
    "using IF" must {
      "parse an error response from an upstream service" in new Setup {
        stubPutWithResponseBody(
          "/individuals/time-to-pay/quote/CustRef1234/PlanId1234",
          400,
          errorResponse("BAD_REQUEST", "Invalid request body")
        )
        val result = connector.updatePlan(
          UpdatePlanRequest(
            CustomerReference("CustRef1234"),
            PlanId("PlanId1234"),
            UpdateType("Cancel"),
            Some(PlanStatus.ResolvedCancelled),
            None,
            None,
            None,
            None
          )
        )

        await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
      }
    }

    "using TTP" must {
      "parse an error response from an upstream service" in new Setup(false) {
        stubPutWithResponseBody(
          "/debts/time-to-pay/quote/CustRef1234/PlanId1234",
          400,
          errorResponse("BAD_REQUEST", "Invalid request body")
        )
        val result = connector.updatePlan(
          UpdatePlanRequest(
            CustomerReference("CustRef1234"),
            PlanId("PlanId1234"),
            UpdateType("Cancel"),
            Some(PlanStatus.ResolvedCancelled),
            None,
            None,
            Some(true),
            None
          )
        )

        await(result.value) mustBe Left(ConnectorError(400, "Invalid request body"))
      }
    }
  }

  private def errorResponse(code: String, reason: String): String =
    s"""
       |{
       | "failures":[
       |   {
       |     "code":"$code",
       |     "reason":"$reason"
       |   }
       | ]
       |}
       |""".stripMargin
}
