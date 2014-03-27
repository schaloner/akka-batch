import sbt._
import Keys._

object AkkaBatchBuild extends Build {

    lazy val publishM2Configuration = TaskKey[PublishConfiguration]("publish-m2-configuration", "Configuration for publishing to the .m2 repository.")

    lazy val publishM2 = TaskKey[Unit]("publish-m2", "Publishes artifacts to the .m2 repository.")

    lazy val m2Repo = Resolver.file("publish-m2-local", Path.userHome / ".m2" / "repository") 

    publishM2Configuration <<= (packagedArtifacts, checksums in publish, ivyLoggingLevel) map { (arts, cs, level) =>
      Classpaths.publishConfig(arts, None, resolverName = m2Repo.name, checksums = cs, logging = level)
    }

    publishM2 <<= Classpaths.publishTask(publishM2Configuration, deliverLocal)
    
    otherResolvers += m2Repo
}
