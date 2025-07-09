package rdd

import com.typesafe.scalalogging.Logger
import core.{CliParser, Config, Purchase, Shared}
import org.apache.spark.sql.{SaveMode, SparkSession}

object Main {
  private val logger = Logger(getClass.getName)

  def main(args: Array[String]): Unit = {
    val config: Config = CliParser.getConfigOrExit(args)

    val spark = SparkSession.builder()
      .appName("instacart-map-reduce-rdd")
      .getOrCreate()

    val sc = spark.sparkContext

    logger.info(s"Reading from: ${config.input}")

    val raw = sc.textFile(config.input)

    val purchases = raw.map { line =>
      val Array(orderId, itemIdStr) = line.split(",", 2)
      Purchase(orderId.trim.toInt, itemIdStr.trim.toInt)
    }

    val resultRDD = computeWithRdd(purchases)

    val top10 = resultRDD.take(10)
    logger.info(s"""First 10 results:
                   |${top10.mkString("- ", "\n- ", "\n---")}
                   |""".stripMargin)

    val saveMode =
      if (config.forceWrite) SaveMode.Overwrite else SaveMode.ErrorIfExists

    import spark.implicits._
    val df = spark.createDataset(resultRDD)
      .toDF("item1", "item2", "count")

    logger.info(
      s"Writing to ${config.output} with force-write=${config.forceWrite}"
    )

    df.write.mode(saveMode).csv(config.output)

    spark.stop()
  }

  private def computeWithRdd(purchases: org.apache.spark.rdd.RDD[Purchase])
  : org.apache.spark.rdd.RDD[(Int, Int, Long)] = {
    logger.info("Using RDD")

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
