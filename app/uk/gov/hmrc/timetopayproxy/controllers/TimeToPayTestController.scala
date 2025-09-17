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

package uk.gov.hmrc.timetopayproxy.controllers

import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContent, BaseController, ControllerComponents, Results }
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.timetopayproxy.models.RequestDetails
import uk.gov.hmrc.timetopayproxy.services.TTPTestService

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

// $COVERAGE-OFF$
//Coverage disabled for non-prod source
@Singleton()
class TimeToPayTestController @Inject() (cc: ControllerComponents, ttpTestService: TTPTestService)
    extends BackendController(cc) with BaseController {
  implicit val ec: ExecutionContext = cc.executionContext

  def requests: Action[AnyContent] = Action.async { implicit request =>
    ttpTestService
      .retrieveRequestDetails()
      .leftMap(ttppError => ttppError.toWriteableProxyError)
      .fold(e => e.toErrorResult, r => Results.Ok(Json.toJson(r)))
  }

  def deleteRequest(requestId: String): Action[AnyContent] = Action.async { implicit request =>
    ttpTestService
      .deleteRequestDetails(requestId)
      .leftMap(ttppError => ttppError.toWriteableProxyError)
      .fold(e => e.toErrorResult, _ => Results.Ok)
  }

  def response: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[RequestDetails] { details: RequestDetails =>
      ttpTestService
        .saveResponseDetails(details)
        .leftMap(ttppError => ttppError.toWriteableProxyError)
        .fold(e => e.toErrorResult, _ => Results.Ok)
    }
  }

  def saveError(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[RequestDetails] { details: RequestDetails =>
      ttpTestService
        .saveError(details)
        .leftMap(ttppError => ttppError.toWriteableProxyError)
        .fold(e => e.toErrorResult, _ => Results.Ok)
    }
  }

  def getErrors(): Action[AnyContent] = Action.async { implicit request =>
    ttpTestService
      .getErrors()
      .leftMap(ttppError => ttppError.toWriteableProxyError)
      .fold(e => e.toErrorResult, r => Results.Ok(Json.toJson(r)))
  }
}
// $COVERAGE-ON$
