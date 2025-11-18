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

package uk.gov.hmrc.timetopayproxy.auth

import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.http.Status
import play.api.mvc.{ Action, AnyContent, InjectedController }
import play.api.test.Helpers.{ await, stubControllerComponents }
import play.api.test.{ DefaultAwaitTimeout, FakeRequest }
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{ Enrolment, InsufficientEnrolments, PlayAuthConnector }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.actions.auth.{ AuthoriseAction, ReadAuthoriseAction }
import uk.gov.hmrc.timetopayproxy.config.FeatureSwitch
import uk.gov.hmrc.timetopayproxy.models.featureSwitches.EnrolmentAuthEnabled

import scala.concurrent.{ ExecutionContext, Future }

class ReadAuthoriseActionSpec extends AnyFreeSpec with MockFactory with DefaultAwaitTimeout {
  class Harness(authAction: AuthoriseAction) extends InjectedController {
    def onPageLoad(): Action[AnyContent] = authAction { _ =>
      Ok("")
    }
  }

  lazy val mockAuthConnector: PlayAuthConnector = mock[PlayAuthConnector]
  val featureSwitch: FeatureSwitch = mock[FeatureSwitch]
  lazy val cc = stubControllerComponents()
  class Setup {
    val authAction = new ReadAuthoriseAction(mockAuthConnector, cc, featureSwitch)
    val controller = new Harness(authAction)
  }

  "ReadAuthoriseAction" - {

    // Upon construction the .authorise is called
    ".apply" - {

      "when the auth connector responds successfully" - {
        "return Ok and the user is authorised" in {
          val setup = new Setup

          val authConnectorResponse = Future.successful(())

          (() => featureSwitch.enrolmentAuthEnabled).expects().returning(EnrolmentAuthEnabled(true))
          (mockAuthConnector
            .authorise[Unit](_: Predicate, _: Retrieval[Unit])(_: HeaderCarrier, _: ExecutionContext))
            .expects(Enrolment("read:time-to-pay-proxy"), *, *, *)
            .returning(authConnectorResponse)

          val result = await(setup.controller.onPageLoad()(FakeRequest("GET", "/test")))

          result.header.status shouldBe Status.OK
        }
      }

      "when the auth connector responds with an InsufficientEnrolments exception" - {
        "return Forbidden and the user is not authorised" in {
          val setup = new Setup

          val authConnectorResponse = Future.failed(new InsufficientEnrolments)

          (() => featureSwitch.enrolmentAuthEnabled).expects().returning(EnrolmentAuthEnabled(true))
          (mockAuthConnector
            .authorise[Unit](_: Predicate, _: Retrieval[Unit])(_: HeaderCarrier, _: ExecutionContext))
            .expects(Enrolment("read:time-to-pay-proxy"), *, *, *)
            .returning(authConnectorResponse)

          val result = await(setup.controller.onPageLoad()(FakeRequest("GET", "/test")))

          result.header.status shouldBe Status.FORBIDDEN
        }
      }

      "when the auth connector responds with an InternalError exception" - {
        "return Forbidden and the user is not authorised" in {
          val setup = new Setup

          val authConnectorResponse = Future.failed(new InternalError)

          (() => featureSwitch.enrolmentAuthEnabled).expects().returning(EnrolmentAuthEnabled(true))
          (mockAuthConnector
            .authorise[Unit](_: Predicate, _: Retrieval[Unit])(_: HeaderCarrier, _: ExecutionContext))
            .expects(Enrolment("read:time-to-pay-proxy"), *, *, *)
            .returning(authConnectorResponse)

          val result = await(setup.controller.onPageLoad()(FakeRequest("GET", "/test")))

          result.header.status shouldBe Status.SERVICE_UNAVAILABLE
        }
      }

      "when enrolmentAuth is disabled" - {
        "then do not call the auth connector and return Ok" in {
          val setup = new Setup

          (() => featureSwitch.enrolmentAuthEnabled).expects().returning(EnrolmentAuthEnabled(false))

          val result = await(setup.controller.onPageLoad()(FakeRequest("GET", "/test")))

          result.header.status shouldBe Status.OK
        }
      }
    }
  }
}
