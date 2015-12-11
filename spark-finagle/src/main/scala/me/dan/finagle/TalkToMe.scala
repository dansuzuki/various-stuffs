package me.dan.finagle

import com.twitter.finagle.{ Http, Service }
import com.twitter.finagle.http
import com.twitter.util.{ Await, Future }
import me.dan.spark._
import scala.sys.Prop
import me.dan.sys.ProcessSpawner

object TalkToMe extends App {

  println(Prop[String]("java.class.path"))
  println(Prop[String]("java.home"))

  def process(resp: http.Response): http.Response = {

    resp.setContentString(ProcessMe.processNow().toString())
    resp
  }

  def greeting(s: String, resp: http.Response): http.Response = {
    resp.setContentString(s)
    resp
  }

  val service = new Service[http.Request, http.Response] {
    def apply(req: http.Request): Future[http.Response] =
      {
        val resp = http.Response(req.version, http.Status.Ok)
        req.path match {

          case "/command" => {
            Future.value(
              {
                val o = ProcessSpawner("find . -type f")
                resp.setContentString(o.toString())
                resp
              })
          }

          case "/process" => {
            Future.value(process(resp))
          }
          case "/greetings" => {
            Future.value(greeting("This is the greeting", resp))
          }
          case _ =>
            Future.value(resp)
        }
      }
  }

  val server = Http.serve(":8080", service)
  Await.ready(server)

}

