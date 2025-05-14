package build
import mill._, scalalib._

object foo extends ScalaModule {
  def scalaVersion = "3.3.6"

  override def ivyDeps = Agg(
    ivy"org.apache.spark::spark-core:3.5.5".withDottyCompat(scalaVersion()),
    ivy"org.apache.spark::spark-sql:3.5.5".withDottyCompat(scalaVersion())
  )

  override def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
//
//  object test extends ScalaTests {
//    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.8.5")
//    def testFramework = "utest.runner.Framework"
//
//    def forkArgs = Seq("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
//  }

}