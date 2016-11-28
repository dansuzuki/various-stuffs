package me.dan.finagle

import com.twitter.concurrent.AsyncStream
import com.twitter.conversions.time._
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.http.path._
import com.twitter.finagle.http.Method
import com.twitter.io.{Buf, BufInputStream, Reader}
import com.twitter.util.{Await, Future, JavaTimer}


import java.io.File

import scala.io.Source
import scala.util.Random

/**
 * An example of a streaming HTTP server using chunked transfer encoding.
 */
object StreamingServer {

  def getStream = new Service[Request, Response] {

    val itr = Source.fromFile(new File("pom.xml")).getLines

    def getNext: Future[Option[String]] = Future.value({
      if(itr.hasNext) Some(itr.next)
      else None
    })

    def mkStream: AsyncStream[Option[String]] = AsyncStream.fromFuture(getNext) ++ mkStream

    def messages: AsyncStream[Buf] = mkStream takeWhile(_ isDefined) map(l => Buf.Utf8(l.get + "\n"))

    def apply(request: Request): Future[Response] = {
      val writable = Reader.writable()
      // Start writing thread.
      messages
        .foreachF(buf => writable.write(buf))
        .onSuccess {
          r => writable
            .close()
            .ensure { println("everthing has been read") }
        }

      Future.value(Response(request.version, Status.Ok, writable))
    }
  }


  def postStream = new Service[Request, Response] {
      def apply(request: Request): Future[Response] = {
        def doAndReturn = {
            // read(1024) foreach(ob => ob foreach(println))
            AsyncStream.fromReader(request.reader) foreach(buf => Source.fromInputStream(new BufInputStream(buf)).getLines.foreach(println))
            Response(request.version, Status.Created)
        }
        val ret = Future.value(doAndReturn)
        println("already returned")
        ret
      }
  }

  val services = HttpRouter.byRequest { request =>
   (request.method, Path(request.path)) match {
     case Method.Get  -> Root => getStream
     case Method.Post -> Root => postStream
   }
 }


  def main(args: Array[String]): Unit = {


    Await.result(Http.server
      // Translate buffered writes into HTTP chunks.
      .withStreaming(enabled = true)
      // Listen on port 8080.
      .serve("0.0.0.0:58888", services)
    )
  }
}
