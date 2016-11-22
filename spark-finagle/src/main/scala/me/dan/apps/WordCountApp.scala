package me.dan.apps


import com.twitter.finagle.http.{HttpMuxer, ParamMap, Request, Response}
import com.twitter.finagle.{http, Http, Service, param}
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.filter.CorsFilter
import com.twitter.finagle.http.service.RoutingService
import com.twitter.finagle.http.path._
import com.twitter.finagle.http.Method
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future}


import me.dan.finagle.{WordCountService, _}

object WordCountApp extends TwitterServer {

  override def failfastOnFlagsNotParsed: Boolean = true
  val port = flag("port", ":8080", "The port where the server will listen to")
  val maxConcurrent = flag("maxconn", 32, "Maximum concurrent transactions.")
  val maxQueued = flag("maxqueue", 128, "Maximum queued requests after reaching maxconn.")

  val prefix = Root / "word-count" / "api"

  def main() {
    HttpMuxer.addRichHandler("/", corsFilter andThen(WordCountService.routes(prefix)))
    val server = Http
      .server
      .configured(param.Label(this.getClass.getName))
      .withAdmissionControl
      .concurrencyLimit(maxConcurrent.getWithDefault.get, maxQueued.getWithDefault.get)
      .serve(port.getWithDefault.get, HttpMuxer)
    onExit {
      server.close()
    }
    Await.ready(server)

  }

}
