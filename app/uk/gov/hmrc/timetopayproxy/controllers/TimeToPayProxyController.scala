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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, BaseController, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.timetopayproxy.actions.auth.AuthoriseAction
import uk.gov.hmrc.timetopayproxy.models.{CustomerReference, GenerateQuoteRequest, PlanId, UpdateQuoteRequest}
import uk.gov.hmrc.timetopayproxy.models.TimeToPayErrorResponse._
import uk.gov.hmrc.timetopayproxy.services.TTPQuoteService
import uk.gov.hmrc.timetopayproxy.utils.TtppErrorHandler._
import uk.gov.hmrc.timetopayproxy.utils.TtppResultConverter._
import uk.gov.hmrc.timetopayproxy.models.GenerateQuoteResponse._

@Singleton()
class TimeToPayProxyController @Inject()(authoriseAction: AuthoriseAction,
                                         cc: ControllerComponents,
                                         timeToPayProxyService: TTPQuoteService)
    extends BackendController(cc)
    with BaseController {
  implicit val ec = cc.executionContext

  def generateQuote: Action[JsValue] = authoriseAction.async(parse.json) {
    implicit request =>
      withJsonBody[GenerateQuoteRequest] {
        timeToPayRequest: GenerateQuoteRequest => {
          timeToPayProxyService
            .generateQuote(timeToPayRequest)
            .leftMap(ttppError => ttppError.toErrorResponse)
            .fold(e => e.toResult, r => r.toResult)
        }
      }
  }

  def getExistingPlan(customerReference: String, planId: String) =
    authoriseAction.async { implicit request =>
      timeToPayProxyService
        .getExistingPlan(CustomerReference(customerReference), PlanId(planId))
        .leftMap(ttppError => ttppError.toErrorResponse)
        .fold(e => e.toResult, r => r.toResult)
    }

  def updateQuote(customerReference: String, planId: String): Action[JsValue] = authoriseAction.async(parse.json) {
    implicit request =>
      withJsonBody[UpdateQuoteRequest] {
        updateQuoteRequest: UpdateQuoteRequest => {
          timeToPayProxyService
            .updateQuote(updateQuoteRequest)
            .leftMap(ttppError => ttppError.toErrorResponse)
            .fold(e => e.toResult, r => r.toResult)
        }
      }
  }
}
