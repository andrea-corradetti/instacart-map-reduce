package foo

import org.apache.spark.sql.SparkSession
import utest.*
import scala3encoders.given

object FooTests extends TestSuite{
  val spark = SparkSession
    .builder()
    .appName("Test Spark")
    .config("spark.master", "local[*]")
    .getOrCreate()

  val schema = scala3encoders.encoder[Purchase].schema
  val orderProductsCsv = getClass.getResource("/order_products.csv")
  val purchases = spark.read.schema(schema).csv(orderProductsCsv.getPath).as[Purchase]
  purchases.printSchema()

  val tests = Tests{

  }
}