package controllers

import play.api.mvc._
import services.ApiServer
import services.ApiServer._
import spatutorial.shared.Api

import scala.concurrent.ExecutionContext.Implicits.global


object Application extends Controller {


  def index = Action {
    Ok(views.html.index("SPA tutorial"))
  }

  def autowireApi(path: String) = Action.async(parse.raw) {
    implicit request =>
      println(s"Request path: $path")

      // get the request body as Array[Byte]
      val b = request.body.asBytes(parse.UNLIMITED).get

      // call Autowire route
      ApiServer.route(path, b).map(Ok(_))

  }

  def logging = Action(parse.anyContent) {
    implicit request =>
      request.body.asJson.foreach { msg =>
        println(s"CLIENT - $msg")
      }
      Ok("")
  }
}
