/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.utils

import play.api.libs.json.Json
import play.api.mvc.{ Headers, RequestHeader }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger

import java.util.{ Locale, UUID }

/** Overrides the HMRC implicit value to ensure that we always have a CorrelationId as early in our logic as possible.
  *
  * The default logic to construct a HC does not log a warning when the CorrelationId has problems.
  * Also, generating the missing CorrelationId right before forwarding the requests to TTP/TTPE may cause the Proxy logs
  *   to not be labeled correctly, making debugging harder.
  */
// Because the original is an implicit, it's crucial that we don't extend BackendHeaderCarrierProvider here or elsewhere,
//   or we risk overriding the wrong logic when this one is deprecated or a similar implicit is added to another trait.
// Directly extending BackendHeaderCarrierProvider risks us overriding a trait that is never used in production.
trait HeaderCarrierWithForcedCorrelationId { this: BackendHeaderCarrierProvider =>

  protected def logger: RequestAwareLogger

  // Duplicated because we don't have access to `super` without directly extending BackendHeaderCarrierProvider.
  private object OriginalLogic extends BackendHeaderCarrierProvider {
    // Make it public.
    override def hc(implicit request: RequestHeader): HeaderCarrier = super.hc
  }

  protected override final implicit def hc(implicit originalRequest: RequestHeader): HeaderCarrier = {
    // Note that we are overriding the super implementation without calling it through `super`.
    // If the bootstrap library adds another layer that overrides BackendHeaderCarrierProvider, this will discard that logic.
    // We think that the small risk of such a thing happening is worth it if it means we get to consistently log warnings
    //   for missing CorrelationId headers, which are a frequent occurrence in `time-to-pay-proxy`.

    val loggableRequestDescription: String =
      s"""REQUEST ID ${originalRequest.id} (Not unique across instances)
         |
         |${originalRequest.method} ${originalRequest.path} ${originalRequest.version}
         |${originalRequest.headers.headers.map(headerToLoggableString).mkString("\n")}
         |""".stripMargin.trim

    originalRequest.headers.getAll("correlationid").toList match {
      case Nil =>
        val newCorrelationId: String = UUID.randomUUID().toString
        val newRequest = originalRequest.withHeaders(Headers("correlationid" -> newCorrelationId))

        val populatedHeaderCarrier: HeaderCarrier = OriginalLogic.hc(newRequest)
        logger.warn(
          s"""No correlationId found in the request headers. Generating a new one: $newCorrelationId
             |
             |$loggableRequestDescription
             |""".stripMargin
        )(populatedHeaderCarrier)

        populatedHeaderCarrier

      case _ :: Nil =>
        OriginalLogic.hc(originalRequest)

      case multiple @ _ :: _ :: _ =>
        val populatedHeaderCarrier: HeaderCarrier = OriginalLogic.hc(originalRequest)
        logger.warn(
          s"""Multiple CorrelationId headers found. Picking an arbitrary one. Found: ${Json.toJson(multiple)}
             |
             |$loggableRequestDescription
             |""".stripMargin
        )(populatedHeaderCarrier)

        populatedHeaderCarrier
    }
  }

  private def headerToLoggableString(header: (String, String)): String = {
    val safeToLogHeaders: Set[String] =
      Set("Content-Length", "Content-Type", "Host", "Accept", "user-agent").map(_.toLowerCase(Locale.UK))

    if (safeToLogHeaders.contains(header._1.toLowerCase(Locale.UK))) {
      s"${header._1}: ${header._2}"
    } else {
      s"${header._1}: <hidden>"
    }
  }
}
