package proofspace.trustregistry.n2n

import com.dimafeng.testcontainers.{ContainerDef, MongoDBContainer}
import com.dimafeng.testcontainers.munit.TestContainerForAll


class TestCardanoTestNetContracts extends munit.FunSuite with TestContainerForAll with TrustRegistryFixturesWithContainer {

  
  
  override val containerDef: ContainerDef = MongoDBContainer.Def("mongo:8.0")

  

}
