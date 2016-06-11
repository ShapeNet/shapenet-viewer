package edu.stanford.graphics.shapenet.jme3.viewer

import java.io.File

import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.common.CategoryUtils
import edu.stanford.graphics.shapenet.util.{ConfigHelper, IOUtils}
import de.lessvoid.nifty.controls.{ConsoleCommands, Console}
import de.lessvoid.nifty.controls.ConsoleCommands.ConsoleCommand

/**
 * Command Console for the viewer
 * @author Angel Chang
 */
class CommandConsole(val controller: ViewerController,
                     val console: Console) {

  private var consoleCommands: ConsoleCommands = null
  def viewer = controller.viewer
  def nifty = controller.nifty

  def init() {
    // output hello to the console
    console.output("Hello! :) Welcome to the ShapeNet viewer console.  Use F4 to toggle the console on/off.")
    console.output("Type 'help' for a list of the commands")

    // create the console commands class and attach it to the console
    consoleCommands = new ConsoleCommands(nifty, console)

    // register some mutable configuration for set command
    registerConfig()

    // register commands
    for (command <- commands) {
      consoleCommands.registerCommand(command.name, command)
      for (alias <- command.aliases) {
        consoleCommands.registerCommand(alias, command)
      }
      for (args <- command.registerArgs) {
        consoleCommands.registerCommand(command.name + " " + args, command)
      }
    }

    // finally enable command completion
    consoleCommands.enableCommandCompletion(true)
  }

  def registerConfig(): Unit = {
    viewer.config.registerMutableBoolean("offscreenDisplay", "Show offscreen display",
      x => viewer.getOffScreenDisplay.isEnabled(),
      flag => viewer.getOffScreenDisplay.setEnabled(flag)
    )
  }

  def run(str: String) {
    // TODO: Make this more generic...
    val args = str.split(" +")
    val command = consoleCommands.findCommand(args(0))
    command.execute(args:_*)
  }

  private class LoadCommand extends BasicConsoleCommand {
    override def name = "load"
    override def description = "Load model"
    override def aliases = Seq("l")
    override def registerArgs = Seq("model", "model random")
    override def extendedDescription =
        "load <id> - Load specified scene or model\n" +
        "load model random [<source]] - Load random model\n" +
        "load model <id> [<transform>]- Load specified model (with optional transform in row major)"
    override def executeImpl(args: Array[String]) {
      try {
        args match {
          case Array(_, "model", "random", source, category) => {
            viewer.loadModelRandom(source, CategoryUtils.normalize(category))
          }
          case Array(_, "model", "random", source) => {
            viewer.loadModelRandom(source)
          }
          case Array(_, "model", "random") => {
            viewer.loadModelRandom()
          }
          case Array(_, "model", loadId, transform) => {
            val transformMatrix4f = edu.stanford.graphics.shapenet.jme3.jsonArrayStringToMatrix4f(transform)
            val rowMajor = true
            if (rowMajor) {
              transformMatrix4f.transposeLocal()
            }
            viewer.loadModel(loadId, transform = transformMatrix4f)
          }
          case Array(_, "model", loadId) => {
            viewer.loadModel(loadId)
          }
          case Array(_, loadId) => {
            viewer.load(loadId)
          }
          case _ => {
            console.output("Invalid load parameters")
          }
        }
      } catch {
        case ex:Throwable => {
          // TODO: Rework error recovery - capture load errors where they happen and propagate
          val argsStr = "'" + args.mkString(" ") + "'"
          console.output("Error invoking command " + argsStr)
          if (viewer.state == ViewerState.LOAD || viewer.state == ViewerState.BUSY) {
            viewer.state = ViewerState.READY
          }
          viewer.logger.warn("Error invoking command " + argsStr, ex)
        }
      }
    }
  }

  private class SaveCommand extends BasicConsoleCommand {
    override def name = "save"
    override def description = "Save model screenshots"
    override def registerArgs = Seq(
      "model stats", "model stats query", "model stats test",
      "model screenshots", "model screenshots query", "model screenshots test"
    )
    override def extendedDescription =
      "save model screenshots test|test <modelId>}<filename> - Takes screenshots for models and saves them away\n" +
      "save model stats test|test <modelId>}<filename> - Get statistics for models and saves them away"
    override def executeImpl(args: Array[String]) {
      args match {
        case Array(_, "model", "screenshots" | "stats", "query", query, limit) => {
          val modelIds = viewer.dataManager.solrQuerier.queryModelIds(query, limit.toInt, Seq(), false)
          processModels(args(2), modelIds.map( x => x._1))
        }
        case Array(_, "model", "screenshots" | "stats", "all") => {
          val modelIds = viewer.dataManager.getModelIds()
          processModels(args(2), modelIds.toSeq)
        }
        case Array(_, "model", "screenshots" | "stats", "all", source) => {
          val modelIds = viewer.dataManager.getModelIds(source, null)
          processModels(args(2), modelIds.toSeq)
        }
        case Array(_, "model", "screenshots" | "stats", "all", source, category) => {
          val modelIds = viewer.dataManager.getModelIds(source, category)
          processModels(args(2), modelIds.toSeq,
            Option(viewer.screenShotDir + File.separator + "modelsByCategory" + File.separator + category + File.separator))
        }
        case Array(_, "model", "screenshots" | "stats", "test", modelId) => {
          val modelIds = Seq(modelId)
          processModels(args(2), modelIds.toSeq)
        }
        case Array(_, "model", "screenshots" | "stats", "test") => {
          val modelIds = viewer.scene.scene.objects.map( x => x.fullId() ).distinct
          processModels(args(2), modelIds.toSeq)
        }
        case Array(_, "model", "screenshots" | "stats", filename) => {
          val modelIds = IOUtils.getLines(filename)
          processModels(args(2), modelIds.toSeq)
        }
        case Array(_, "model", "screenshots" | "stats") => {
          val modelIds = viewer.scene.scene.objects.map( x => x.fullId() ).distinct
          processModels(args(2), modelIds.toSeq)
        }
        case _ => {
          console.output("Invalid save parameters")
        }
      }
    }
    private def processModels(cmd: String, modelIds: Iterable[String], outputDir: Option[String] = None): Unit = {
      cmd match {
        case "stats" => viewer.saveModelStats(modelIds, Constants.WORK_DIR + "modelstats.csv")
        case "screenshots" => viewer.saveModelScreenshots(modelIds, outputDir)
      }
    }

  }

  private class PrintCommand extends BasicConsoleCommand {
    override def name = "print"
    override def description = "Print information"
    override def aliases = Seq("p")
    override def registerArgs = Seq("hierarchy", "objects",
      "info model", "info selected")
    override def extendedDescription =
      "print hierarchy - Print scene hierarchy\n" +
      "print objects - Print information about objects in the scene\n" +
      "print info model <modelId> - Print information about the specified modelId\n" +
      "print info selected - Print information about the selected object"

    override def executeImpl(args: Array[String]) {
      args match {
        case Array(_, "hierarchy") => {
          val scene = viewer.scene
          if (scene != null && scene.scene != null) {
            val str = scene.scene.sceneHierarchyString(viewer.dataManager)
            console.output(str)
          } else {
            console.output("No scene is loaded")
          }
        }
        case Array(_, "objects") => {
          val scene = viewer.scene
          if (scene != null) {
            for (mi <- scene.modelInstances) {
              console.output("obj" + mi.index + ": " + mi.model.name + ", id=" + mi.model.fullId)
            }
          } else {
            console.output("No scene is loaded")
          }
        }
        case Array(_, "info", "model", modelId) => {
          val modelStats = viewer.getModelStats(modelId)
          console.output(modelStats.mkString("\n"))
        }
        case Array(_, "info", "model") => {
          val selected = viewer.scene.modelInstances.map( x => x.model.fullId ).distinct
          for (modelId <- selected) {
            val modelStats = viewer.getModelStats(modelId)
            console.output(modelStats.mkString("\n"))
          }
        }
        case Array(_, "info", "selected") => {
          val selected = viewer.getSelectedModelInstances.map( x => x.model.fullId )
          for (modelId <- selected) {
            val modelStats = viewer.getModelStats(modelId)
            console.output(modelStats.mkString("\n"))
          }
        }
        case _ => {
          console.output("Invalid print parameters")
        }
      }
    }
  }

  private class ShowCommand extends BasicConsoleCommand {
    override def name = "show"
    override def description = "Show various things for the scene"
    override def aliases = Seq("s")
    override def registerArgs = _getArgs()
    private def _getArgs(): Seq[String] = Seq("meshes", "meshes withWireframe", "mesh hierarchy")
    override def extendedDescription =
      "show hierarchy - Show scene hierarchy\n" +
        "show mesh hierarchy - Show mesh hierarchy\n" +
        "show meshes - Shows meshes of selected object" +
        "show meshes withWireframe - Shows meshes of selected object with wireframe lines"

    override def executeImpl(args: Array[String]) {
      args match {
        // Show meshes
        case Array(_, "meshes") => {
          viewer.debugVisualizer.showMeshes(false)
        }
        case Array(_, "meshes", "withWireframe") => {
          viewer.debugVisualizer.showMeshes(true)
        }
        case Array(_, "mesh", "hierarchy") => {
          viewer.showMeshTreePanel(viewer.scene.node)
        }
      }
    }

  }

  private class SetCommand extends BasicConsoleCommand {
    override def name = "set"
    override def description = "Set options"
    override def registerArgs = _getOptionNameArgs
    override def extendedDescription = _getOptionDescs().mkString("\n")
//      "set sceneLayout grid|probabilistic - Set sceneLayout"
    private def _getOptionNameArgs(): Seq[String] = {
      val nameArgs = new scala.collection.mutable.ArrayBuffer[String]()
      for (option <- viewer.config.getOptions) {
        val args = if (option.supportedValues != null) option.supportedValues else Seq()
        nameArgs.append(option.name)
        for (arg <- args) {
          nameArgs.append(option.name + " " + arg)
        }
      }
      nameArgs.toSeq
    }
    private def _getOptionDescs(): Seq[String] = {
      val descs = for (option <- viewer.config.getOptions) yield {
        "set " + option.name + " - " + option.gloss
      }
      descs.toSeq
    }
    override def executeImpl(args: Array[String]) {
      args match {
        case Array(_, optionName, value) => {
          val option = viewer.config.getOption(optionName)
          if (option != null) {
            try {
              option.setValue(value)
            } catch {
              case ex: IllegalArgumentException => {
                console.output("Invalid value " + value + " for " + optionName)
              }
            }
          } else {
            console.output("Invalid set parameters - unknown option " + optionName)
          }
        }
        case Array(_, optionName) => {
          val option = viewer.config.getOption(optionName)
          if (option != null) {
            console.output(optionName + " = " + option.getValue())
          } else {
            console.output("Invalid set parameters - unknown option " + optionName)
          }
        }
        case _ => {
          console.output("Invalid set parameters")
        }
      }
    }
  }

  private class OptimizeCommand extends BasicConsoleCommand {
    override def name = "optimize"
    override def description = "Optimize"
    override def registerArgs = Seq("cameraPosition")
    override def extendedDescription =
      "optimize cameraPosition"
    override def executeImpl(args: Array[String]) {
      args match {
        case Array(_, "cameraPosition") => {
          viewer.optimizeCameraPosition()
        }
        case _ => {
          console.output("Invalid parameters for " + name)
        }
      }
    }
  }

  private class RegisterCommand extends BasicConsoleCommand {
    override def name = "register"
    override def description = "Register"
    override def registerArgs = Seq("shapeNetCore")
    override def extendedDescription =
      "register shapeNetCore <dir>"
    override def executeImpl(args: Array[String]) {
      args match {
        case Array(_, "shapeNetCore", dir) => {
          viewer.dataManager.registerShapeNetCore(dir)
        }
        case Array(_, "shapeNetSem", dir) => {
          viewer.dataManager.registerShapeNetSem(dir)
        }
        case _ => {
          console.output("Invalid parameters for " + name)
        }
      }
    }
  }

  private class HelpCommand extends BasicConsoleCommand {
    override def name = "help"
    override def description = "Help"
    override def aliases = Seq("h")
    override def executeImpl(args: Array[String]) {
      if (args.length <= 1) {
        for (command <- commands) {
          console.output(command.name + " - " + command.description)
        }
      } else {
        val name = args(1)
        val command = commandMap.getOrElse(name, null)
        if (command != null) {
          console.output(command.extendedDescription)
        } else {
          console.output("Unknown command " + name)
        }
      }
    }
  }

  private class ClearCommand extends BasicConsoleCommand {
    override def name = "clear"
    override def aliases = Seq("c")
    override def description = "Clear selected nodes and debug markers"
    override def executeImpl(args: Array[String]) {
      args match {
        case Array(_, "cache") => {
          viewer.jme.clearCache(true)
        }
        case _ => {
          viewer.clearSelectedNodes()
        }
      }
    }
  }

  private class ExitCommand extends BasicConsoleCommand {
    override def name = "exit"
    override def description = "Exit the application"
    override def executeImpl(args: Array[String]) {
      viewer.stop()
    }
  }

  lazy val commands = Array(
    new ClearCommand(),
    new LoadCommand(),
    new SetCommand(),
    new OptimizeCommand(),
    new SaveCommand(),
    new RegisterCommand(),
    new PrintCommand(),
    new ShowCommand(),
    new ExitCommand(),
    new HelpCommand()
  )
  lazy val commandMap = commands.view.map(c => (c.name, c)).toMap ++
    commands.map( c => c.aliases.map( x => x -> c) ).flatten.toMap

  trait BasicConsoleCommand extends ConsoleCommand {
    def name: String
    def description: String
    def aliases: Seq[String] = Seq()
    def registerArgs: Seq[String] = Seq() // Arguments to be registered so they show up in the command history
    def extendedDescription: String = description
    def executeImpl(args: Array[String])  // Please implement me!!!

    override def execute(strings: String*): Unit = {
      this.executeWrapper(strings.toArray)
    }

    def executeWrapper(args: Array[String]) {
      // Guard against exception - no exception doesn't mean program will proceed normally though
      // Save our history...
      val name = args(0)
      try {
        executeImpl(args)
      } catch {
        case ex: Throwable => {
          console.output("Error executing " + args.mkString(" "))
          ex.printStackTrace()
        }
      }
    }
    def parseBoolean(str: String): Boolean = {
      ConfigHelper.parseBoolean(str)
    }
  }
}

