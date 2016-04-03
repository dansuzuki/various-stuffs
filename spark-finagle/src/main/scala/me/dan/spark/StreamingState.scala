package me.dan.spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import akka.actor.Actor
import org.apache.spark.streaming.receiver.ActorHelper
import akka.actor.Props
import org.apache.spark.SparkEnv
import org.apache.spark.rdd.PairRDDFunctions
import org.apache.spark.rdd.EmptyRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

class CustomActor extends Actor with ActorHelper {
  def receive = {
    case data: String => {
      //println(">>> received a message: \"" + data + "\"")
      store(data)

    }
  }
}

object CustomActor {
  val name: String = "CustomActor"
}

object StreamingState extends App {

  // entry point here
  run()

  def run() {
    val sparkConf = new SparkConf()
      .setAppName("Streaming State")
      .setMaster("local[*]")
      .set("spark.akka.logLifecycleEvents", "true")
      .set("spark.logConf", "true")

    val ssc = new StreamingContext(sparkConf, Seconds(5))
    ssc.checkpoint("./tmp/" + this.getClass.getCanonicalName)

    val lines = ssc.actorStream[String](Props[CustomActor], CustomActor.name)
    val words = lines.flatMap(_.split(" "))
    val pairs = words.map(word => (word, 1))
    val wordCounts = pairs.reduceByKey(_ + _)

    val updated = wordCounts.updateStateByKey(updateFunc)
    updated.print(20)
    val transformed = updated.transform(rdd => {
      val filtered = rdd.filter(!_._1.equals("the"))
      filtered.checkpoint()
      filtered
    })
    transformed.print(20)
    ssc.start()
    ActorSender(SparkEnv.get.actorSystem, ssc.sparkContext.getConf)
    ssc.awaitTermination()
    ssc.stop(true, true)

  }
  def updateFunc(newValues: Seq[Int], state: Option[Int]): Option[Int] = {
    Some(newValues.sum + state.getOrElse(0))
  }
}