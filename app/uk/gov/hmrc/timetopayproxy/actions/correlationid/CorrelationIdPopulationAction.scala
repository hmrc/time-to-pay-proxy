/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.actions.correlationid

import play.api.mvc.{ ActionTransformer, Request }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class CorrelationIdPopulationAction @Inject() ()(implicit ec: ExecutionContext) extends ActionTransformer[Request, Request] {
  private val logger = new RequestAwareLogger(this.getClass)

  override protected def transform[A](request: Request[A]): Future[Request[A]] = {
    val baseHeaderCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    request.headers.get("correlationId") match {
      case Some(_) =>
        Future.successful(request)
      case None =>
        val generatedCorrelationId = UUID.randomUUID().toString
        logger.warn(s"No correlationId found in request header for ${request.uri}, generated: $generatedCorrelationId")(
          baseHeaderCarrier
        )
        Future.successful(request.withHeaders(request.headers.add("correlationId" -> generatedCorrelationId)))
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
