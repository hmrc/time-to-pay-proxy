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

package uk.gov.hmrc.timetopayproxy.services

import cats.data.EitherT
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.ChargeMigrationConnector
import uk.gov.hmrc.timetopayproxy.models.ChannelIdentifier
import uk.gov.hmrc.timetopayproxy.models.cdcs.chargemigration.{ ChargeMigration, ChargeMigrationRequest, ChargeMigrationResponse, ReplacementCharge }
import uk.gov.hmrc.timetopayproxy.models.error.ProxyEnvelopeError

import java.time.{ Instant, LocalDate }
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.ExecutionContext.Implicits.global

class ChargeMigrationServiceSpec extends AnyFreeSpec with MockFactory with ScalaFutures {

  private val mockConnector = mock[ChargeMigrationConnector]
  private val service = new ChargeMigrationService(mockConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "ChargeMigrationService" - {
    "return the connector response" in {
      val request: ChargeMigrationRequest =
        ChargeMigrationRequest(
          planId = "planId",
          migratedAt = Instant.parse("2026-07-08T13:49:51.123Z"),
          planCreationChannel = ChannelIdentifier.Advisor,
          chargeMigrations = List(
            ChargeMigration(
              originalChargeId = "chargeId01",
              replacementDebtItemChargeId = "chargeId02",
              replacementCharges = List(
                ReplacementCharge(
                  parentMainTrans = Some("5330"),
                  mainTrans = "5330",
                  subTrans = "7006",
                  originalDebtAmount = BigInt(5000),
                  interestStartDate = Some(LocalDate.parse("2026-06-30")),
                  paymentHistory = None
                )
              )
            )
          )
        )

      val response: ChargeMigrationResponse =
        ChargeMigrationResponse(
          planId = "planId",
          processingDateTime = Instant.parse("2026-07-08T13:50:00Z")
        )

      (mockConnector
        .chargeMigration(_: ChargeMigrationRequest)(
          _: ExecutionContext,
          _: HeaderCarrier
        ))
        .expects(request, *, *)
        .returning(EitherT.rightT[Future, ProxyEnvelopeError](response))

      service
        .chargeMigration(request)
        .value
        .futureValue shouldBe Right(response)
    }
  }
}
