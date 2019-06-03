resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.2")

addSbtPlugin("fr.iscpif" % "scalajs-execnpm" % "0.7")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.27")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.7")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.0")
