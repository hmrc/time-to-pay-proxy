/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.timetopayproxy.connectors

import cats.implicits.catsSyntaxEitherId
import com.google.inject.ImplementedBy
import play.api.http.Status.{ NO_CONTENT, OK }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient, HttpException, HttpResponse, UpstreamErrorResponse }
import uk.gov.hmrc.timetopayproxy.config.AppConfig
import uk.gov.hmrc.timetopayproxy.models.TtppEnvelope.TtppEnvelope
import uk.gov.hmrc.timetopayproxy.models.{ ConnectorError, RequestDetails, TtppEnvelope }
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

// $COVERAGE-OFF$
//Coverage disabled for non-prod source
@ImplementedBy(classOf[DefaultTtpTestConnector])
trait TtpTestConnector {
  def retrieveRequestDetails()(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Seq[RequestDetails]]

  def saveResponseDetails(details: RequestDetails)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Unit]

  def deleteRequest(requestId: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Unit]

  def saveError(details: RequestDetails)(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Unit]

  def getErrors()(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Seq[RequestDetails]]
}

@Singleton
class DefaultTtpTestConnector @Inject()(appConfig: AppConfig, httpClient: HttpClient) extends TtpTestConnector {
  override def retrieveRequestDetails()(
    implicit
    ec: ExecutionContext,
    hc: HeaderCarrier): TtppEnvelope[Seq[RequestDetails]] = {
    val path = "/test-only/requests"
    val url = s"${appConfig.stubBaseUrl}$path"

    TtppEnvelope {
      httpClient
        .GET[Seq[RequestDetails]](url)
        .map(r => r.asRight[ConnectorError])
        .recover {
          case e: HttpException         => ConnectorError(e.responseCode, e.message).asLeft[Seq[RequestDetails]]
          case e: UpstreamErrorResponse => ConnectorError(e.statusCode, e.getMessage()).asLeft[Seq[RequestDetails]]
        }
    }
  }
  override def saveResponseDetails(
    details: RequestDetails
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Unit] = {
    val path = "/test-only/response"
    val url = s"${appConfig.stubBaseUrl}$path"

    TtppEnvelope {
      httpClient
        .POST[RequestDetails, HttpResponse](url, details)
        .map { response =>
          response.status match {
            case OK => ()
            case _  => ConnectorError(response.status, "Unexpected response code").asLeft[Unit]
          }
        }
        .recover {
          case e: HttpException         => ConnectorError(e.responseCode, e.message).asLeft[Unit]
          case e: UpstreamErrorResponse => ConnectorError(e.statusCode, e.getMessage()).asLeft[Unit]
        }
    }
  }
  override def deleteRequest(
    requestId: String
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Unit] = {
    val path = s"/test-only/request/$requestId"
    val url = s"${appConfig.stubBaseUrl}$path"

    TtppEnvelope {
      httpClient
        .DELETE(url)
        .map { response =>
          response.status match {
            case OK         => Unit
            case NO_CONTENT => Unit
            case _          => ConnectorError(response.status, "Unexpected response code").asLeft
          }
        }
        .recover {
          case e: HttpException         => ConnectorError(e.responseCode, e.message).asLeft
          case e: UpstreamErrorResponse => ConnectorError(e.statusCode, e.getMessage()).asLeft
        }
    }

  }
  override def saveError(
    details: RequestDetails
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Unit] = {
    val path = "/test-only/errors"
    val url = s"${appConfig.stubBaseUrl}$path"

    TtppEnvelope {
      httpClient
        .POST[RequestDetails, HttpResponse](url, details)
        .map { response =>
          response.status match {
            case OK => ()
            case _  => ConnectorError(response.status, "Unexpected response code").asLeft[Unit]
          }
        }
        .recover {
          case e: HttpException         => ConnectorError(e.responseCode, e.message).asLeft[Unit]
          case e: UpstreamErrorResponse => ConnectorError(e.statusCode, e.getMessage()).asLeft[Unit]
        }
    }
  }
  override def getErrors()(implicit ec: ExecutionContext, hc: HeaderCarrier): TtppEnvelope[Seq[RequestDetails]] = {
    val path = "/test-only/errors"
    val url = s"${appConfig.stubBaseUrl}$path"

    TtppEnvelope {
      httpClient
        .GET[Seq[RequestDetails]](url)
        .map(r => r.asRight[ConnectorError])
        .recover {
          case e: HttpException         => ConnectorError(e.responseCode, e.message).asLeft[Seq[RequestDetails]]
          case e: UpstreamErrorResponse => ConnectorError(e.statusCode, e.getMessage()).asLeft[Seq[RequestDetails]]
        }
    }
  }
}
// $COVERAGE-ON$
