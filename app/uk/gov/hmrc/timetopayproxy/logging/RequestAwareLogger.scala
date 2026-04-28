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

package uk.gov.hmrc.timetopayproxy.logging

import org.slf4j.MDC
import org.slf4j.event.Level
import play.api.Logger
import uk.gov.hmrc.http.{ HeaderCarrier, RequestId }
import uk.gov.hmrc.timetopayproxy.models.HeaderNames.{ CorrelationIdKey, RequestIdKey }

class RequestAwareLogger(underlying: Logger) {

  def this(clazz: Class[_]) =
    this(Logger(clazz))

  def trace(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.trace(msg))
  def trace(msg: => String, error: => Throwable)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(
    underlying.trace(msg, error)
  )

  def debug(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.debug(msg))
  def debug(msg: => String, error: => Throwable)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(
    underlying.debug(msg, error)
  )

  def info(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.info(msg))
  def info(msg: => String, error: => Throwable)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(
    underlying.info(msg, error)
  )

  def warn(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.warn(msg))
  def warn(msg: => String, error: => Throwable)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(
    underlying.warn(msg, error)
  )

  def error(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.error(msg))
  def error(msg: => String, error: => Throwable)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(
    underlying.error(msg, error)
  )

  private def withRequestIDsInMDC(f: => Unit)(implicit hc: HeaderCarrier): Unit = {
    val requestId = hc.requestId.getOrElse(RequestId("Undefined"))
    val correlationId = hc.otherHeaders
      .collectFirst { case (key, value) if key.equalsIgnoreCase(CorrelationIdKey) => value }

    MDC.put(RequestIdKey, requestId.value)
    correlationId.foreach(MDC.put(CorrelationIdKey, _))
    f
    MDC.remove(RequestIdKey)
    correlationId.foreach(_ => MDC.remove(CorrelationIdKey))
  }

  /** Logs twice, once with a summary and again with the summary and detail.
    * The summary is necessary in order to match on the full string.
    * The summary must be a constant string with no interpolation.
    * The summary must match the log alerts in the alerts-config
    */
  def alert(
    pagerAlert: PagerAlert,
    additionalDetail: String,
    throwable: Throwable
  )(implicit
    hc: HeaderCarrier
  ): Unit = {
    val detailedMessage = s"${pagerAlert.summary}\n$additionalDetail"
    pagerAlert.level match {
      case Level.ERROR =>
        error(pagerAlert.summary, throwable)
        error(detailedMessage, throwable)
      case Level.WARN =>
        warn(pagerAlert.summary, throwable)
        warn(detailedMessage, throwable)
      case Level.INFO =>
        info(pagerAlert.summary, throwable)
        info(detailedMessage, throwable)
      case Level.DEBUG =>
        debug(pagerAlert.summary, throwable)
        debug(detailedMessage, throwable)
      case Level.TRACE =>
        trace(pagerAlert.summary, throwable)
        trace(detailedMessage, throwable)
    }
  }

  /** Logs twice, once with a summary and again with the summary and detail.
    * The summary is necessary in order to match on the full string.
    * The summary must be a constant string with no interpolation.
    * The summary must match the log alerts in the alerts-config
    */
  def alert(pagerAlert: PagerAlert, additionalDetail: String)(implicit hc: HeaderCarrier): Unit = {
    val detailedMessage = s"${pagerAlert.summary}\n$additionalDetail"
    pagerAlert.level match {
      case Level.ERROR =>
        error(pagerAlert.summary)
        error(detailedMessage)
      case Level.WARN =>
        warn(pagerAlert.summary)
        warn(detailedMessage)
      case Level.INFO =>
        info(pagerAlert.summary)
        info(detailedMessage)
      case Level.DEBUG =>
        debug(pagerAlert.summary)
        debug(detailedMessage)
      case Level.TRACE =>
        trace(pagerAlert.summary)
        trace(detailedMessage)
    }
  }
}

/** @param summary a value to be matched against in the alerts config
  * @param level the error level given in Kibana, only WARN and ERROR should be in Prod
  *
  * Please ensure that a matching check for the log/summary is added to the alerts-config when you add a PagerAlert case
  * https://github.com/hmrc/alert-config/blob/main/src/main/scala/uk/gov/hmrc/alertconfig/configs/DTD.scala
  */
sealed abstract class PagerAlert(val summary: String, val level: Level)

object PagerAlert {

  case object ProxyJsonIssueAlert
      extends PagerAlert(summary = "CRITICAL: A request in time-to-pay-proxy has JSON issues", level = Level.ERROR)

  case object ProxyValidationIssueAlert
      extends PagerAlert(
        summary = "CRITICAL: A request in time-to-pay-proxy has validation issues",
        level = Level.ERROR
      )

  case object ProxyOtherIssueAlert
      extends PagerAlert(summary = "WARNING: An issue in time-to-pay-proxy has occurred", level = Level.WARN)
}
