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

package uk.gov.hmrc.timetopayproxy.auth

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{Action, AnyContent, InjectedController}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.timetopayproxy.actions.auth.{AuthoriseAction, AuthoriseActionImpl}
import uk.gov.hmrc.timetopayproxy.support.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, PlayAuthConnector, SessionRecordNotFound}
import uk.gov.hmrc.http.HeaderCarrier
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, PlayAuthConnector, SessionRecordNotFound}
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers.stubControllerComponents

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class Harness(authAction: AuthoriseAction) extends InjectedController {
  def onPageLoad(): Action[AnyContent] = authAction { _ =>
    Ok("")
  }
}

class AuthoriseActionSpec extends UnitSpec {
  lazy val mockAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]
  lazy val cc = stubControllerComponents()
  class Setup(val authConnectorResponse: Future[Unit]) {
    (mockAuthConnector.authorise[Unit](_: Predicate, _: Retrieval[Unit])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(authConnectorResponse)
    val authAction = new AuthoriseActionImpl(mockAuthConnector, cc)
    val controller = new Harness(authAction)
  }
  "A user with no active session" should {
    "return 401 response" in {
      val authConnectorResponse = Future.failed(new SessionRecordNotFound)
      val setup = new Setup(authConnectorResponse)

      val result = setup.controller.onPageLoad()(FakeRequest("", ""))

      result.map(_.header.status shouldBe Status.UNAUTHORIZED)
    }
  }
  "A user with an active session" should {
    "return the request when the user is authorised" in {
      val authConnectorResponse = Future.successful(())
      val setup = new Setup(authConnectorResponse)

      val result = await(setup.controller.onPageLoad()(FakeRequest("GET", "/test")))

      result.header.status shouldBe Status.OK
    }
    "return Forbidden when the enrolment is not present" in {
      val authConnectorResponse = Future.failed(new InsufficientEnrolments)
      val setup = new Setup(authConnectorResponse)

      val result = await(setup.controller.onPageLoad()(FakeRequest("GET", "/test")))

      result.header.status shouldBe Status.FORBIDDEN
    }

  }
  "A user with no active session" should {
    "be able to access the TTPP endpoints" when {
      "authentication is disabled" in {
        val authConnectorResponse = Future.successful(())
        val setup = new Setup(authConnectorResponse)

        val result = await(setup.controller.onPageLoad()(FakeRequest("GET", "/test")))

        result.header.status shouldBe Status.OK
      }
    }
  }
}
