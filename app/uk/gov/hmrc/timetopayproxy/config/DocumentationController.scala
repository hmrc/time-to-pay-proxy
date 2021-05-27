package uk.gov.hmrc.timetopayproxy.config

import javax.inject.{Inject, Singleton}
import controllers.Assets
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class DocumentationController @Inject()(assets: Assets, controllerComponents: ControllerComponents)
  extends BackendController(controllerComponents){
  def definition(): Action[AnyContent] = {
    assets.at("/public/api", "definition.json")
  }

  def raml(version: String, file: String): Action[AnyContent] = {
    assets.at(s"/public/api/conf/$version", file)
  }
}
