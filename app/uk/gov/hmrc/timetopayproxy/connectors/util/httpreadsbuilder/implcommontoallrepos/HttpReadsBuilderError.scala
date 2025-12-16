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
import play.api.libs.json.{ JsPath, JsonValidationError }
import uk.gov.hmrc.timetopayproxy.connectors.util.httpreadsbuilder.implcommontoallrepos.HttpReadsBuilderError.WhichEitherWasExpected

/** This trait exists so that repo-specific error classes don't have to appear in the `HttpReadsBuilder` unit tests.
  * If `HttpReadsBuilder` returns only instances of this trait, all copies of `HttpReadsBuilder` across our repos
  *   can be consistent, and the transformation into repo-specific errors is only declared here.
  *
  * Implementations of this trait have all the data necessary to create a connector error, no matter what those
  *   classes might be. If any data is missing from these classes (e.g. timestamps), refactor them to include it,
  *   and use that data in the conversions of this file that need it.
  *
  * @tparam StoredServiceError The type of connector/service error we are required to convert this into.
  *                            It's covariant because if we store a repo-specific service error, we have no choice but to return it.
  */
/* ℹ️ Note about updating this file: ℹ️
 * This file must be kept consistent with every copy in the other debt-transformation repos.
 * The most complex features are required by time-to-pay, so that is the best place to test any refactoring.
 */
private[httpreadsbuilder] sealed trait HttpReadsBuilderError[+StoredServiceError] {

  def whichEitherWasExpected: WhichEitherWasExpected

  /** Overridden so we don't accidentally log sensitive information. The `time-to-pay` service is quite sensitive to this. */
  // The implementation is such that ScalaTest will be able to enhance it under the hood in test failures.
  final override def toString: String = s"${this.getClass.getSimpleName}(<hidden because it may be sensitive>)"
}

private[httpreadsbuilder] object HttpReadsBuilderError {

  /** "What does a received status code mean for the connector?"
    *
    * This is important when we decide if a failed HTTP request may be retried.
    * Some HoDs don't want their successes to be retried, even if their responses could not be understood.
    * The entire utility does not use specific status codes to determine what's generally a success and what isn't,
    *   except when it comes to deciding if a response body is safe to log in production.
    *   Successes are determined by how they are added to the `HttpReadsBuilder`: `Left` vs `Right`.
    */
  sealed abstract class WhichEitherWasExpected(val description: String)

  case object OriginallyMeantToBeRight
      extends WhichEitherWasExpected(description = "Originally expected to turn response into a Right.")

  case object OriginallyMeantToBeLeft
      extends WhichEitherWasExpected(description = "Originally expected to turn response into a Left.")

  final case class PassthroughServiceError[+ServiceError](error: ServiceError)
      extends HttpReadsBuilderError[ServiceError] {

    def whichEitherWasExpected: WhichEitherWasExpected = OriginallyMeantToBeLeft
  }

  /** The subclasses of [[HttpReadsBuilderError]] that are fully understood by the `HttpReadsBuilder`. */
  sealed trait CentrallyImmplementedVariant extends HttpReadsBuilderError[Nothing] {
    def prodSummaryAndDetail: String = logMessageSummary + prodLogMessageDetail.fold("")("\nDetail: " + _)

    protected def prodLogMessageDetail: Option[String]

    /** Title at the top of a log message involving this error. It summarises what happened. */
    // Declared centrally to discourage error-specific information, because it's a summary.
    private final def logMessageSummary: String =
      this match {
        case _: UnexpectedStatusCode =>
          s"HTTP status is unexpected in received HTTP response. ${whichEitherWasExpected.description}"
        case _: ResponseBodyNotJson =>
          s"HTTP body is not JSON in received HTTP response. ${whichEitherWasExpected.description}"
        case _: ResponseBodyInvalidJsonStructure =>
          s"JSON structure is not valid in received HTTP response. ${whichEitherWasExpected.description}"
        case _: ResponseBodyNotEmpty =>
          s"Body of received HTTP response is not empty. ${whichEitherWasExpected.description}"
      }

  }

  /** Error created when the builder was never told to expect the received status code. */
  final case class UnexpectedStatusCode(sourceClass: Class[_], responseContext: ResponseContext)
      extends CentrallyImmplementedVariant {

    def whichEitherWasExpected: WhichEitherWasExpected = OriginallyMeantToBeLeft

    def prodLogMessageDetail: None.type = None
  }

  /** Error created when the builder was told to expect this status code, but the HTTP body was not JSON. */
  final case class ResponseBodyNotJson(
    sourceClass: Class[_],
    responseContext: ResponseContext,
    whichEitherWasExpected: WhichEitherWasExpected,
    sensitiveException: Throwable
  ) extends CentrallyImmplementedVariant {

    def prodLogMessageDetail: Option[String] =
      if (Status.isSuccessful(responseContext.response.status))
        None
      else
        Some(sensitiveException.toString) // This strips the stack trace and is not ideal. Better to expose the error.
  }

  /** Error created when the builder was told to expect this status code, but the HTTP body was the wrong JSON. */
  final case class ResponseBodyInvalidJsonStructure(
    sourceClass: Class[_],
    responseContext: ResponseContext,
    whichEitherWasExpected: WhichEitherWasExpected,
    errs: Seq[(JsPath, Seq[JsonValidationError])]
  ) extends CentrallyImmplementedVariant {

    def prodLogMessageDetail: Option[String] =
      // We cannot log the errors in prod when the response is a 2xx because we expect it to contain sensitive data.
      if (Status.isSuccessful(responseContext.response.status))
        None
      else
        Some {
          val validationErrors: String = errs
            .map { case (path, validationErrors) =>
              s"    - For path $path , errors: ${validationErrors.map(_.message).mkString("[", ", ", "]")}"
            }
            .mkString("", ";\n", ".")

          s"Validation errors:\n$validationErrors"
        }
  }

  /** Error created when the builder was told to expect this status code, but we got a HTTP body without expecting one. */
  final case class ResponseBodyNotEmpty(
    sourceClass: Class[_],
    responseContext: ResponseContext,
    whichEitherWasExpected: WhichEitherWasExpected
  ) extends CentrallyImmplementedVariant {

    def prodLogMessageDetail: None.type = None
  }

}
