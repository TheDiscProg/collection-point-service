package simex.collectionpoint.config

import simex.caching.config.HazelcastConfig

object ServiceConfig {

  def getHazelcastConfig(caching: Option[HazelcastConfig]): HazelcastConfig =
    caching match {
      case Some(config) => config
      case None =>
        HazelcastConfig(
          clusterName = "shareprice",
          clusterAddress = "localhost",
          ports = "5701",
          outwardPort = "4700-34710",
          authTokenTTL = 0
        )
    }

}
