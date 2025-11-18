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

import com.google.inject.ImplementedBy
import enumeratum.{ Enum, EnumEntry }
import play.api.Logger
import play.api.mvc.Results.{ Forbidden, ServiceUnavailable }
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.timetopayproxy.actions.auth.StoredEnrolmentScope.DtdEnrolments.ReadTimeToPayProxy

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

sealed abstract class StoredEnrolmentScope(override val entryName: String) extends EnumEntry

object StoredEnrolmentScope extends Enum[StoredEnrolmentScope] {
  def values: IndexedSeq[StoredEnrolmentScope] = findValues

  object DtdEnrolments {
    case object ReadTimeToPayProxy extends StoredEnrolmentScope("read:time-to-pay-proxy")
  }
}

@ImplementedBy(classOf[AuthoriseActionImpl])
trait AuthoriseAction extends ActionBuilder[Request, AnyContent]

class AuthoriseActionImpl @Inject() (override val authConnector: PlayAuthConnector, cc: ControllerComponents)
    extends AuthoriseAction with AuthorisedFunctions {
  private val logger = Logger(classOf[AuthoriseActionImpl])
  override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequest(request)

    val result = authorised(Enrolment(ReadTimeToPayProxy.entryName)) {
      block(request)
    }(hc, cc.executionContext)

    result.recover {
      case ie: InsufficientEnrolments =>
        logger.debug(s"Forbidden request - Insufficient Enrolments: ${ie.reason}")
        Forbidden
      case NonFatal(ex) =>
        logger.error(s"Caught an unexpected exception", ex)
        ServiceUnavailable
    }(cc.executionContext)
  }

  override protected def executionContext: ExecutionContext = cc.executionContext

}
