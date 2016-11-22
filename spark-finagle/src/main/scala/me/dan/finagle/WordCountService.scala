package me.dan.finagle


import me.dan.spark.{WordCountExecutor => WCE}

import collection.JavaConversions._

import com.twitter.finagle.http.{HttpMuxer, ParamMap, Request, Response}
import com.twitter.finagle.{http, Http, Service, param}
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.filter.CorsFilter
import com.twitter.finagle.http.service.RoutingService
import com.twitter.finagle.http.path._
import com.twitter.finagle.http.Method
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future}

object WordCountService {

  def download(jobId: Int) = service { (req: http.Request) => {
      val resp = http.Response(req.version, http.Status.Ok)
      resp.contentString = WCE.output(jobId).mkString("\n")
      resp
    }
  }

  def checkStatus(jobId: Int) = service { (req: http.Request) => {
      val resp = http.Response(req.version, http.Status.Ok)
      resp.contentString = WCE.status(jobId).name
      resp.contentType = "html/text"
      resp
    }
  }

  def clear(jobId: Int) = service { (req: http.Request) => {
      val resp = http.Response(req.version, http.Status.Ok)
      WCE.clear(jobId)
      resp
    }
  }

  def process = service { (req: http.Request) => {
      val resp = http.Response(req.version, http.Status.Ok)
      val jobId = WCE.id

      WCE.process(jobId, req.contentString.split(",").map(_.toInt).toList)

      resp.contentString = jobId.toString
      resp.contentType = "html/text"
      resp
    }
  }

  /** just a dummy upload, not multipart, getting from a contentString instead */
  def upload = service { (req: http.Request) => {
      val resp = http.Response(req.version, http.Status.Created)
      val contentId = WCE.id
      WCE.newContent(WCE.Content(contentId, req.contentString))
      resp.contentString = contentId.toString
      resp.contentType = "html/text"
      resp
    }
  }



  def routes(prefix: Path) = HttpRouter.byRequest { request =>
    (request.method, Path(request.path)) match {
      case Method.Post  -> prefix / "data" => upload
      case Method.Post -> prefix / "jobs"  => process
      case Method.Get -> prefix / "jobs" / id / "output" => download(id.toInt)
      case Method.Get -> prefix / "jobs" / id / "status" => checkStatus(id.toInt)
      case Method.Delete -> prefix / "jobs" / id => clear(id.toInt)

    }
  }
}
