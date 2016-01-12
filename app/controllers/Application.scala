package controllers

import collector.EventCollector
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index(""))
  }

  def cameraList = Action {
    Ok(Json.toJson(EventCollector.getCameraList))
  }

  def getSoundStatistics(cameraId: Int) = Action {
    Ok(Json.toJson(EventCollector.getSoundEventList(cameraId)))
  }
}