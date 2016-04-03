package me.dan.spark

import org.apache.spark.SparkEnv
import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.apache.spark.SparkConf
import scala.concurrent.Await

object ActorSender {

  implicit val ec = ExecutionContext.global
  def apply(system: ActorSystem, conf: SparkConf) {
    Future {
      //val url = "akka.tcp://spark@" + conf.get("spark.driver.host") + ":" + conf.get("spark.driver.port") + "user/Supervisor0/" + CustomActor.name
      val actorReceiver = system.actorSelection("akka://sparkDriver/user/Supervisor0/CustomActor")
      println("Now ready to send messages!!!")
      while (true) {
        actorReceiver ! "the quick brown fox jumps over the lazy dog"
        //println(">>> sent a message")
        Thread.sleep(1000)
      }
    }
  }

}