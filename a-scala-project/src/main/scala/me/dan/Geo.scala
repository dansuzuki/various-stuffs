package me.dan

import scala.math._

/** the following is from http://www.movable-type.co.uk/scripts/latlong.html */
object Geo {

  case class Bearing(value: Double)
  object ToNorth extends Bearing(0)
  object ToEast extends Bearing(90)
  object ToSouth extends Bearing(180)
  object ToWest extends Bearing(270)

  final val earthDiameter: Int = 12742 // in kilometres
  final val earthRadius: Int = 6371000 // in metres, R

  // from https://rosettacode.org/wiki/Haversine_formula#Scala
  val R = 6372.8
  def haversinex(lat1: Double, lon1: Double, lat2: Double, lon2: Double) = {
    import scala.math._
     val dLat=(lat2 - lat1).toRadians
     val dLon=(lon2 - lon1).toRadians

     val a = pow(sin(dLat/2),2) + pow(sin(dLon/2),2) * cos(lat1.toRadians) * cos(lat2.toRadians)
     val c = 2 * asin(sqrt(a))
     R * c
  }

  /**
   * Distance from point to point
   */
  def haversine(lat1: Double, long1: Double, lat2: Double, long2: Double): Double = {
    val lat1Radians = lat1.toRadians
    val lat2Radians = lat2.toRadians
    val deltaLat = (lat2 - lat1).toRadians
    val deltaLong = (long2 - long1).toRadians
    val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
      cos(lat1Radians) * cos(lat2Radians) *
      sin(deltaLong / 2) * sin(deltaLong / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    earthRadius * c
  }

  /**
   * next point from a given point and distance (in meters)
   */
  def travel(direction: Bearing)(distance: Double)(lat1: Double, long1: Double): (Double, Double) = {
    val lat1Radians = lat1.toRadians
    val long1Radians = (((long1 + 540) % 360) - 180).toRadians
    val bearingRadians = direction.value.toRadians
    val angularDistance = distance / earthRadius
    /** for lat2
        var φ2 = Math.asin(Math.sin(φ1) * Math.cos(d/R) + Math.cos(φ1) * Math.sin(d / R) * Math.cos(brng));
     */
    val lat2Radians = asin(sin(lat1Radians) * cos(angularDistance) + cos(lat1Radians) * sin(angularDistance) * cos(bearingRadians))

    /** for long2
        var λ2 = λ1 + Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(φ1), Math.cos(d / R) - Math.sin(φ1) * Math.sin(φ2));
     */
    val long2Radians = long1Radians + atan2(sin(bearingRadians) * sin(angularDistance) * cos(lat1Radians), cos(angularDistance) - sin(lat1Radians) * sin(lat2Radians))

    /** now convert to degrees */
    (lat2Radians.toDegrees, long2Radians.toDegrees)
  }

  /** (lat, long) = (y, x) **/
  case class Grid(
    ne: (Double, Double),
    sw: (Double, Double),
    nw: (Double, Double),
    se: (Double, Double))

  /**
   * Note: lat = y, long = x
   * will create a geo-grid from a point and distance r (in meters)
   * 1. travel north to get extreme top
   * 2. from north travel west to get extreme left
   * 3. travel south to get extreme bottom
   * 4. derive extreme right from the difference of extreme left and mid
   *
   */
  def selectGrid(lat: Double, long: Double)(dist: Double): Grid = {
    val atNorth = travel(ToNorth)(dist)(lat, long)
    val atWest = travel(ToWest)(dist)(atNorth._1, long)
    val atSouth = travel(ToSouth)(dist)(lat, long)
    val atEast = (lat, (long - atWest._2) + long)
    Grid((atNorth._1, atEast._2), (atSouth._1, atWest._2), (atNorth._1, atWest._2), (atSouth._1, atEast._2))
  }

  /**
   * if two quads intersects
   * take note on latlong, that is, x = long, lat = y
   *
   * from http://stackoverflow.com/questions/13390333/two-rectangles-intersection
   */
  def intersect(
    x: Double, y: Double, a: Double, b: Double, // quad 1
    x1: Double, y1: Double, a1: Double, b1: Double // quad 2
  ): Boolean = !(a < x1 || a1 < x || b < y1 || b1 < y)

  case class Circle(lat: Double, long: Double, radius: Double)
  /**
   * if a point is inside a circle (or point of a circle)
   * from: http://stackoverflow.com/questions/481144/equation-for-testing-if-a-point-is-inside-a-circle
   *
   * (x - center_x)^2 + (y - center_y)^2 < radius^2
   *
   */
  def intersect(c: Circle)(lat: Double, long: Double): Boolean =
    haversine(lat, long, c.lat, c.long) <= c.radius


  def main(args: Array[String]) {
    val pointA = (args(0).toDouble, args(1).toDouble)
    val dist = args(2).toDouble
    val atNorth = travel(ToNorth)(dist)(pointA._1, pointA._2)
    val atNEast = travel(ToEast)(dist)(atNorth._1, pointA._2)
    val atEast = travel(ToEast)(dist)(pointA._1, pointA._2)
    val atSouth = travel(ToSouth)(dist)(pointA._1, pointA._2)
    val atSEast = travel(ToEast)(dist)(atSouth._1, pointA._2)

    val midNtoNE = haversine(atNorth._1, pointA._2, atNEast._1, atEast._2)
    val midToEast = haversine(pointA._1, pointA._2, pointA._1, atEast._2)
    val midStoSE = haversine(atSouth._1, pointA._2, atSEast._1, atEast._2)

    println(atNorth + " >>> " + atNEast + " d => " + midNtoNE)
    println(pointA  + " >>> " + atEast  + " d => " + midToEast)
    println(atSouth + " >>> " + atSEast + " d => " + midStoSE)
    println("----")
    val grid = selectGrid(pointA._1, pointA._2)(dist)
    println(grid)
    println(haversine(grid.sw._1, grid.sw._2, grid.ne._1, grid.ne._2))
    println(sqrt(pow(dist*2, 2) + pow(dist*2, 2)))
  }

}
