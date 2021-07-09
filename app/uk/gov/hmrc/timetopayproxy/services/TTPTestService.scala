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

import com.google.inject.ImplementedBy
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.timetopayproxy.connectors.TtpTestConnector
import uk.gov.hmrc.timetopayproxy.models.RequestDetails
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[DefaultTTPTestService])
trait TTPTestService {
  def retrieveRequestDetails()(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Seq[RequestDetails]]
  def saveResponseDetails(details: RequestDetails)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Unit]
}

@Singleton
class DefaultTTPTestService @Inject()(connector: TtpTestConnector) extends TTPTestService {
  override def retrieveRequestDetails()(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Seq[RequestDetails]] =
    connector.retrieveRequestDetails()

  override def saveResponseDetails(details: RequestDetails)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Unit] =
    connector.saveResponseDetails(details)
}
