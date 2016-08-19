package me.dan

import scala.reflect.ClassTag
import scala.util.Try
import sext._

/** the following is from http://www.movable-type.co.uk/scripts/latlong.html */
object haversine {
  final val earthDiameter:Int = 12742 // in kilometres
  final val earthRadius:Int = 6371000 // in metres
  def apply(lat1: Double, long1: Double, lat2: Double, long2: Double): Double = {
    val lat1Radians = Math.toRadians(lat1)
    val lat2Radians = Math.toRadians(lat2)
    val deltaLat = Math.toRadians(lat2 - lat1)
    val deltaLong = Math.toRadians(long2 - long1)
    val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
      Math.cos(lat1Radians) * Math.cos(lat2Radians) *
      Math.sin(deltaLong / 2) * Math.sin(deltaLong / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    earthRadius * c
  }
}

object KDTree {
  abstract class Dimension
  class XDimension extends Dimension
  class YDimension extends Dimension

  val XDim = new XDimension
  val YDim = new YDimension

  abstract class Bias
  object NE extends Bias // north east
  object SE extends Bias // south east
  object NW extends Bias // north west
  object SW extends Bias // south west

  type DD = (Double, Double)

  // lower left, upper right
  case class Box(ll: DD,  ur: DD) {
    val lr: DD = (ur._1, ll._2)
    val ul: DD = (ll._1, ur._2)
    val distance: Double = haversine(ll._1, ll._2, ur._1, ur._2)
  }

  case class Node(
    box: Box,
    left: Option[Node],
    right: Option[Node],
    points: Option[List[DD]]
  )
  def intersects(n: Node)(pt: DD): Boolean =
    n.box.ll._1 < pt._1 && pt._1 < n.box.ur._1 && n.box.ll._2 < pt._2 && pt._2 < n.box.ur._2

  def intersects(bias: Bias)(n: Node)(pt: DD): Boolean = {
    bias match {
        case NE => n.box.ll._1 < pt._1 && pt._1 <= n.box.ur._1 && n.box.ll._2 < pt._2 && pt._2 <= n.box.ur._2
        case SE => n.box.ll._1 < pt._1 && pt._1 <= n.box.ur._1 && n.box.ll._2 <= pt._2 && pt._2 < n.box.ur._2
        case NW => n.box.ll._1 <= pt._1 && pt._1 < n.box.ur._1 && n.box.ll._2 < pt._2 && pt._2 <= n.box.ur._2
        case SW => n.box.ll._1 <= pt._1 && pt._1 < n.box.ur._1 && n.box.ll._2 <= pt._2 && pt._2 < n.box.ur._2
    }
  }

  def whereIsThis(n: Node)(pt: DD): Node = {
    if(n.left == None && n.right == None) n
    else
      if(intersects(n.left.get)(pt)) whereIsThis(n.left.get)(pt)
      else if(intersects(n.right.get)(pt)) whereIsThis(n.right.get)(pt)
      else n
  }

  /**
   * x = latitude
   * y = longitude
   * ((minx, miny), (maxx, maxy))
   */
  final val minMaxCoord = ((90.0, 180.0), (-90.0, -180.0))

  /**
   * entry point
   */
  def apply(list: List[DD]): Node = {
      val sz = list.size
      val dim: Dimension = XDim
      val boxCoord = minMaxXY(list)
      newNode(dim, boxCoord, sz, 10000, list)
  }

  /**
   * returns minimum (x, y), maximum (x, y)
   */
  def minMaxXY(list: List[DD]): (DD, DD) = {
    list.foldLeft(minMaxCoord)((a, c) => {
      val (minx, miny) = a._1
      val (maxx, maxy) = a._2

      val newMinX = if(c._1 < minx) c._1 else minx
      val newMinY = if(c._2 < miny) c._2 else miny
      val newMaxX = if(c._1 > maxx) c._1 else maxx
      val newMaxY = if(c._2 > maxy) c._2 else maxy
      ((newMinX, newMinY), (newMaxX, newMaxY))
    })
  }

  /**
   * functions to do depending on the dimension being used
   */
  def atXorY[T: ClassTag](atX: => T, atY: => T)(dim: Dimension) : T = {
    dim match {
      case _: XDimension => atX
      case _: YDimension => atY
    }
  }

  def swapDim = atXorY(YDim, XDim) _

  /**
   * just a helper function to return Some(T) on true, None on false
   */
  def ifElseNone[T: ClassTag](cond: Boolean)(ret: => T): Option[T] = {
    if(cond) Some(ret)
    else None
  }

  /**
   * recursive entry
   */
  def newNode(dim: Dimension,
    boxCoord: (DD, DD),
    sz: Int,
    max: Int,
    ldd: List[DD]): Node = {

    val sorted = ldd.sortBy(dd => atXorY(dd._1, dd._2)(dim))
    val box = Box(boxCoord._1, boxCoord._2)
    val asParent: Boolean = if(box.distance > max && sz > 4) true else false
    def ifParent[T: ClassTag](ret: => T) = ifElseNone[T](asParent)(ret)

    val leftSize = if((sz / 2) > 0) sz / 2 else 1
    val rightSize = sz - leftSize

    val l: List[DD] = sorted.take(leftSize)
    val r: List[DD] = sorted.takeRight(rightSize)

    // left's upper right point
    val lur: Option[DD] = ifParent[DD]({
      atXorY[DD](
        { (l.last._1, boxCoord._2._1) },
        { (boxCoord._2._1, l.last._2) }
      )(dim)
    })

    // rights's lower left point
    val rll = ifParent {
      atXorY(
        (l.last._1, boxCoord._1._2),
        (boxCoord._1._1, l.last._2)
      )(dim)
    }

    val lbox: Option[(DD, DD)] = ifParent { (boxCoord._1, lur.get) }
    val rbox: Option[(DD, DD)] = ifParent { (rll.get, boxCoord._2) }

    val nextDim = swapDim(dim)

    Node(box,
      ifParent { newNode(nextDim, lbox.get, leftSize, max, l)},
      ifParent { newNode(nextDim, rbox.get, rightSize, max, r)},
      if(!asParent) Some(ldd) else None
    )
  }

  @throws(classOf[Exception])
  def main(args: Array[String]) {

    val points = (1 to 10000).map(n => (n / 1.0, n / 2.0)).toList
    val tree = KDTree(points)
    //println(tree.valueTreeString)
    println(whereIsThis(tree)((9823.0, 4916.5)))
  }
}
