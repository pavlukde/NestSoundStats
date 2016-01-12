package collector

import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.{Calendar, TimeZone}

import anorm.SqlParser._
import anorm._
import org.joda.time.DateTime
import play.Play
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import play.api.libs.json.{JsError, JsResult, JsSuccess, Json}
import play.api.libs.ws.WS

/**
 * Created by dmitry on 11.12.15.
 */
object EventCollector {

  val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  format.setTimeZone(TimeZone.getTimeZone("GMT"))

  case class Camera(id: Long, name: String)

  case class SoundEvent(startDate: DateTime, duration: Long)

  object Camera {
    implicit val format = Json.format[Camera]

    val parser: RowParser[Camera] = {
      get[Long]("camera.id") ~
        get[String]("camera.name") map {
        case id ~ name => Camera(id, name)
      }
    }
  }

  object SoundEvent {
    implicit val format = Json.format[SoundEvent]

    val parser: RowParser[SoundEvent] = {
      get[DateTime]("sound_event.start_date") ~
        get[Long]("sound_event.duration") map {
        case startDate ~ duration => SoundEvent(startDate, duration)
      }
    }
  }


  implicit val lastEventFormat = Json.format[LastEvent]
  implicit val cameraFormat = Json.format[CameraObj]

  case class CameraObj(name: String, last_event: Option[LastEvent]) {
    def insert()(implicit c: Connection): Unit = {
      var maybeId = SQL("select id from camera where name = {name}")
        .on("name" -> name)
        .apply
        .headOption.map(row => row[Long]("id"))

      if (maybeId.isEmpty) {
        Logger.debug(s"Pre camera insert $name")
        maybeId = SQL"insert into camera (name) values ($name)".executeInsert()
        Logger.debug(s"Post camera insert $name")
      }

      maybeId match {
        case Some(id) =>
          last_event.foreach(_.insert(id))
        case None => // ignore
          Logger.debug(s"Unable to find camera id")
      }
    }
  }

  case class LastEvent(has_sound: Boolean, start_time: String, end_time: String) {
    def insert(cameraId: Long)(implicit c: Connection): Unit = {

      var startTime = format.parse(start_time)
      val endTime = format.parse(end_time)
      var duration = endTime.getTime - startTime.getTime

      Logger.debug(s"Pre sound_event insert: $cameraId,$startTime,$duration")

      SQL("select count(*) from sound_event where camera_id={cameraId} and start_date = {startTime}")
        .on("cameraId" -> cameraId)
        .on("startTime" -> startTime)
        .apply.headOption.map(row => row[Long]("COUNT(*)")).get match {
        case 1 =>
          Logger.debug(s"Pre sound_event insert: Event was already saved")
          startTime = Calendar.getInstance().getTime()
          duration = 0
        case _ => //ignore
      }

      SQL("insert into sound_event (camera_id, start_date, duration) values ({cameraId}, {startTime}, {duration})")
        .on("cameraId" -> cameraId)
        .on("startTime" -> startTime)
        .on("duration" -> duration)
        .executeInsert()
      Logger.debug("Post sound_event insert ")
    }
  }

  def collectCameraEvents(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val token = Play.application().configuration().getString("nest.auth.token")
    WS.url("https://developer-api.nest.com").
      withHeaders(
        "Accept" -> "application/json",
        "Authorization" -> s"Bearer $token").get() map { r =>

      val cameras = r.json \ "devices" \ "cameras"

      val camerasMap: JsResult[Map[String, CameraObj]] = cameras.validate[Map[String, CameraObj]]
      camerasMap match {
        case map: JsSuccess[Map[String, CameraObj]] =>
          DB.withConnection { implicit c =>
            val camerasList = map.get.values.toList
            Logger.debug(s"Inserting ${camerasList.size} elements")

            try camerasList.foreach(_.insert()) catch {
              case e: Exception => Logger.error(e.getMessage)
            }
          }

        case e: JsError => println("Errors: " + JsError.toFlatJson(e).toString())
      }
    }
  }

  def getCameraList: List[Camera] = {
    DB.withConnection { implicit c =>
      SQL("Select * from camera;")
        .as(Camera.parser.*)
    }
  }

  def getSoundEventList(cameraId: Int): List[SoundEvent] = {
    DB.withConnection { implicit c =>
      SQL("Select * from sound_event where camera_id = {camera_id} order by start_date asc;")
        .on("camera_id" -> cameraId)
        .as(SoundEvent.parser.*)
    }
  }

}
