/**
   Copyright 2012 Greg L. Turnquist

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/* Assume all percents are given in "%" format, while absolutes are in pure decimal format */
object Main extends App {

  val snp = List(
    (1951, 16.3), (1952, 11.8), (1953, -6.6), (1954, 26.4), (1955, 26.4),
    (1956, 2.6), (1957, -14.3), (1958, 38.1), (1959, 8.5), (1960, -3.0),
    (1961, 23.1), (1962, -11.8), (1963, 18.9), (1964, 13.0), (1965, 9.1),
    (1966, -13.1), (1967, 20.1), (1968, 7.7), (1969, -11.4), (1970, 0.1),
    (1971, 10.8), (1972, 15.6), (1973, -17.4), (1974, -29.7), (1975, 31.5),
    (1976, 19.1), (1977, -11.5), (1978, 1.1), (1979, 12.3), (1980, 25.8),
    (1981, -9.7), (1982, 14.8), (1983, 17.3), (1984, 1.4), (1985, 26.3),
    (1986, 14.6), (1987, 2.0), (1988, 12.4), (1989, 27.3), (1990, -6.6), 
    (1991, 26.3), (1992, 4.5), (1993, 7.1), (1994, -1.5), (1995, 34.1),
    (1996, 20.3), (1997, 31.0), (1998, 26.7), (1999, 19.5), (2000, -10.1),
    (2001, -13.0), (2002, -23.4), (2003, 26.4), (2004, 9.0), (2005, 3.0),
    (2006, 13.6), (2007, 3.5), (2008, -38.5), (2009, 23.5), (2010, 12.8))

  def round(x:Double, digits:Int):Double = {
    val factor = math.pow(10, digits)
    math.round(x * factor) / factor
  }

  def aMean(xs: Seq[(Int, Double)]): Double = xs.foldLeft(0.0)((subtotal, relChange) => subtotal + relChange._2) / xs.size

  def absChange(relChange:Double) = 1 + relChange/100.0
  def relChange(absChange:Double) = 100.0 * (absChange - 1.0)
  
  def actualAbsGrowth(xs: Seq[(Int, Double)]): Double = xs.foldLeft(1.0)((subtotal, relChange) => subtotal * absChange(relChange._2))

  def gMean(xs: Seq[(Int, Double)]): Double = {
    relChange(math.pow(actualAbsGrowth(xs), 1.0/xs.size))
  }

  /** apply allows it to sort the right value into the middle, and then pick it 
   *  For example, EiulLimits(0.0, 15.0)(4.0)  would become List(0.0, 4.0, 15.0), with 4.0 being in the middle
   *               EiulLimits(0.0, 15.0)(-2.4  would become List(-2.4, 0.0, 15.0), with 0.0 being in the middle
   *               EiulLimits(0.0, 15.0)(22.5) would become List(0.0, 15.0, 22.5), with 15.0 being in the middle
   */
  case class EiulLimits(lower:Double, upper:Double) {
    def apply(x: Double) = List(x, lower, upper).sorted.apply(1)
  }

  def eiul(xs: Seq[(Int, Double)], limits: EiulLimits): Seq[(Int, Double)] = {
    xs.map { case(year, relChange) => (year, limits(relChange)) }
  }

  def series(xs: Seq[(Int, Double)], years: Int) = {
    xs.sliding(years).map(sublist => 
      (sublist(0)._1, sublist.takeRight(1)(0)._1, aMean(sublist), gMean(sublist))
    ).toList
  }

  def stddev(xs: Seq[Double]): Double = {
    val mean = xs.sum/xs.size
    val squareSum = xs.foldLeft(0.0)((subtotal, item) => subtotal + math.pow(item - mean, 2))
    math.sqrt(squareSum/xs.size)
  }

  def stats(xs: Seq[(Int, Int, Double, Double)]) = {
    val aMeans = xs.map(_._3)
    val gMeans = xs.map(_._4)
    Map("average geom mean" -> round(gMeans.sum/xs.size, 2), "min geom mean" -> round(gMeans.min, 2), "max geom mean" -> round(gMeans.max, 2),
        "stddev" -> stddev(gMeans))
  }

  println("S&P 500 performance = " + snp)
  println("Arithmetic mean = " + aMean(snp) + "%")
  println("Geometric mean = " + gMean(snp) + "%" )
  println("Actual total growth factor = " + actualAbsGrowth(snp))
  println

  val eiulData = eiul(snp, EiulLimits(0.0, 15.0))

  println("EIUL performance = " + eiulData)
  println("EIUL arithmetic performance = " + aMean(eiulData) + "%")
  println("EIUL geometric performance = " + gMean(eiulData) + "%")
  println("Actual EIUL total growth factor = " + actualAbsGrowth(eiulData))
  println

  for {
    window <- List(10, 15, 20, 25, 30)
  } {
    val snpStats = stats(series(snp, window))
    val eiulStats = stats(series(eiulData, window))
    println(window + "-year stats")
    for {
      stats <- List(("S&P 500", snpStats), ("EIUL", eiulStats))
    } {
      stats match {
        case (desc, stats) => println(desc + " stats: " + 
                                      " Avg geom mean = " + stats("average geom mean") + 
                                      " (" + stats("min geom mean") + ".." + stats("max geom mean") + ") 68% chance +/- " + 
                                      stats("stddev"))
      }
    }
    println
  }
}
