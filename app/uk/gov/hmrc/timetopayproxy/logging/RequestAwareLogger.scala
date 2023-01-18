/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.timetopayproxy.logging

import org.slf4j.MDC
import play.api.Logger
import uk.gov.hmrc.http.{ HeaderCarrier, RequestId }
import uk.gov.hmrc.timetopayproxy.models.HeaderNames.{ CorrelationIdKey, RequestIdKey }

class RequestAwareLogger(clazz: Class[_]) {

  private val underlying = Logger(clazz)

  def trace(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.trace(msg))
  def debug(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.debug(msg))
  def info(msg:  => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.info(msg))
  def warn(msg:  => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.warn(msg))
  def error(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.error(msg))

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

}
