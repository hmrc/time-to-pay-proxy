package uk.gov.hmrc.timetopayproxy.support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.concurrent.{Eventually, IntegrationPatience}

object WireMockHelper extends Eventually with IntegrationPatience {

  val wireMockPort: Int = 11111
  val host: String = "localhost"
}

trait WireMockHelper {
  import WireMockHelper._

  lazy val wireMockConf: WireMockConfiguration = wireMockConfig.port(wireMockPort)
  lazy val wireMockServer: WireMockServer = new WireMockServer(wireMockConf)

  def startWireMock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(host, wireMockPort)
  }

  def stopWireMock(): Unit = wireMockServer.stop()

  def resetWireMock(): Unit = WireMock.reset()
}