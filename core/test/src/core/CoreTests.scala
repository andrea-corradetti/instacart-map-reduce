package core

import org.apache.spark.sql.SparkSession
import utest.{*, TestSuite}

object CoreTests extends TestSuite{
  val spark = SparkSession
    .builder()
    .appName("Test Spark")
    .config("spark.master", "local[*]")
    .getOrCreate()

  val tests = Tests{

  }
}