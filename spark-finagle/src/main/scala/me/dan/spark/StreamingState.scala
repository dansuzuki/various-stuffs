package me.dan.spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds

object StreamingState extends App {

  val sparkConf = new SparkConf().setAppName("Streaming State").setMaster("local[*]")
  val ssc = new StreamingContext(sparkConf, Seconds(5))

  val lines = ssc.socketTextStream("0.0.0.0", 9999)
  val words = lines.flatMap(_.split(" "))
  val pairs = words.map(word => (word, 1))
  val wordCounts = pairs.reduceByKey(_ + _)

  
  wordCounts.print(10000)
  
  
  ssc.start()
  ssc.awaitTermination()
  ssc.stop(true, true)

}