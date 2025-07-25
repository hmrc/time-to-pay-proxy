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

import cats.syntax.either._
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.timetopayproxy.actions.auth.AuthoriseAction
import uk.gov.hmrc.timetopayproxy.config.FeatureSwitch
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.TtppErrorResponse._
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.affordablequotes.AffordableQuotesRequest
import uk.gov.hmrc.timetopayproxy.models.chargeInfoApi.ChargeInfoRequest
import uk.gov.hmrc.timetopayproxy.services.{ TTPEService, TTPQuoteService }
import uk.gov.hmrc.timetopayproxy.utils.TtppErrorHandler._
import uk.gov.hmrc.timetopayproxy.utils.TtppResponseConverter._

import javax.inject.{ Inject, Singleton }
import scala.annotation.unused
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

@Singleton()
class TimeToPayProxyController @Inject() (
  authoriseAction: AuthoriseAction,
  cc: ControllerComponents,
  timeToPayQuoteService: TTPQuoteService,
  timeToPayEligibilityService: TTPEService,
  @unused
  fs: FeatureSwitch
) extends BackendController(cc) with BaseController {
  implicit val ec: ExecutionContext = cc.executionContext

  private val queryParameterNotMatchingPayload =
    "customerReference and planId in the query parameters should match the ones in the request payload"

  def generateQuote: Action[JsValue] = authoriseAction.async(parse.json) { implicit request =>
    withJsonBody[GenerateQuoteRequest] { timeToPayRequest: GenerateQuoteRequest =>
      timeToPayQuoteService
        .generateQuote(timeToPayRequest, request.queryString)
        .leftMap(ttppError => ttppError.toErrorResponse)
        .fold(e => e.toResponse, r => r.toResponse)
    }
  }

  def viewPlan(customerReference: String, planId: String) =
    authoriseAction.async { implicit request =>
      timeToPayQuoteService
        .getExistingPlan(CustomerReference(customerReference), PlanId(planId))
        .leftMap(ttppError => ttppError.toErrorResponse)
        .fold(e => e.toResponse, r => r.toResponse)
    }

  def updatePlan(customerReference: String, planId: String): Action[JsValue] =
    authoriseAction.async(parse.json) { implicit request =>
      withJsonBody[UpdatePlanRequest] { updatePlanRequest: UpdatePlanRequest =>
        val result = for {
          validatedUpdatePlanRequest <-
            validateUpdateRequestMatchesQueryParams(customerReference, planId, updatePlanRequest)
          response <- timeToPayQuoteService.updatePlan(validatedUpdatePlanRequest)
        } yield response

        result
          .leftMap(ttppError => ttppError.toErrorResponse)
          .fold(e => e.toResponse, r => r.toResponse)
      }
    }

  def createPlan = authoriseAction.async(parse.json) { implicit request =>
    withJsonBody[CreatePlanRequest] { createPlanRequest: CreatePlanRequest =>
      timeToPayQuoteService
        .createPlan(createPlanRequest, request.queryString)
        .leftMap(ttppError => ttppError.toErrorResponse)
        .fold(e => e.toResponse, r => r.toResponse)
    }
  }

  def getAffordableQuotes = authoriseAction.async(parse.json) { implicit request =>
    withJsonBody[AffordableQuotesRequest] { affordableQuoteRequest: AffordableQuotesRequest =>
      timeToPayQuoteService
        .getAffordableQuotes(affordableQuoteRequest)
        .leftMap(ttppError => ttppError.toErrorResponse)
        .fold(e => e.toResponse, r => r.toResponse)
    }
  }

  def checkChargeInfo: Action[JsValue] = authoriseAction.async(parse.json) { implicit request =>
    withJsonBody[ChargeInfoRequest] { chargeInfoRequest: ChargeInfoRequest =>
      timeToPayEligibilityService
        .checkChargeInfo(chargeInfoRequest)
        .leftMap(ttppError => ttppError.toErrorResponse)
        .fold(e => e.toResponse, r => r.toResponse)
    }
  }

  private def validateUpdateRequestMatchesQueryParams(
    customerReference: String,
    planId: String,
    updatePlanRequest: UpdatePlanRequest
  ): TtppEnvelope[UpdatePlanRequest] =
    (updatePlanRequest.customerReference, updatePlanRequest.planId) match {
      case (CustomerReference(cr), PlanId(pid)) if (cr.trim == customerReference) && (pid.trim == planId) =>
        TtppEnvelope(updatePlanRequest)
      case _ => TtppEnvelope(ValidationError(queryParameterNotMatchingPayload).asLeft[UpdatePlanRequest])
    }
  private def extractFieldFromJsPath(jsPath: JsPath): String =
    s"${jsPath.path.reverse.headOption.fold("-")(_.toString.replace("/", ""))}"
  private def generateReadableMessageFromError(errs: Seq[(JsPath, Seq[JsonValidationError])]): String = {

    val fieldInfo = errs.headOption
      .map { x =>
        val (jsPath, _) = x
        s"Field name: ${extractFieldFromJsPath(jsPath)}"
      }
      .getOrElse("")

    val detailedMessageMaybe = for {
      (_, valErrors)      <- errs.headOption
      jsonValidationError <- valErrors.headOption
      message             <- jsonValidationError.messages.headOption
    } yield message match {
      case m if m.startsWith("error.expected.date.isoformat") => "Date format should be correctly provided"
      case m if m.startsWith("error.expected.validenumvalue") => "Valid enum value should be provided"
      case _                                                  => ""
    }
    val detailedMessage = detailedMessageMaybe.getOrElse("")
    s"$fieldInfo. $detailedMessage"
  }

  def withJsonBody[T](
    f: T => Future[Result]
  )(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]): Future[Result] =
    Try(request.body.validate[T]) match {
      case Success(JsSuccess(payload, _)) => f(payload)

      case Success(JsError(errs)) =>
        Future.successful(
          TtppErrorResponse(
            BAD_REQUEST.intValue(),
            s"Invalid ${m.runtimeClass.getSimpleName} payload: Payload has a missing field or an invalid format. ${generateReadableMessageFromError(
                errs.toSeq.map(err => (err._1, err._2.toSeq))
              )}"
          ).toResponse
        )
      case Failure(e) =>
        Future.successful(
          TtppErrorResponse(
            BAD_REQUEST.intValue(),
            s"Could not parse body due to ${e.getMessage}"
          ).toResponse
        )
    }

}
