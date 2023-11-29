package simex.collectionpoint.domain.caching

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import simex.caching.config.HazelcastConfig

trait HazelcastContainer {

  private val HazelcastImage = "hazelcast/hazelcast:latest"
  private val clusterName = "shareprice"

  def setUpHCastContainer(): (HazelcastConfig, GenericContainer[Nothing]) = {
    val container: GenericContainer[Nothing] = new GenericContainer(
      DockerImageName.parse(HazelcastImage)
    ) {
      override def addFixedExposedPort(hostPort: Int, containerPort: Int): Unit =
        super.addFixedExposedPort(hostPort, containerPort)
    }
    container.withExposedPorts(5701)
    container.withEnv("HZ_CLUSTERNAME", clusterName)
    container.start()
    val host = container.getHost
    val port = container.getFirstMappedPort
    val config = HazelcastConfig(
      clusterName = clusterName,
      clusterAddress = host,
      ports = port.toString,
      outwardPort = "",
      authTokenTTL = 300L
    )
    (config, container)
  }
}
