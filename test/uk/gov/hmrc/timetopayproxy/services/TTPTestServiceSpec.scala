/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.services

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpTestConnector
import uk.gov.hmrc.timetopayproxy.models.{ConnectorError, RequestDetails, TtppEnvelope, TtppError}
import uk.gov.hmrc.timetopayproxy.support.UnitSpec
import scala.concurrent.ExecutionContext.Implicits.global
import cats.syntax.either._

import scala.concurrent.ExecutionContext

class TTPTestServiceSpec extends UnitSpec {
  implicit val hc = HeaderCarrier()

  val requestDetails = Seq(
    RequestDetails("someId", "content", Some("www.uri.com"), false),
    RequestDetails("someId", "content", Some("www.uri.com"), true)
  )

  "Retrieve request details" should {
    "return all the request details retrieved from the stub" when {
      "connector returns success" in {
        val connector = mock[TtpTestConnector]
        (
          connector
            .retrieveRequestDetails()(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(*, *)
          .returning(TtppEnvelope(requestDetails))

        val testService = new DefaultTTPTestService(connector)
        await(testService.retrieveRequestDetails().value) shouldBe requestDetails.asRight[TtppError]
      }
    }

    "return a failure response" when {
      "connector returns failure" in {
        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")

        val connector = mock[TtpTestConnector]
        (
          connector
            .retrieveRequestDetails()(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(*, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[Seq[RequestDetails]])
          )

        val testService = new DefaultTTPTestService(connector)
        await(testService.retrieveRequestDetails().value) shouldBe errorFromTtpConnector.asLeft[Seq[RequestDetails]]
      }
    }
  }

  "Save response details" should {
    "save the request details retrieved from the stub" when {
      "connector returns success" in {
        val details = RequestDetails("someId", "content", Some("www.uri.com"), true)
        val connector = mock[TtpTestConnector]
        (
          connector
            .saveResponseDetails(_: RequestDetails)(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(details, *, *)
          .returning(TtppEnvelope(()))

        val testService = new DefaultTTPTestService(connector)
        await(testService.saveResponseDetails(details).value) shouldBe ().asRight[TtppError]
      }
    }

    "return a failure response" when {
      "connector returns failure" in {
        val details = RequestDetails("someId", "content", Some("www.uri.com"), true)

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")

        val connector = mock[TtpTestConnector]
        (
          connector
            .saveResponseDetails(_: RequestDetails)(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(*, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[Unit])
          )

        val testService = new DefaultTTPTestService(connector)
        await(testService.saveResponseDetails(details).value) shouldBe errorFromTtpConnector.asLeft[Seq[RequestDetails]]
      }
    }
  }

  "Retrieve error details" should {
    "return all the request details retrieved from the stub" when {
      "connector returns success" in {
        val connector = mock[TtpTestConnector]
        (
          connector
            .getErrors()(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(*, *)
          .returning(TtppEnvelope(requestDetails))

        val testService = new DefaultTTPTestService(connector)
        await(testService.getErrors().value) shouldBe requestDetails.asRight[TtppError]
      }
    }

    "return a failure response" when {
      "connector returns failure" in {
        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")

        val connector = mock[TtpTestConnector]
        (
          connector
            .getErrors()(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(*, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[Seq[RequestDetails]])
          )

        val testService = new DefaultTTPTestService(connector)
        await(testService.getErrors().value) shouldBe errorFromTtpConnector.asLeft[Seq[RequestDetails]]
      }
    }
  }

  "Save error details" should {
    "save the request details retrieved from the stub" when {
      "connector returns success" in {
        val details = RequestDetails("someId", "content", Some("www.uri.com"), true)
        val connector = mock[TtpTestConnector]
        (
          connector
            .saveError(_: RequestDetails)(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(details, *, *)
          .returning(TtppEnvelope(()))

        val testService = new DefaultTTPTestService(connector)
        await(testService.saveError(details).value) shouldBe ().asRight[TtppError]
      }
    }

    "return a failure response" when {
      "connector returns failure" in {
        val details = RequestDetails("someId", "content", Some("www.uri.com"), true)

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")

        val connector = mock[TtpTestConnector]
        (
          connector
            .saveError(_: RequestDetails)(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(*, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[Unit])
          )

        val testService = new DefaultTTPTestService(connector)
        await(testService.saveError(details).value) shouldBe errorFromTtpConnector.asLeft[Seq[RequestDetails]]
      }
    }
  }

  "Delete request details" should {
    "Delete a request identified by the requestId in the parameters" when {
      "connector returns success" in {
        val id = "1"
        val connector = mock[TtpTestConnector]
        (
          connector
            .deleteRequest(_: String)(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(id, *, *)
          .returning(TtppEnvelope(()))

        val testService = new DefaultTTPTestService(connector)
        await(testService.deleteRequestDetails(id).value) shouldBe ().asRight[TtppError]
      }
    }

    "return a failure response" when {
      "connector returns failure" in {
        val id = "2"

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")

        val connector = mock[TtpTestConnector]
        (
          connector
            .deleteRequest(_: String)(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(*, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[Unit])
          )

        val testService = new DefaultTTPTestService(connector)
        await(testService.deleteRequestDetails(id).value) shouldBe errorFromTtpConnector.asLeft[Seq[RequestDetails]]
      }
    }
  }
}
