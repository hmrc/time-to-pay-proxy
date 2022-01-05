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

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.mvc.ControllerComponents
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.actions.auth.{AuthoriseAction, AuthoriseActionImpl}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class MicroserviceHelloWorldControllerSpec extends AnyWordSpec with Matchers with MockFactory {

  private val fakeRequest = FakeRequest("GET", "/")
  private val authConnector: PlayAuthConnector = mock[PlayAuthConnector]
  (authConnector.authorise[Unit](_: Predicate, _: Retrieval[Unit])(_: HeaderCarrier, _: ExecutionContext))
    .expects(*, *, *, *)
    .returning(Future.successful())
  private val cc: ControllerComponents = Helpers.stubControllerComponents()
  private val authoriseAction: AuthoriseAction = new AuthoriseActionImpl(authConnector, cc)

  private val controller = new MicroserviceHelloWorldController(authoriseAction, cc)

  "GET /" should {
    "return 200" in {
      val result = controller.hello()(fakeRequest)
      status(result) shouldBe Status.OK
    }
  }
}
