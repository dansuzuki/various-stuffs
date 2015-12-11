package me.dan.spark

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

object ProcessMe {

  val conf = new SparkConf().setMaster("local[*]").setAppName(this.getClass.getCanonicalName)
  
  val sc = new SparkContext(conf)
  
  
  def processNow() : Long = {
    
    val l = (1 to 1000)
    val rdd = sc.parallelize(l)
    rdd.count
  }
  
  def stop() {
    sc.stop()
  }

}