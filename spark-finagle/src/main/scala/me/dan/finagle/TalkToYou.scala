package me.dan.finagle

import com.twitter.finagle.{ Http, Service }
import com.twitter.finagle.http
import com.twitter.util.{ Await, Future }

object TalkToYou extends App {
  
  
  val client: Service[http.Request, http.Response] = Http.newService(":8080")
  
  val request = http.Request(http.Method.Get, "/command")
  val response: Future[http.Response] = client(request)
  response.onSuccess(resp => println(resp.getContentString()))
  Await.ready(response)
  Thread.sleep(1000)
  //println(response.get().getStatusCode())
  //println(response.get().getContentString())


  
}