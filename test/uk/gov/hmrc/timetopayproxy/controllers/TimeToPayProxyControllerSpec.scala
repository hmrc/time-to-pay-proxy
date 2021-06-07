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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers.status
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.actions.auth.{AuthoriseAction, AuthoriseActionImpl}
import uk.gov.hmrc.timetopayproxy.services.GenerateQuoteService
import uk.gov.hmrc.timetopayproxy.models.TimeToPayRequest
import uk.gov.hmrc.timetopayproxy.models._
import play.api.test.{FakeRequest, Helpers}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._
import cats.syntax.either._

class TimeToPayProxyControllerSpec extends AnyWordSpec with Matchers with MockFactory {


  private val authConnector: PlayAuthConnector = mock[PlayAuthConnector]

  private val cc: ControllerComponents = Helpers.stubControllerComponents()
  private val authoriseAction: AuthoriseAction = new AuthoriseActionImpl(authConnector, cc)

  private val generateQuoteService = mock[GenerateQuoteService]
  private val controller = new TimeToPayProxyController(authoriseAction, cc, generateQuoteService)

  private val timeToPayRequest = TimeToPayRequest(
    "customerReference",
    10,
    List(Customer("quoteType", "2021-01-01", 1, "", "", 1, LocalDate.now(), "paymentPlanType")),
    List()
  )

  "POST /" should {
    "return 200" when {
      "service returns success" in {

        (authConnector.authorise[Unit](_: Predicate, _: Retrieval[Unit])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val responseFromTtp = TimeToPayResponse(
          "quoteReference",
          "customerReference",
          "quoteStatus",
          "quoteType",
          List(Payment("2021-01-01", 1)),
          1,
          "",
          0.1,
          1
        )
        (generateQuoteService.generateQuote(
          _: TimeToPayRequest
        )
        (
          _: ExecutionContext,
          _: HeaderCarrier
        )
          )
          .expects(timeToPayRequest, *, *)
          .returning(TtppEnvelope(responseFromTtp))

        val fakeRequest: FakeRequest[JsValue] = FakeRequest("POST", "/individuals/time-to-pay/quote").withHeaders(CONTENT_TYPE -> MimeTypes.JSON).withBody(Json.toJson[TimeToPayRequest](timeToPayRequest))
        val response: Future[Result] = controller.generateQuote()(fakeRequest)
        status(response) shouldBe Status.OK
      }
    }
    "return 500" when {
      "service returns failure" in {

        (authConnector.authorise[Unit](_: Predicate, _: Retrieval[Unit])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returning(Future.successful())

        val errorFromTtpConnector = ConnectorError(500, "Internal Service Error")
        (generateQuoteService.generateQuote(
          _: TimeToPayRequest
        )
        (
          _: ExecutionContext,
          _: HeaderCarrier
        )
          )
          .expects(timeToPayRequest, *, *)
          .returning(TtppEnvelope(errorFromTtpConnector.asLeft[TimeToPayResponse]))

        val fakeRequest: FakeRequest[JsValue] = FakeRequest("POST", "/individuals/time-to-pay/quote").withHeaders(CONTENT_TYPE -> MimeTypes.JSON).withBody(Json.toJson[TimeToPayRequest](timeToPayRequest))
        val response: Future[Result] = controller.generateQuote()(fakeRequest)

        println(contentAsJson(response))
        status(response) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
