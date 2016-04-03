package me.dan.spark

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

object DateOps {
  val fmt = DateTimeFormat.forPattern("HHmm");
  
  
  def timeInHHmm(): String = fmt.print(new DateTime())
 
  
  def pastHHmm(n:Int): String = fmt.print(new DateTime().minusMinutes(n))
}