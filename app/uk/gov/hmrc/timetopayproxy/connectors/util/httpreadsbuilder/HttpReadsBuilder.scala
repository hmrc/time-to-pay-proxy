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

package uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder

import play.api.libs.json.Reads
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads, HttpResponse }
import uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder.implcommontoallrepos.{ HttpReadsBuilderError, HttpReadsBuilderErrorConverter, LoggingContext, ResponseContext, SingleStatusHandler }
import uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder.implrepospecific.HttpReadsBuilderCompanionInterfaces
import uk.gov.hmrc.timetopayproxy.logging.RequestAwareLogger

/** IMPORTANT:
  *   Setup methods throw an exception when the same HTTP status code is set up with a handler more than once,
  *   so you MUST ensure the HTTP status codes are not provided based on application.conf, as that will compromise testing.
  *
  * @param explicitHandlers A map from a configured status code to a way to create a Result or an error from the response.
  *
  * @tparam ServiceError The type of error we expect to return. Would be covariant if HMRC's `HttpReads` allowed it.
  * @tparam Result The type of successful result we expect to return. Would be covariant if HMRC's `HttpReads` allowed it.
  */
/* ℹ️ Note about updating this file: ℹ️
 * This file must be kept consistent with every copy in the other debt-transformation repos.
 * The most complex features are required by time-to-pay, so that is the best place to test any refactoring.
 */
final class HttpReadsBuilder[ServiceError, Result] private[httpreadsbuilder] (
  sourceClass: Class[_],
  explicitHandlers: Map[Int, SingleStatusHandler[ServiceError, Result]],
  createConnectorError: HttpReadsBuilderError[ServiceError] => ServiceError
) {

  /* ⚠️ IMPORTANT ⚠️
   * LOGGERS MUST NOT BE DECLARED HERE.
   * Some Debt Transformation connectors are not allowed to log to production.
   * e.g. time-to-pay's CronScheduler requests to Customer Check, HoD Referral etc. must not log to Prod without extreme care.
   */

  /** NOTE: The HMRC library used does not distinguish between an empty HTTP body and no HTTP body. HTTP itself does.
    * @throws IllegalArgumentException if the `incomingStatus` value is already configured.
    */
  def handleSuccessNoEntity(incomingStatus: Int, value: Result): HttpReadsBuilder[ServiceError, Result] =
    this.withNewHandlerOrThrow(
      incomingStatus = incomingStatus,
      handler = SingleStatusHandler.assumingNoBody[Nothing, Result](sourceClass = sourceClass, Right(value))
    )

  /** NOTE: The HMRC library used does not distinguish between an empty HTTP body and no HTTP body. HTTP itself does.
    * @throws IllegalArgumentException if the `incomingStatus` value is already configured.
    */
  def handleErrorNoEntity(
    incomingStatus: Int,
    value: ServiceError
  ): HttpReadsBuilder[ServiceError, Result] =
    this.withNewHandlerOrThrow(
      incomingStatus = incomingStatus,
      handler = SingleStatusHandler.assumingNoBody[ServiceError, Nothing](sourceClass = sourceClass, Left(value))
    )

  /** @throws IllegalArgumentException if the `incomingStatus` value is already configured. */
  def handleSuccess[ReadableResult <: Result: Reads](incomingStatus: Int): HttpReadsBuilder[ServiceError, Result] =
    this.withNewHandlerOrThrow(
      incomingStatus = incomingStatus,
      handler = SingleStatusHandler.assumingJsonSuccess[ServiceError, Result, ReadableResult](
        sourceClass = sourceClass,
        transform = identity
      )
    )

  /** @throws IllegalArgumentException if the `incomingStatus` value is already configured. */
  def handleError[ReadableError <: ServiceError: Reads](incomingStatus: Int): HttpReadsBuilder[ServiceError, Result] =
    this.withNewHandlerOrThrow(
      incomingStatus = incomingStatus,
      handler = SingleStatusHandler.assumingJsonError[Result, ReadableError, ServiceError](
        sourceClass = sourceClass,
        transform = identity
      )
    )

  /** @throws IllegalArgumentException if the `incomingStatus` value is already configured. */
  def handleErrorTransformed[ReadableError: Reads](
    incomingStatus: Int,
    transform: ReadableError => ServiceError
  ): HttpReadsBuilder[ServiceError, Result] =
    this.withNewHandlerOrThrow(
      incomingStatus = incomingStatus,
      handler = SingleStatusHandler.assumingJsonError[Result, ReadableError, ServiceError](
        sourceClass = sourceClass,
        transform = transform
      )
    )

  /** Builds a `HttpReads` to parse the HTTP response into an appropriate `Either`. */
  def httpReads(
    logger: RequestAwareLogger,
    makeErrorSafeToLogInProd: ServiceError => String
  )(implicit
    hc: HeaderCarrier
  ): HttpReads[Either[ServiceError, Result]] = {
    val loggingContext = new LoggingContext(
      logger,
      hc,
      makeErrorSafeToLogInProd = createConnectorError.andThen(makeErrorSafeToLogInProd)
    )
    this.httpReadsOptionalLogging(Some(loggingContext))
  }

  /** Builds a `HttpReads` to parse the HTTP response into an appropriate `Either`. Does not have ANY logging. */
  def httpReadsNoLogging: HttpReads[Either[ServiceError, Result]] =
    this.httpReadsOptionalLogging(loggingContext = None)

  private def httpReadsOptionalLogging(
    loggingContext: Option[LoggingContext[ServiceError]]
  ): HttpReads[Either[ServiceError, Result]] =
    (method: String, url: String, response: HttpResponse) => {
      val responseContext = ResponseContext(method = method, url = url, response)

      explicitHandlers.get(response.status) match {
        case Some(handler) =>
          handler.processor(responseContext, loggingContext).left.map(createConnectorError)
        case None =>
          SingleStatusHandler
            .fallbackErrorForUnknownStatusCode(sourceClass = sourceClass)
            .processor(responseContext, loggingContext)
            .left
            .map(createConnectorError)
      }
    }

  /** @throws IllegalArgumentException if the `incomingStatus` value is already configured. */
  private def withNewHandlerOrThrow(
    incomingStatus: Int,
    handler: SingleStatusHandler[ServiceError, Result]
  ): HttpReadsBuilder[ServiceError, Result] = {
    // This will throw, but these builders shouldn't be constructed dynamically, so any testing should catch it.
    // We think it's more important to not misconfigure handlers (by using confusing handler precedence) than to throw an exception.
    require(
      !this.explicitHandlers.contains(incomingStatus),
      s"""Cannot add handler for status $incomingStatus twice."""
    )

    new HttpReadsBuilder(
      sourceClass = sourceClass,
      explicitHandlers = explicitHandlers + (incomingStatus -> handler),
      createConnectorError = createConnectorError
    )
  }
}

object HttpReadsBuilder extends HttpReadsBuilderCompanionInterfaces {

  /** Makes no choices about how to convert/present any encountered errors, e.g. unexpected status codes.
    * @param converter Instance responsible for converting the internal builder errors into something the connector will understand.
    */
  def empty[ServiceError, Result](
    sourceClass: Class[_],
    converter: HttpReadsBuilderErrorConverter[ServiceError]
  ): HttpReadsBuilder[ServiceError, Result] =
    new HttpReadsBuilder[ServiceError, Result](
      sourceClass = sourceClass,
      explicitHandlers = Map.empty,
      createConnectorError = converter.toConnectorError[ServiceError]
    )

}
