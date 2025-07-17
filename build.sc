package build

import mill._
import mill.scalalib._

object shared extends ScalaModule {
  def scalaVersion = "2.12.18"

  override def ivyDeps = Agg(
    ivy"com.github.scopt::scopt:4.1.1-M3",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.5",
  )
}

object rddApp extends SparkAppModule {
  override def mainClass = Some("rdd.Main")
}

object datasetApp extends SparkAppModule {
  override def mainClass = Some("dataset.Main")
}

trait SparkAppModule extends ScalaModule {
  def scalaVersion = shared.scalaVersion

  override def moduleDeps = Seq(shared)

  override def compileIvyDeps = Agg(
    ivy"org.apache.spark::spark-core:3.5.5",
    ivy"org.apache.spark::spark-sql:3.5.5"
  )

  override def prependShellScript = ""

  override def forkArgs = Seq(
    "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"
  )
}