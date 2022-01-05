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

import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import javax.inject.{Inject, Singleton}
import org.apache.commons.logging.LogFactory
import uk.gov.hmrc.timetopayproxy.actions.auth.AuthoriseAction

import scala.concurrent.Future

@Singleton()
class MicroserviceHelloWorldController @Inject()
  (
    authoriseAction: AuthoriseAction,
    cc: ControllerComponents
  )
  extends BackendController(cc) {
  private val logger = LogFactory.getLog(classOf[MicroserviceHelloWorldController])

  def hello(): Action[AnyContent] = authoriseAction.async { implicit request =>
    logger.info("Hello world by time-to-pay-proxy!")
    Future.successful(Ok("Hello world"))
  }
}
