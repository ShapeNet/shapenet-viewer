name := "shapenet-viewer"

version := "0.1.0"

scalaVersion := "2.11.7"

exportJars := true

publishTo := Some("dovahkiin" at "https://dovahkiin.stanford.edu/artifactory/repo")

// JMonkeyEngine https://bintray.com/jmonkeyengine/org.jmonkeyengine
resolvers += "bintray-jmonkeyengine-org.jmonkeyengine" at "http://dl.bintray.com/jmonkeyengine/org.jmonkeyengine"

// Nifty for jme3 3.1.0-SNAPSHOT
resolvers += "nifty-maven-repo.sourceforge.net" at "http://nifty-gui.sourceforge.net/nifty-maven-repo"

// Disable parallel execution in test
parallelExecution in Test := false

// Xml dependencies for SceneInteractionAnnotationTool...
// add scala-xml dependency when needed (for Scala 2.11 and newer) in a robust way
// this mechanism supports cross-version publishing
// taken from: http://github.com/scala/scala-module-dependency-sample
libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    // if scala 2.11+ is used, add dependency on scala-xml module
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3",
        "org.scala-lang.modules" %% "scala-swing" % "1.0.1")
    case _ =>
      // or just libraryDependencies.value if you don't depend on scala-swing
      libraryDependencies.value :+ "org.scala-lang" % "scala-swing" % scalaVersion.value
  }
}

libraryDependencies ++= Seq(
  // Stuff from jmonkey engine maven (see http://wiki.jmonkeyengine.org/doku.php/jme3:maven)
  // other ones to include? networking, jogg, terrain, blender
  "org.jmonkeyengine" % "jme3-core" % "3.1.0-beta1",
  "org.jmonkeyengine" % "jme3-plugins" % "3.1.0-beta1",
  "org.jmonkeyengine" % "jme3-effects" % "3.1.0-beta1",
  //"org.jmonkeyengine" % "jme3-jbullet" % "3.1.0-beta1",
  "org.jmonkeyengine" % "jme3-niftygui" % "3.1.0-beta1",
  "org.jmonkeyengine" % "jme3-desktop" % "3.1.0-beta1",
  "org.jmonkeyengine" % "jme3-lwjgl" % "3.1.0-beta1",
  "org.bushe" % "eventbus" % "1.4",
  // For ply loading
  "org.smurn" % "jply" % "0.2.0",
  // Basic data structures
  "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.3.1",
  // Logging
  "org.slf4j" % "slf4j-api" % "1.7.9",
  "org.slf4j" % "slf4j-ext" % "1.7.9", // Extensions
  "ch.qos.logback" % "logback-classic" % "1.0.13", // Logback (main slf4j logging impl)
  "org.slf4j" % "log4j-over-slf4j" % "1.7.9", // Capture log4j messages
  "org.slf4j" % "jul-to-slf4j" % "1.7.9", // Capture java logging messages
  "uk.org.lidalia" % "sysout-over-slf4j" % "1.0.2", // Capture stderr, stdout message
  "org.fusesource.jansi" % "jansi" % "1.8", // Ansi colors for windows
  "commons-logging" % "commons-logging" % "1.2", // Logging for solr hhtpclient
  //
  // Json/Yaml/Csv/ProtoBuf
  // Automatic java to json serialization
  "com.cedarsoftware" % "json-io" % "2.5.2",
  "org.yaml" % "snakeyaml" % "1.12",
  "com.googlecode.json-simple" % "json-simple" % "1.1.1",
  "org.json4s" %% "json4s-native" % "3.3.0",
//  "org.json4s" %% "json4s-jackson" % "3.3.0",
  "net.sf.opencsv" % "opencsv" % "2.3",
  // Compressions (bzip)
  "org.apache.commons" % "commons-compress" % "1.4.1",
  // Simple math/matrix
  "org.apache.commons" % "commons-math3" % "3.5",
  // Indices/Databases
  "org.apache.solr" % "solr-solrj" % "5.3.1",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "org.sql2o" % "sql2o" % "1.5.4",
  "mysql" % "mysql-connector-java" % "5.1.36",
  // Web
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  // Utilities
//  "com.google.guava" % "guava" % "18.0", 
//  "com.fasterxml.jackson.core" % "jackson-core" % "2.4.3",
//  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.4.3",
//  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.3",
  // Jline for command console
  "jline" % "jline" % "2.12.1",
  // Options
  "net.sf.jopt-simple" % "jopt-simple" % "4.5",
  // Config and Actors (is this needed?)
  "com.typesafe" % "config" % "1.3.0",
  // Scala arm for autoclose of resources
  "com.jsuereth" %% "scala-arm" % "1.4",
  // Test
  "org.assertj" % "assertj-core" % "3.1.0" % "test",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test"
)

//unmanagedBase := baseDirectory.value / "../lib"
