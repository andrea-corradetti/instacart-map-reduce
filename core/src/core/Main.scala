package core

import com.typesafe.scalalogging.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._

object Main {
  private val logger = Logger(getClass.getName)

  def main(args: Array[String]): Unit = {
    val config = CliParser.parse(args) match {
      case Some(c) => c
      case None    => sys.exit(1)
    }

    val spark: SparkSession = SparkSession
      .builder()
      .appName("instacart-map-reduce")
      .getOrCreate()

    import spark.implicits._

    val schema = Encoders.product[Purchase].schema
    val purchases = spark.read.schema(schema).csv(config.input).as[Purchase]

    logger.info(s"""Using Schema:
        |${purchases.schema.treeString}
        |""".stripMargin)

    val writableDf: DataFrame =
      if (config.useRdd) computeWithRdd(spark)(purchases)
      else computeWithDataset(spark)(purchases)

    writableDf.cache()

    val head = writableDf.take(10)
    logger.info(s"""Printing first 10:
                   |${head.mkString("- ", "\n- ", "\n---")}
                   |""".stripMargin)

    val saveMode =
      if (config.forceWrite) SaveMode.Overwrite else SaveMode.ErrorIfExists

    logger.info(
      s"Writing to ${config.output} with force-write=${config.forceWrite}"
    )
    writableDf.write.mode(saveMode).csv(config.output)

    spark.stop()
  }


  private def computeWithDataset(
      spark: SparkSession
  )(purchases: Dataset[Purchase]) = {
    import spark.implicits._

    val combinations =
      purchases.groupByKey(_.orderId).flatMapGroups { (_, itemIds) =>
        itemIds.toStream.distinct.map(_.itemId).combinations(2).map {
          case Seq(itemA, itemB) => sortPair(itemA, itemB)
        }
      }

    val counts: Dataset[((Int, Int), Long)] =
      combinations.groupByKey(identity).count().sort($"count(1)".desc)

    //noinspection Duplicates
    //duplication is lexical but the types are completely different
    val writableDf: DataFrame = counts
      .map { case ((a, b), count) => (a, b, count) }
      .toDF("item1", "item2", "count")

    writableDf
  }

  private def computeWithRdd(
      spark: SparkSession
  )(purchases: Dataset[Purchase]) = {
    import spark.implicits._

    val combinations = purchases.rdd
      .map(p => (p.orderId, p.itemId))
      .groupByKey()
      .flatMap { case (_, itemIds) =>
        itemIds.toStream.distinct.combinations(2).map { case Seq(itemA, itemB) =>
          sortPair(itemA, itemB)
        }
      }

    val counts: RDD[((Int, Int), Int)] = combinations
      .map(pair => (pair, 1))
      .reduceByKey(_ + _)
      .sortBy(_._2)

    //noinspection Duplicates
    //duplication is lexical but the types are completely different
    val writableDf: DataFrame = counts
      .map { case ((a, b), count) => (a, b, count) }
      .toDF("item1", "item2", "count")

    writableDf
  }

  private def sortPair(itemA: Int, itemB: Int) = {
    if (itemA <= itemB) (itemA, itemB) else (itemB, itemA)
  }

}
