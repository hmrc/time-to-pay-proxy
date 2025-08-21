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

package uk.gov.hmrc.timetopayproxy.services

import cats.syntax.either._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpConnector
import uk.gov.hmrc.timetopayproxy.models._
import uk.gov.hmrc.timetopayproxy.support.UnitSpec

import java.time.LocalDate
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class UpdatePlanServiceSpec extends UnitSpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val updatePlanRequest = UpdatePlanRequest(
    CustomerReference("customerReference"),
    PlanId("planId"),
    UpdateType("updateType"),
    None,
    Some(PlanStatus.ResolvedCancelled),
    None,
    Some(CancellationReason("reason")),
    Some(true),
    Some(
      List(
        PaymentInformation(
          PaymentMethod.Bacs,
          Some(PaymentReference("reference"))
        )
      )
    )
  )

  "Update Quote endpoint" should {
    "return a success response" when {
      "connector returns success" in {
        val responseFromTtp = UpdatePlanResponse(
          CustomerReference("customerReference"),
          PlanId("planId"),
          PlanStatus.Success,
          LocalDate.now
        )
        val connector = mock[TtpConnector]
        (
          connector
            .updatePlan(_: UpdatePlanRequest)(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(updatePlanRequest, *, *)
          .returning(TtppEnvelope(responseFromTtp))

        val ttpQuoteService = new DefaultTTPQuoteService(connector)
        await(
          ttpQuoteService.updatePlan(updatePlanRequest).value,
          5,
          TimeUnit.SECONDS
        ) shouldBe responseFromTtp.asRight[TtppError]
      }
    }
    "return a failure response" when {
      "connector returns failure" in {

        val errorFromTtpConnector =
          ConnectorError(500, "Internal Service Error")
        val connector = mock[TtpConnector]
        (
          connector
            .updatePlan(_: UpdatePlanRequest)(
              _: ExecutionContext,
              _: HeaderCarrier
            )
          )
          .expects(updatePlanRequest, *, *)
          .returning(
            TtppEnvelope(errorFromTtpConnector.asLeft[UpdatePlanResponse])
          )

        val ttpQuoteService = new DefaultTTPQuoteService(connector)
        await(
          ttpQuoteService.updatePlan(updatePlanRequest).value,
          5,
          TimeUnit.SECONDS
        ) shouldBe errorFromTtpConnector.asLeft[UpdatePlanResponse]
      }
    }
  }
}
