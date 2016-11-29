package me.dan.finagle


import com.twitter.concurrent.AsyncStream
import com.twitter.conversions.time._
import com.twitter.util.{Await, Base64StringEncoder => Base64, Future, JavaTimer}
import com.twitter.finagle.http.{Request, Method, Status, Version}
import com.twitter.finagle.http.path._
import com.twitter.finagle.Http
import com.twitter.io.{Buf, Reader}

import java.io.File

import scala.io.Source
import scala.util.Random

object StreamingClient extends App {

  val random = new Random
  implicit val timer = new JavaTimer

  val limit: Long = args(0).toLong
  var counter: Long = 0

  def anInt: Option[Int] = {
    //Thread.sleep(1)
    counter = counter + 1
    if(counter < limit) Some(random.nextInt)
    else None
  }

  def streamInt: AsyncStream[Option[Int]] = AsyncStream.of(anInt) ++ streamInt


  val client = Http.client.withStreaming(enabled = true).newService("localhost:58888")

  val writable = Reader.writable()
  streamInt
    .takeWhile(_ isDefined)
    .map(n => {
      val v = n.get.toString
      println(v)
      Buf.Utf8(v)
    })
    .foreachF(writable.write)
    .onSuccess {
      case _ => writable.close.ensure { println("everything has been read") }
    }

  val request = Request(Version.Http11, Method.Post, "/", writable)

  Await.ready(client(request)
    .onSuccess {
      case resp if resp.status == Status.Created => {
        println("its good!")
        System.exit(0)
      }
      case _ => println("baaaadddd!!!")
    }
  )

  //Thread.sleep(1000)
}
