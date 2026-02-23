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
import uk.gov.hmrc.timetopayproxy.actions.auth.ReadAuthoriseAction
import uk.gov.hmrc.timetopayproxy.config.FeatureSwitch
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.models.affordablequotes.AffordableQuotesRequest
import uk.gov.hmrc.timetopayproxy.models.error.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.error.{ TtppEnvelope, TtppErrorResponse, ValidationError }
import uk.gov.hmrc.timetopayproxy.models.saonly.chargeInfoApi.{ ChargeInfoRequest, ChargeInfoResponse }
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpcancel.TtpCancelRequest
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpfullamend.TtpFullAmendRequest
import uk.gov.hmrc.timetopayproxy.models.saonly.ttpinform.InformRequest
import uk.gov.hmrc.timetopayproxy.services.{ TTPEService, TTPQuoteService, TtpFeedbackLoopService }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

@Singleton()
class TimeToPayProxyController @Inject() (
  readAuthoriseAction: ReadAuthoriseAction,
  cc: ControllerComponents,
  timeToPayQuoteService: TTPQuoteService,
  ttpFeedbackLoopService: TtpFeedbackLoopService,
  timeToPayEligibilityService: TTPEService,
  featureSwitch: FeatureSwitch
) extends BackendController(cc) with BaseController {
  implicit val ec: ExecutionContext = cc.executionContext

  private val queryParameterNotMatchingPayload =
    "customerReference and planId in the query parameters should match the ones in the request payload"

  def generateQuote: Action[JsValue] = readAuthoriseAction.async(parse.json) { implicit request =>
    withJsonBody[GenerateQuoteRequest] { timeToPayRequest: GenerateQuoteRequest =>
      timeToPayQuoteService
        .generateQuote(timeToPayRequest, request.queryString)
        .leftMap(ttppError => ttppError.toWriteableProxyError)
        .fold(e => e.toErrorResult, r => Results.Ok(Json.toJson(r)))
    }
  }

  def viewPlan(customerReference: String, planId: String) =
    readAuthoriseAction.async { implicit request =>
      timeToPayQuoteService
        .getExistingPlan(CustomerReference(customerReference), PlanId(planId))
        .leftMap(ttppError => ttppError.toWriteableProxyError)
        .fold(e => e.toErrorResult, r => Results.Ok(Json.toJson(r)))
    }

  def updatePlan(customerReference: String, planId: String): Action[JsValue] =
    readAuthoriseAction.async(parse.json) { implicit request =>
      withJsonBody[UpdatePlanRequest] { updatePlanRequest: UpdatePlanRequest =>
        val result = for {
          validatedUpdatePlanRequest <-
            validateUpdateRequestMatchesQueryParams(customerReference, planId, updatePlanRequest)
          response <- timeToPayQuoteService.updatePlan(validatedUpdatePlanRequest)
        } yield response

        result
          .leftMap(ttppError => ttppError.toWriteableProxyError)
          .fold(e => e.toErrorResult, r => Results.Ok(Json.toJson(r)))
      }
    }

  def createPlan = readAuthoriseAction.async(parse.json) { implicit request =>
    withJsonBody[CreatePlanRequest] { createPlanRequest: CreatePlanRequest =>
      timeToPayQuoteService
        .createPlan(createPlanRequest, request.queryString)
        .leftMap(ttppError => ttppError.toWriteableProxyError)
        .fold(e => e.toErrorResult, r => Results.Ok(Json.toJson(r)))
    }
  }

  def getAffordableQuotes = readAuthoriseAction.async(parse.json) { implicit request =>
    withJsonBody[AffordableQuotesRequest] { affordableQuoteRequest: AffordableQuotesRequest =>
      timeToPayQuoteService
        .getAffordableQuotes(affordableQuoteRequest)
        .leftMap(ttppError => ttppError.toWriteableProxyError)
        .fold(e => e.toErrorResult, r => Results.Ok(Json.toJson(r)))
    }
  }

  def checkChargeInfo: Action[JsValue] = readAuthoriseAction.async(parse.json) { implicit request =>
    if (featureSwitch.chargeInfoEndpointEnabled) {
      withJsonBody[ChargeInfoRequest] { chargeInfoRequest: ChargeInfoRequest =>
        timeToPayEligibilityService
          .checkChargeInfo(chargeInfoRequest)
          .leftMap(ttppError => ttppError.toWriteableProxyError)
          .fold(e => e.toErrorResult, r => Results.Ok(Json.toJson(r)(ChargeInfoResponse.writes)))
      }
    } else {
      Future.successful(
        TtppErrorResponse(
          statusCode = 503,
          errorMessage = "/charge-info endpoint is not currently enabled"
        ).toErrorResult
      )
    }
  }

  def cancelTtp: Action[JsValue] = readAuthoriseAction.async(parse.json) { implicit request =>
    if (featureSwitch.cancelEndpointEnabled) {
      withJsonBody[TtpCancelRequest] { deserialisedRequest: TtpCancelRequest =>
        ttpFeedbackLoopService
          .cancelTtp(deserialisedRequest)
          .leftMap(ttppError => ttppError.toWriteableProxyError)
          .fold(e => e.toErrorResult, r => Results.Ok(Json.toJson(r)))
      }
    } else {
      Future.successful(
        TtppErrorResponse(statusCode = 503, errorMessage = "/cancel endpoint is not currently enabled").toErrorResult
      )
    }
  }

  def informTtp: Action[JsValue] = readAuthoriseAction.async(parse.json) { implicit request =>
    if (featureSwitch.informEndpointEnabled) {
      implicit val requestFormat = InformRequest.format(featureSwitch)
      withJsonBody[InformRequest] { deserialisedRequest: InformRequest =>
        ttpFeedbackLoopService
          .informTtp(deserialisedRequest)
          .leftMap(ttppError => ttppError.toWriteableProxyError)
          .fold(e => e.toErrorResult, r => Results.Ok(Json.toJson(r)))
      }
    } else {
      Future.successful(
        TtppErrorResponse(statusCode = 503, errorMessage = "/inform endpoint is not currently enabled").toErrorResult
      )
    }
  }

  def fullAmendTtp: Action[JsValue] = readAuthoriseAction.async(parse.json) { implicit request =>
    if (featureSwitch.fullAmendEndpointEnabled) {
      withJsonBody[TtpFullAmendRequest] { deserialisedRequest: TtpFullAmendRequest =>
        ttpFeedbackLoopService
          .fullAmendTtp(deserialisedRequest)
          .leftMap(ttppError => ttppError.toWriteableProxyError)
          .fold(e => e.toErrorResult, r => Results.Ok(Json.toJson(r)))
      }
    } else {
      Future.successful(
        TtppErrorResponse(
          statusCode = 503,
          errorMessage = "/full-amend endpoint is not currently enabled"
        ).toErrorResult
      )
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
          ).toErrorResult
        )
      case Failure(e) =>
        Future.successful(
          TtppErrorResponse(
            BAD_REQUEST.intValue(),
            s"Could not parse body due to ${e.getMessage}"
          ).toErrorResult
        )
    }

}
