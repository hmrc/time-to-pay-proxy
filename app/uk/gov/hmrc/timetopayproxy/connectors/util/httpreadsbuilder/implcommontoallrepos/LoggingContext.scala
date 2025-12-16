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

package uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder.implcommontoallrepos

import play.api.http.Status
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger

/** This has all the information we need in order to effectively log yet-unknown requests and responses.
  * The `E` is contravariant because this class is only responsible for logging existing errors.
  */
/* ℹ️ Note about updating this file: ℹ️
 * This file must be kept consistent with every copy in the other debt-transformation repos.
 * The most complex features are required by time-to-pay, so that is the best place to test any refactoring.
 */
private[httpreadsbuilder] final class LoggingContext[-ServiceError](
  logger: RequestAwareLogger,
  hc: HeaderCarrier,
  makeErrorSafeToLogInProd: HttpReadsBuilderError[ServiceError] => String
) {

  def logError(responseContext: ResponseContext, error: HttpReadsBuilderError.CentrallyImmplementedVariant): Unit =
    logger.error(
      s"""${error.prodSummaryAndDetail}
         |Returning: ${makeErrorSafeToLogInProd(error)} .
         |Request made for received HTTP response: ${responseContext.method} ${responseContext.url} .
         |Received HTTP response status: ${responseContext.response.status}.
         |${safeToLogResponseBodyDescription(responseContext.response)}""".stripMargin
    )(hc)

  def logWarningAboutValidUnsuccessfulResponse(responseContext: ResponseContext): Unit =
    logger.warn(
      s"""Valid and expected error response was received from HTTP call.
         |Request made for received HTTP response: ${responseContext.method} ${responseContext.url} .
         |Received HTTP response status: ${responseContext.response.status}.
         |${safeToLogResponseBodyDescription(responseContext.response)}""".stripMargin
    )(hc)

  private def safeToLogResponseBodyDescription(response: HttpResponse): String =
    if (Status.isSuccessful(response.status)) {
      s"Received HTTP response body not logged for 2xx statuses."
    } else {
      s"Received HTTP response body: ${response.body}"
    }
}
