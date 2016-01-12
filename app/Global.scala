import collector.EventCollector._
import play.Play
import play.api.{Application, GlobalSettings}
import play.api.Play.current
import play.api.libs.concurrent.Akka

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by dmitry on 11.12.15.
 */
object Global extends GlobalSettings {

  override def onStart(application: Application): Unit = {
    val loopTime = Play.application().configuration().getString("nest.loop.time").toInt
    Akka.system.scheduler.schedule(0.second, loopTime.second)({
      collectCameraEvents()
    })
  }
}
