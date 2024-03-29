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

package uk.gov.hmrc.timetopayproxy.config

import com.google.inject.{ AbstractModule, Provides }
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.timetopayproxy.connectors.{ DefaultTtpConnector, TtpConnector }
import uk.gov.hmrc.timetopayproxy.services.{ DefaultTTPQuoteService, TTPQuoteService }

class DIModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[PlayAuthConnector])
      .to(classOf[DefaultAuthConnector])
      .asEagerSingleton()

    bind(classOf[TtpConnector])
      .to(classOf[DefaultTtpConnector])
      .asEagerSingleton()

    bind(classOf[TTPQuoteService])
      .to(classOf[DefaultTTPQuoteService])
      .asEagerSingleton()
  }

  @Provides protected def provideFeatureSwitch(appConfig: AppConfig): FeatureSwitch =
    FeatureSwitch(appConfig.featureSwitch)
}
