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

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.core.read.ListAppender
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import org.slf4j.LoggerFactory
import play.api.mvc.Request
import play.api.test.FakeRequest

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.ListHasAsScala

class CorrelationIdPopulationActionSpec extends AnyFreeSpec {

  class TestCorrelationIdPopulationAction extends CorrelationIdPopulationAction() {
    def transformPublic[A](request: Request[A]): Future[Request[A]] = transform(request)
  }

  def testWithLoggingCapture[A](body: ListAppender[ILoggingEvent] => A): A = {
    val logger = LoggerFactory.getLogger(classOf[TestCorrelationIdPopulationAction]).asInstanceOf[Logger]
    val appender = new ListAppender[ILoggingEvent]()

    appender.start()
    logger.addAppender(appender)

    try {
      body(appender)
    } finally {
      logger.detachAppender(appender)
    }
  }

  val testCorrelationIdPopulationAction = new TestCorrelationIdPopulationAction()

  "CorrelationIdPopulationAction" - {
    ".transform" - {
      "use an existing correlationId when provided one in the request" - {
        "adhering to case sensitivity" in {
          val testCorrelationId = UUID.randomUUID().toString

          val request = FakeRequest().withHeaders("correlationId" -> testCorrelationId)
          val result = testCorrelationIdPopulationAction.transformPublic(request).futureValue

          result.headers.get("correlationId") shouldBe Some(testCorrelationId)
        }

        "not adhering to case sensitivity" in {
          val testCorrelationId = UUID.randomUUID().toString

          val request = FakeRequest().withHeaders("CORreLaTioNiD" -> testCorrelationId)
          val result = testCorrelationIdPopulationAction.transformPublic(request).futureValue

          result.headers.get("correlationId") shouldBe Some(testCorrelationId)
        }
      }

      "log and generate a new correlationId when not provided in the request" in {
        val requestUri = "/test/uri"
        val request = FakeRequest(method = "GET", path = requestUri)

        testWithLoggingCapture { appender =>
          val result = testCorrelationIdPopulationAction.transformPublic(request).futureValue

          val maybeGeneratedCorrelationId = result.headers.get("correlationId")
          maybeGeneratedCorrelationId should not be None
          maybeGeneratedCorrelationId.get should fullyMatch regex "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"

          val generatedCorrelationId = maybeGeneratedCorrelationId.getOrElse(fail("Generated correlationId was somehow None after assertion"))

          val logs = appender.list.asScala
          logs.exists(event => {

            // Duplicate assertions here because the shouldBe true at the bottom doesn't point to what exactly failed
            event.getLevel shouldBe Level.WARN
            event.getFormattedMessage shouldBe s"[CORRELATION ID] Not found in request header for $requestUri, generated new correlationId: $generatedCorrelationId"

            event.getLevel == Level.WARN &&
              event.getFormattedMessage.contains(s"[CORRELATION ID] Not found in request header for $requestUri, generated new correlationId: $generatedCorrelationId")
          }) shouldBe true
          // This assertion of shouldBe true makes sure that the log actually happened
        }
      }
    }
  }
}
