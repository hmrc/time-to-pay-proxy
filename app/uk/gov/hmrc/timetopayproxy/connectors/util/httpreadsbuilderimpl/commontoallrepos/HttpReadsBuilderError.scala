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

package uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilderimpl.commontoallrepos

import play.api.http.Status
import play.api.libs.json.{ JsPath, JsonValidationError }

/** This trait exists so that repo-specific error classes don't have to appear in the `HttpReadsBuilder` unit tests.
  * If `HttpReadsBuilder` returns only instances of this trait, all copies of `HttpReadsBuilder` across our repos
  *   can be consistent, and the transformation into repo-specific errors is only declared here.
  *
  * Implementations of this trait have all the data necessary to create a connector error, no matter what those
  *   classes might be. If any data is missing from these classes (e.g. timestamps), refactor them to include it,
  *   and use that data in the conversions of this file that need it.
  *
  * @tparam StoredServiceError The type of connector/service error we are required to convert this into.
  *                            It's covariant because if we store a service error, we have no choice but to return it.
  */
/* ℹ️ Note about updating this file: ℹ️
 * This file must be kept consistent with every copy in the other debt-transformation repos.
 * The most complex features are required by time-to-pay, so that is the best place to test any refactoring.
 */
sealed trait HttpReadsBuilderError[+StoredServiceError] {

  /** Overridden so we don't accidentally log sensitive information. */
  // The implementation is such that ScalaTest will be able to enhance it under the hood in test failures.
  final override def toString: String = s"${this.getClass.getSimpleName}(<hidden because it may be sensitive>)"
}

object HttpReadsBuilderError {

  /** Package-private because the connectors should have no business dealing with the specific implementations. */
  private[util] object Impl {

    final case class PassthroughServiceError[+ServError](error: ServError) extends HttpReadsBuilderError[ServError]

    /** The subclasses of [[HttpReadsBuilderError]] that are fully understood by the `HttpReadsBuilder`. */
    sealed trait CentrallyImmplementedVariant extends HttpReadsBuilderError[Nothing]

    final case class GeneralErrorForUnsuccessfulStatusCode(
      sourceClass: Class[_],
      responseContext: ResponseContext,
      simpleMessage: String
    ) extends CentrallyImmplementedVariant

    object GeneralErrorForUnsuccessfulStatusCode {
      def createAndLogForUnexpectedStatusCode[ServError](
        sourceClass: Class[_],
        responseContext: ResponseContext,
        maybeLoggingContext: Option[LoggingContext[ServError]]
      ): GeneralErrorForUnsuccessfulStatusCode = {
        val error = GeneralErrorForUnsuccessfulStatusCode(
          sourceClass = sourceClass,
          responseContext,
          simpleMessage = "HTTP status is unexpected in received HTTP response."
        )

        maybeLoggingContext.foreach(_.logError(responseContext, simpleMessage = error.simpleMessage, error))
        error
      }

      /** This is only for error responses */
      def createAndLogForNonJsonErrorInErrorResponse[ServError](
        sourceClass: Class[_],
        responseContext: ResponseContext,
        maybeLoggingContext: Option[LoggingContext[ServError]]
      ): GeneralErrorForUnsuccessfulStatusCode = {
        val error = GeneralErrorForUnsuccessfulStatusCode(
          sourceClass = sourceClass,
          responseContext,
          simpleMessage = "HTTP body is not JSON in received error HTTP response."
        )

        maybeLoggingContext.foreach(_.logError(responseContext, simpleMessage = error.simpleMessage, error))
        error
      }

      /** This is only for error responses */
      def createAndLogForInvalidJsonStructureInErrorResponse[ServError](
        sourceClass: Class[_],
        responseContext: ResponseContext,
        maybeLoggingContext: Option[LoggingContext[ServError]],
        errs: Seq[(JsPath, Seq[JsonValidationError])]
      ): GeneralErrorForUnsuccessfulStatusCode = {
        val possiblyDetailedMessage: String =
          if (Status.isSuccessful(responseContext.response.status))
            "JSON structure is not valid in received error HTTP response."
          else {
            val validationErrors: String = errs
              .map { case (path, validationErrors) =>
                s"    - For path $path , errors: ${validationErrors.map(_.message).mkString("[", ", ", "]")}"
              }
              .mkString("", ";\n", ".")

            s"JSON structure is not valid in received error HTTP response.\n  Validation errors:\n$validationErrors"
          }

        val error = GeneralErrorForUnsuccessfulStatusCode(
          sourceClass = sourceClass,
          responseContext,
          simpleMessage = possiblyDetailedMessage
        )

        maybeLoggingContext.foreach(_.logError(responseContext, simpleMessage = possiblyDetailedMessage, error))
        error
      }

      def createAndLogForNonEmptyBodyInErrorResponse[ServError](
        sourceClass: Class[_],
        responseContext: ResponseContext,
        maybeLoggingContext: Option[LoggingContext[ServError]]
      ): GeneralErrorForUnsuccessfulStatusCode = {
        val trimmedBodyLength: Int = responseContext.response.body.trim.length

        val error = GeneralErrorForUnsuccessfulStatusCode(
          sourceClass = sourceClass,
          responseContext,
          simpleMessage = s"Body of error upstream HTTP response is not empty. Trimmed body length: $trimmedBodyLength."
        )

        maybeLoggingContext.foreach(_.logError(responseContext, simpleMessage = error.simpleMessage, error))
        error
      }
    }

    final case class NotJsonErrorForSuccess(
      sourceClass: Class[_],
      responseContext: ResponseContext,
      sensitiveException: Throwable
    ) extends CentrallyImmplementedVariant

    final case class JsonNotValidErrorForSuccess(
      sourceClass: Class[_],
      responseContext: ResponseContext,
      errs: Seq[(JsPath, Seq[JsonValidationError])]
    ) extends CentrallyImmplementedVariant

    final case class BodyNotEmptyErrorForSuccess(sourceClass: Class[_], responseContext: ResponseContext)
        extends CentrallyImmplementedVariant

  }

}
