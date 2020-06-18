package com.article.contracts

import com.article.states.CakeState
import com.article.states.CakeType
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class CakeContractTest {

  private val notary = TestIdentity(CordaX500Name("notary", "London", "GB"))
  private val baker = TestIdentity(CordaX500Name("Cake Thief Bakery", "London", "GB"))
  private val customer = TestIdentity(CordaX500Name("Ella", "London", "GB"))

  private val mockServiceHub = MockServices(cordappPackages = listOf("com.article.states", "com.article.contracts"))

  @Test
  fun testGoodContract() {

    mockServiceHub.ledger(notary.party) {

      val commandSigners = listOf(baker.publicKey)

      transaction {
        command(commandSigners, CakeContract.Commands.Bake())
        output(CakeContract.ID, CakeState(baker.party, CakeType.Eccles, customer.party))
        verifies()    // fails()
      }

    }
  }


}
