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
}
