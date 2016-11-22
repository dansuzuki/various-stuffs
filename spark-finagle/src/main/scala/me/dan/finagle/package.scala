package me.dan

import com.twitter.finagle.{http, Http, Service, param}
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.filter.CorsFilter
import com.twitter.finagle.http.{HttpMuxer, ParamMap, Request, Response}
import com.twitter.finagle.http.service.RoutingService
import com.twitter.util.{Await, Future}


package object finagle {

  def corsPolicy = Cors.Policy(
    allowsOrigin = (s: String) => Some("*"),
    allowsMethods = (s: String) => Some(Seq("GET", "POST", "PUT")),
    allowsHeaders = (ss: Seq[String]) => Some(ss),
    supportsCredentials = false)

  def corsFilter = new Cors.HttpFilter(corsPolicy)

  /** service wrapper */
  def service(func: http.Request => http.Response) = new Service[Request, Response] {
   def apply(req: http.Request): Future[http.Response] = Future.value(func(req))
  }


  object HttpRouter {
   def byRequest[REQUEST](routes: PartialFunction[Request, Service[REQUEST, Response]]) =
     new RoutingService(
       new PartialFunction[Request, Service[REQUEST, Response]] {
         def apply(request: Request)       = routes(request)
         def isDefinedAt(request: Request) = routes.isDefinedAt(request)
       })
  }
}
