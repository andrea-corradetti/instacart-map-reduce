package rdd

import com.typesafe.scalalogging.Logger
import shared.{CliParser, Config, Purchase, Shared}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SparkSession

object Main {
  private val logger = Logger("RddMain")

  def main(args: Array[String]): Unit = {
    val config: Config = CliParser.getConfigOrExit(args)

    val spark = SparkSession
      .builder()
      .appName("instacart-map-reduce-rdd")
      .getOrCreate()

    val sc = spark.sparkContext

    logger.info(s"Reading from: ${config.input} into RDD with ${sc.defaultParallelism} partitions")

    val raw = sc.textFile(config.input, sc.defaultParallelism)

    val purchases = raw.map { line =>
      val Array(orderId, itemIdStr) = line.split(",", 2)
      Purchase(orderId.trim.toInt, itemIdStr.trim.toInt)
    }

    val resultRDD = computeWithRdd(purchases)
    resultRDD.cache()

    val top10 = resultRDD.take(10)
    logger.info(s"""First 10 results:
                   |${top10.mkString("- ", "\n- ", "\n---")}
                   |""".stripMargin)

    val csvLines = resultRDD.map { case (item1, item2, count) =>
      s"$item1,$item2,$count"
    }

    logger.info(
      s"Writing to ${config.output} with force-write=${config.forceWrite}"
    )

    val fs =
      FileSystem.get(new java.net.URI(config.output), sc.hadoopConfiguration)
    val output = new Path(config.output)

    if (config.forceWrite) {
      fs.delete(output, true)
    } else if (fs.exists(output)) {
      throw new RuntimeException(
        s"Output path ${config.output} already exists and forceWrite=false"
      )
    }

    csvLines.saveAsTextFile(config.output)
  }

  private def computeWithRdd(
      purchases: org.apache.spark.rdd.RDD[Purchase]
  ): org.apache.spark.rdd.RDD[(Int, Int, Long)] = {

    val combinations = purchases
      .map(p => (p.orderId, p.itemId))
      .groupByKey()
      .flatMapValues(_.toSet.toSeq.combinations(2))
      .map { case (_, Seq(a, b)) => Shared.sortPair(a, b) }

    val counts = combinations
      .map(pair => (pair, 1L))
      .reduceByKey(_ + _)
      .map { case ((a, b), count) => (a, b, count) }
      .sortBy(_._3, ascending = false)

    counts
  }
}
