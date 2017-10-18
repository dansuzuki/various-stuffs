package me.dan

import com.twitter.finagle.http.{HttpMuxer, ParamMap, Request, Response}
import com.twitter.finagle.{http, Http, Service, param}
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.filter.CorsFilter
import com.twitter.finagle.http.service.RoutingService
import com.twitter.finagle.http.path._
import com.twitter.finagle.http.Method
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future}

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.collection.JavaConversions._
import scala.sys.process._
import scala.util.Try

/**
 * The router partial function. Partial function because our service function
 * varies obviously.
 */
object HttpRouter {
  def byRequest[REQUEST](routes: PartialFunction[Request, Service[REQUEST, Response]]) =
    new RoutingService(
      new PartialFunction[Request, Service[REQUEST, Response]] {
        def apply(request: Request)       = routes(request)
        def isDefinedAt(request: Request) = routes.isDefinedAt(request)
      })
}


object TheServer extends TwitterServer {

  lazy val futurePool = com.twitter.util.FuturePool.unboundedPool

  /** a TwitterServer setting */
  override def failfastOnFlagsNotParsed: Boolean = true

  /**
    * your application parameters on startup with default values and help
    * messages for -help printouts
    */
  val port = flag("port", ":8888", "The port where the server will listen to")
  val maxConcurrent = flag("maxconn", 32, "Maximum concurrent transactions.")
  val maxQueued = flag("maxqueue", 128, "Maximum queued requests after reaching maxconn.")


  /**
   * Here is our routes
   */
  final val APIService = Root / "prefix" / "api" / "v1"
  val routingService =
     HttpRouter.byRequest { case request: Request => {
       log.info("HttpRouter does the routing...")
         (request.method, Path(request.path)) match {
           case Method.Get  -> APIService / "ping"  => ping
           case Method.Get  -> APIService / "params" => serveWithParams
           case Method.Get  -> APIService / "params" / pathParam => serveWithPathParam(pathParam)
           case Method.Post -> APIService / "data" => postWithContent
         }
      }
     }

  /**
   * Main entry point, using def main() because TwitterServer has the actual
   * def main with args. Remember that arguments are pre-parsed for us.
   */
  def main() {
    /**
     * Take note of the corsFilter filter. This make all services pass thru
     * corsFilter first.
     */
    HttpMuxer.addRichHandler("/", corsFilter andThen(routingService))

    val server = Http
      .server
      // so we can see this server on the admin page
      .configured(param.Label(this.getClass.getName))
      .withAdmissionControl
      .concurrencyLimit(maxConcurrent.getWithDefault.get, maxQueued.getWithDefault.get)
      .serve(port.getWithDefault.get, HttpMuxer)
    onExit {
      server.close()
    }
    Await.ready(server)
  }

  /**
   * Cross Origin Resource Sharing = CORS
   * Because we are doing an API server so cross-domain request should be ok.
   */
  def corsPolicy = Cors.Policy(
    allowsOrigin = (s: String) => Some("*"),
    allowsMethods = (s: String) => Some(Seq("GET", "POST")),
    allowsHeaders = (ss: Seq[String]) => Some(ss),
    supportsCredentials = false)

  def corsFilter = new Cors.HttpFilter(corsPolicy)

  /**
   * sample GET Service
   */
  def ping = new Service[Request, Response] {
    def apply(req: http.Request) : Future[http.Response] = {
      futurePool {
        log.info("serving a ping get request...")
        val resp = http.Response(req.version, http.Status.Ok)
        resp.setContentString("pong @ " + System.currentTimeMillis)
        resp
      }
    }
  }

  /**
   * sample POST Service with data
   */
  def postWithContent = new Service[Request, Response] {
    def apply(req: http.Request) : Future[http.Response] = {
      Future {
        log.info("here is the content: " + req.contentString)
        val resp = http.Response(req.version, http.Status.Ok)
        resp
      }
    }
  }

  /**
   * sample GET Service with query parameters
   */
  def serveWithParams = new Service[Request, Response] {
    def apply(req: http.Request) : Future[http.Response] = {
      Future {
        log.info("here are the params: " + req.params)
        req.params.foreach(kv => {
          log.info("params: key = " + kv._1 + ", value = " + kv._2)
        })
        val resp = http.Response(req.version, http.Status.Ok)
        resp
      }
    }
  }

  /**
   * sample GET Service with path parameters
   */
  def serveWithPathParam(theParam: String) = new Service[Request, Response] {
    def apply(req: http.Request) : Future[http.Response] = {
      Future {
        log.info("here the path param is: " + theParam)
        val resp = http.Response(req.version, http.Status.Ok)
        resp
      }
    }
  }

}
