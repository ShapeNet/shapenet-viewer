package edu.stanford.graphics.shapenet.util

import org.slf4j.LoggerFactory
import java.io.File

import org.slf4j.bridge.SLF4JBridgeHandler
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J

/**
 * Logging utilities
 * @author Angel Chang
 */
object Logger {
  setup()
  
  private def setup(): Unit = {
    // Forward system out and err to slf4j
    SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
    Console.setOut(System.out)
    Console.setErr(System.err)

    // Forward j.u.l to slf4j
    // Optionally remove existing handlers attached to j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger()  // (since SLF4J 1.6.5)

    // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
    // the initialization phase of your application
    SLF4JBridgeHandler.install()
  }

  private val DEFAULT_PATTERN = "%date %level [%thread] %logger{10} [%file:%line] %msg%n"

  def appendToFile(filename: String,
                   pattern: String = DEFAULT_PATTERN,
                   loggerName: String = org.slf4j.Logger.ROOT_LOGGER_NAME,
                   /* set to true if root should log too */
                   additive: Boolean = false) = {
    import ch.qos.logback.classic.spi.ILoggingEvent
    import ch.qos.logback.classic.Level
    import ch.qos.logback.classic.LoggerContext
    import ch.qos.logback.classic.encoder.PatternLayoutEncoder
    import ch.qos.logback.core.FileAppender

    // Make sure log directory is created
    val file: File = new File(filename)
    val parent: File = file.getParentFile
    if (parent != null) parent.mkdirs

    val loggerContext = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
    val logger = loggerContext.getLogger(loggerName)

    // Setup pattern
    val patternLayoutEncoder = new PatternLayoutEncoder()
    patternLayoutEncoder.setPattern(pattern)
    patternLayoutEncoder.setContext(loggerContext)
    patternLayoutEncoder.start()

    // Setup appender
    val fileAppender = new FileAppender[ILoggingEvent]()
    fileAppender.setFile(filename)
    fileAppender.setEncoder(patternLayoutEncoder)
    fileAppender.setContext(loggerContext)
    fileAppender.start()

    // Attach appender to logger
    logger.addAppender(fileAppender)
    //logger.setLevel(Level.DEBUG)
    logger.setAdditive(additive)

    fileAppender.getName
  }

  def detachAppender(appenderName: String, loggerName: String = org.slf4j.Logger.ROOT_LOGGER_NAME): Unit = {
    import ch.qos.logback.classic.LoggerContext

    val loggerContext = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
    val logger = loggerContext.getLogger(loggerName)
    logger.detachAppender(appenderName)
  }

  def getLogger(clazz: Class[_]): org.slf4j.Logger = {
    LoggerFactory.getLogger(clazz)
  }

  def getLogger(name: String): org.slf4j.Logger = {
    LoggerFactory.getLogger(name)
  }
}

trait Loggable {
  lazy val logger = Logger.getLogger(this.getClass)

  def startTrack(name: String): Unit = {
    logger.debug("Starting " + name)
  }

  def endTrack(name: String): Unit = {
    logger.debug("Finished " + name)
  }
}

