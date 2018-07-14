enablePlugins(ScalaJSPlugin)

name := "topshell"
organization := "com.github.ahnfelt"
version := "0.1-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")
libraryDependencies += "com.github.ahnfelt" %%% "react4s" % "0.9.14-SNAPSHOT"
libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.2"
libraryDependencies += "com.github.werk" %%% "router4s" % "0.1.0-SNAPSHOT"

scalaVersion := "2.12.4"

scalaJSUseMainModuleInitializer := true
