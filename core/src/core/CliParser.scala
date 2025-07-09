package core

import com.typesafe.scalalogging.Logger
import scopt.OParser

case class Config(
    input: String = "",
    output: String = "",
    forceWrite: Boolean = false,
    useRdd: Boolean = false
)

object CliParser {
  private val logger = Logger(getClass.getName)

  private val builder = OParser.builder[Config]
  private val parser = {
    import builder._
    OParser.sequence(
      programName("instacart-map-reduce"),
      opt[String]('i', "input")
        .required()
        .valueName("<path>")
        .action((x, c) => c.copy(input = x))
        .text("input file is required"),
      opt[String]('o', "output")
        .required()
        .valueName("<path>")
        .action((x, c) => c.copy(output = x))
        .text("output location is required"),
      opt[Unit]('f', "force-write")
        .action((_, c) => c.copy(forceWrite = true))
        .text("overwrite output directory."),
      opt[Unit]("use-rdd")
        .action((_, c) => c.copy(useRdd = true))
        .text("use rdd instead of dataset.")
    )
  }

  private def parse(args: Array[String]): Option[Config] = {
    OParser.parse(parser, args, Config()) match {
      case Some(config) =>
        logger.debug("Cli Args:")
        logger.debug(s"|Input: ${config.input}")
        logger.debug(s"|Output: ${config.output}")
        logger.debug(s"|ForceWrite: ${config.forceWrite}")
        logger.debug(s"|use-rdd: ${config.useRdd}")
        Some(config)
      case _ =>
        None // arguments are bad, error message will have been displayed
    }
  }

  def getConfigOrExit(args: Array[String]): Config =
    CliParser.parse(args) match {
      case Some(c) => c
      case None    => sys.exit(1)
    }

}
