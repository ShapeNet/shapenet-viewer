package edu.stanford.graphics.shapenet.jme3

import com.typesafe.config.{ConfigFactory, Config}
import edu.stanford.graphics.shapenet.util.ConfigHelper
import edu.stanford.graphics.shapenet.jme3.loaders.LoadFormat
import scala.collection.JavaConversions._

/**
 * Configuration for the main JME object
 * @author Angel Chang
 */
class JmeConfig(val modelCacheSize: Option[Int] = None,
                val defaultLoadFormat: Option[LoadFormat.Value] = None) {
}

object JmeConfig {
  // Stupid type-config - have to define defaults for everything....
  val defaults = ConfigFactory.parseMap(
    Map[String,Object](
    )
  )
  def apply(): JmeConfig = JmeConfig(ConfigFactory.empty())
  def apply(inputConfig: Config, name: String = "jme"): JmeConfig = {
    val config = if (inputConfig == null) defaults else inputConfig.withFallback(defaults)
    val configHelper = new ConfigHelper(config)
    new JmeConfig(
      modelCacheSize = configHelper.getIntOption(name + ".modelCacheSize"),
      defaultLoadFormat = configHelper.getStringOption(name + ".defaultLoadFormat").map( s => LoadFormat.withName(s))
    )
  }
}
