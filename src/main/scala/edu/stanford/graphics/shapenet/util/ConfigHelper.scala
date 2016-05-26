package edu.stanford.graphics.shapenet.util

import com.jme3.math.ColorRGBA
import com.typesafe.config.{ConfigFactory, Config}
import edu.stanford.graphics.shapenet.Constants
import scala.collection.JavaConversions._
import joptsimple.{OptionException, OptionParser}
import java.io.File

/**
 * Config helper
  *
  * @author Angel Chang
 */
class ConfigHelper(val config: Config) {
  def getBoolean(s: String, default: Boolean = false) =
    if (config.hasPath(s)) config.getBoolean(s) else default
  def getBooleanOption(s: String) =
    if (config.hasPath(s)) Option(config.getBoolean(s)) else None

  def getInt(s: String, default: Int = 0) =
    if (config.hasPath(s)) config.getInt(s) else default
  def getIntOption(s: String) =
    if (config.hasPath(s)) Option(config.getInt(s)) else None

  def getDouble(s: String, default: Double = 0.0) =
    if (config.hasPath(s)) config.getDouble(s) else default
  def getDoubleOption(s: String) =
    if (config.hasPath(s)) Option(config.getDouble(s)) else None

  def getFloat(s: String, default: Float = 0.0f) =
    if (config.hasPath(s)) config.getDouble(s).toFloat else default
  def getFloatOption(s: String) =
    if (config.hasPath(s)) Option(config.getDouble(s).toFloat) else None

  def getString(s: String, default: String = null) =
    if (config.hasPath(s)) config.getString(s) else default
  def getStringOption(s: String) =
    if (config.hasPath(s)) Option(config.getString(s)) else None

  def getStringList(s: String, default: Seq[String] = null) =
    if (config.hasPath(s)) config.getStringList(s).map( x => x.toString ) else default

  def getColor(s: String, default: ColorRGBA = null) =
    if (config.hasPath(s)) {
      val colorStr = config.getString(s)
      val c = java.awt.Color.decode(colorStr)
      new ColorRGBA(c.getRed/255.0f, c.getGreen/255.0f, c.getBlue/255.0f, c.getAlpha/255.0f)
    } else {
      default
    }
}

trait MutableConfigHelper {
  case class ConfigOption[T](name: String, gloss: String, getValue: Unit => T, setValue: String => Unit, supportedValues: Seq[String] = null) {
  }

  protected val mutableOptions = new scala.collection.mutable.HashMap[String, ConfigOption[_]]
  def registerMutable[T](name: String, gloss: String, getValue: Unit => T, setValue: String => Unit, supportedValues: Seq[String] = null) = {
    val option = ConfigOption[T](name, gloss, getValue, setValue, supportedValues)
    mutableOptions.put(name, option)
    option
  }
  def registerMutableBoolean(name: String, gloss: String, getValue: Unit => Boolean, setValue: Boolean => Unit) = {
    def set(s: String): Unit = {
      val flag = ConfigHelper.parseBoolean(s)
      setValue(flag)
    }
    registerMutable[Boolean](name, gloss, getValue, set, Seq("on", "off"))
  }
  def getOptionNames = mutableOptions.keySet
  def getOptions = mutableOptions.values
  def getOption(name: String) = mutableOptions.getOrElse(name, null)
}

class ConfigManager(config: Config) extends ConfigHelper(config) with MutableConfigHelper {
  def getConfig(): Config = config
}

trait ConfigHandler {
  def init(config: ConfigManager)
  def onUpdate(config: ConfigManager) = init(config)
}

object ConfigHelper {
  def getBoolean(s: String, default: Boolean = false)(implicit config: Config) =
    if (config != null && config.hasPath(s)) config.getBoolean(s) else default
  def getBooleanOption(s: String)(implicit config: Config) =
    if (config != null && config.hasPath(s)) Option(config.getBoolean(s)) else None

  def getInt(s: String, default: Int = 0)(implicit config: Config) =
    if (config != null && config.hasPath(s)) config.getInt(s) else default
  def getIntOption(s: String)(implicit config: Config) =
    if (config != null && config.hasPath(s)) Option(config.getInt(s)) else None

  def getDouble(s: String, default: Double = 0.0)(implicit config: Config) =
    if (config != null && config.hasPath(s)) config.getDouble(s) else default
  def getDoubleOption(s: String)(implicit config: Config) =
    if (config != null && config.hasPath(s)) Option(config.getDouble(s)) else None

  def getFloat(s: String, default: Float = 0.0f)(implicit config: Config) =
    if (config.hasPath(s)) config.getDouble(s).toFloat else default
  def getFloatOption(s: String)(implicit config: Config) =
    if (config.hasPath(s)) Option(config.getDouble(s).toFloat) else None

  def getString(s: String, default: String = null)(implicit config: Config) =
    if (config != null && config.hasPath(s)) config.getString(s) else default
  def getStringOption(s: String)(implicit config: Config) =
    if (config != null && config.hasPath(s)) Option(config.getString(s)) else None

  def getStringList(s: String, default: List[String] = null)(implicit config: Config): Seq[String] =
    if (config != null && config.hasPath(s)) config.getStringList(s) else default

  def fromString(conf: String): Config = {
    val config = ConfigFactory.parseString(conf)
    config.withFallback(defaultConfig).resolve()
  }

  def fromMap(map: Map[String,String]): Config = {
//    val config = ConfigFactory.parseMap(map)
//    config.withFallback(defaultConfig).resolve()
    val str = map.map( x => x._1 + " = " + x._2 ).mkString("\n")
    fromString(str)
  }

  def fromOptions(args: String*): Config = {
    // Set up command line options
    // create the parser
    val optionsParser = new OptionParser()
    val confOption = optionsParser.accepts( "conf" ).withRequiredArg()
      .withValuesSeparatedBy( ',' ).ofType( classOf[String] ).describedAs( "file" )

    // TODO: Should we just use the AppSettings instead of this extra Config?
    var config = ConfigFactory.empty()
    try {
      // parse the command line arguments
      val options = optionsParser.parse( args:_* )
      if (options.has(confOption)) {
        val conffiles = options.valuesOf(confOption)
        for (conffile <- conffiles) {
          println("Using conf " + conffile)
          val c = ConfigFactory.parseFileAnySyntax(new File(conffile))
          config = config.withFallback(c)
        }
      }
      config.withFallback(defaultConfig).withFallback(ConfigFactory.systemProperties).resolve()
    }
    catch {
      // oops, something went wrong
      case exp: OptionException => {
        System.err.println( "Invalid arguments.  Reason: " + exp.getMessage() )
        optionsParser.printHelpOn( System.out )
        sys.exit(-1)
      }
    }
  }


  def parseBoolean(str: String): Boolean = {
    if (str != null) str.toLowerCase match {
      case "true" => true
      case "false" => false
      case "on" => true
      case "off" => false
      case "enabled" => true
      case "disabled" => false
      case "1" => true
      case "0" => true
      case _ => throw new IllegalArgumentException("Invalid boolean for input string: \""+str+"\"")
    }
    else
      throw new IllegalArgumentException("Invalid boolean input string: \"null\"")
  }

  def getSupportedBooleanStrings = Seq("true", "false", "on", "off", "enabled", "disabled", "1", "0")
  def getDefaultConfig = defaultConfig

  private val defaultConfig = ConfigFactory.parseMap(
    Map(
      "HOME_DIR" -> Constants.HOME_DIR,
      "CODE_DIR" -> Constants.CODE_DIR,
      "DATA_DIR" -> Constants.DATA_DIR,
      "WORK_DIR" -> Constants.WORK_DIR,
      "TEST_DIR" -> Constants.TEST_DIR,
      "LOG_DIR" -> Constants.LOG_DIR,
      "CACHE_DIR" -> Constants.CACHE_DIR,
      "ASSETS_DIR" -> Constants.ASSETS_DIR,
      "SHAPENET_VIEWER_DIR" -> Constants.SHAPENET_VIEWER_DIR
    )
  )
}
