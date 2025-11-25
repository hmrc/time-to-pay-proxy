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

package uk.gov.hmrc.timetopayproxy.actions.auth

import enumeratum.{ Enum, EnumEntry }
import play.api.Logger
import play.api.mvc.Results.{ Forbidden, ServiceUnavailable }
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.timetopayproxy.config.FeatureSwitch

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

sealed abstract class StoredEnrolmentScope(override val entryName: String) extends EnumEntry {

  def toEnrolment: Enrolment = Enrolment(this.entryName)
}

object StoredEnrolmentScope extends Enum[StoredEnrolmentScope] {
  def values: IndexedSeq[StoredEnrolmentScope] = findValues

  case object ReadTimeToPayProxy extends StoredEnrolmentScope("read:time-to-pay-proxy")
}

sealed abstract class AuthoriseAction(
  override val authConnector: PlayAuthConnector,
  cc: ControllerComponents,
  featureSwitch: FeatureSwitch
) extends ActionBuilder[Request, AnyContent] with AuthorisedFunctions {
  private val logger = Logger(classOf[AuthoriseAction])

  def storedEnrolmentScope: StoredEnrolmentScope

  final def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequest(request)

    val eventualResult: Future[Result] =
      if (featureSwitch.enrolmentAuthEnabled.enabled)
        authorised(storedEnrolmentScope.toEnrolment) {
          block(request)
        }(hc, cc.executionContext)
      else
        block(request)

    eventualResult.recover {
      case ie: InsufficientEnrolments =>
        logger.debug(s"Forbidden request - Insufficient Enrolments: ${ie.reason}")
        Forbidden
      case NonFatal(ex) =>
        logger.error(s"Caught an unexpected exception", ex)
        ServiceUnavailable
    }(cc.executionContext)
  }

  final def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

  protected final def executionContext: ExecutionContext = cc.executionContext

}

final class ReadAuthoriseAction @Inject() (
  override val authConnector: PlayAuthConnector,
  cc: ControllerComponents,
  featureSwitch: FeatureSwitch
) extends AuthoriseAction(authConnector: PlayAuthConnector, cc: ControllerComponents, featureSwitch: FeatureSwitch) {
  def storedEnrolmentScope: StoredEnrolmentScope = StoredEnrolmentScope.ReadTimeToPayProxy
}
