package me.dan.finagle

import com.twitter.concurrent.AsyncStream
import com.twitter.conversions.time._
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.{Http, Service}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Await, Future, JavaTimer}


import java.io.File

import scala.io.Source
import scala.util.Random

/**
 * An example of a streaming HTTP server using chunked transfer encoding.
 */
object StreamingServer {
  val random = new Random
  implicit val timer = new JavaTimer

  // Int, sleep, repeat.
  /*
  def ints(): AsyncStream[Int] =
    random.nextInt +::
      AsyncStream.fromFuture(Future.sleep(100.millis)).flatMap(_ => ints())
  */

  def main(args: Array[String]): Unit = {
    def service = new Service[Request, Response] {

      val itr = Source.fromFile(new File("pom.xml")).getLines.toIterator

      def getNext: Future[Option[String]] = Future.value({
        if(itr.hasNext) Some(itr.next)
        else None
      })

      def mkStream: AsyncStream[Option[String]] = AsyncStream.fromFuture(getNext) ++ mkStream

      // Only one stream exists.
      // @volatile private[this]
      val messages: AsyncStream[Buf] = mkStream takeWhile(_ isDefined) map(l => Buf.Utf8(l.get + "\n"))

      // Allow the head of the stream to be collected.
      //messages.foreach(_ => messages = messages.drop(1))

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

    Await.result(Http.server
      // Translate buffered writes into HTTP chunks.
      .withStreaming(enabled = true)
      // Listen on port 8080.
      .serve("0.0.0.0:58888", service)
    )
  }
}
