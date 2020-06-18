package com.article.contracts

import com.article.states.CakeRequestState
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

  private val cakeRequest = CakeRequestState(baker.party, CakeType.Eccles, customer.party)
  private val cake = cakeRequest.accept()
  private val commandSigners = listOf(baker.publicKey, customer.publicKey)

  @Test
  fun testGoodContract() {

    mockServiceHub.ledger(notary.party) {

      transaction {
        command(commandSigners, CakeContract.Commands.Request())
        output(CakeContract.ID, "request", cakeRequest)
        verifies()
      }

      transaction {
        command(commandSigners, CakeContract.Commands.Bake())
        input("request")
        output(CakeContract.ID, "cake", cake)
        verifies()
      }

      transaction {
        command(commandSigners, CakeContract.Commands.Eat())
        input("cake")
        verifies()
      }

    }
  }

  @Test
  fun rejectCakeWithoutRequest() {
    mockServiceHub.ledger(notary.party) {
      transaction {
        command(commandSigners, CakeContract.Commands.Bake())
        output(CakeContract.ID, "cake", cake)
        fails()
      }
    }
  }

  @Test
  fun rejectRequestWithMonMatchingRequest() {
    mockServiceHub.ledger(notary.party) {
      transaction {
        command(commandSigners, CakeContract.Commands.Request())
        output(CakeContract.ID, "request", cakeRequest)
        verifies()
      }

      transaction {
        command(commandSigners, CakeContract.Commands.Bake())
        input("request")
        output(CakeContract.ID, cake.copy(type = CakeType.UpsideDown))
        fails()
      }
    }
  }

  @Test
  fun onlyRequesterCanRequest() {

    mockServiceHub.ledger(notary.party) {
      transaction {
        command(listOf(baker.publicKey), CakeContract.Commands.Request())
        output(CakeContract.ID, "request", cakeRequest)
        fails()
      }

    }
  }

  @Test
  fun onlyBakerCanBake() {

    mockServiceHub.ledger(notary.party) {
      transaction {
        command(commandSigners, CakeContract.Commands.Request())
        output(CakeContract.ID, "request", cakeRequest)
        verifies()
      }

      transaction {
        command(listOf(customer.publicKey), CakeContract.Commands.Bake())
        input("request")
        output(CakeContract.ID, cake)
        fails()
      }
    }
  }

  @Test
  fun onlyCustomerCanEat() {

    mockServiceHub.ledger(notary.party) {

      transaction {
        command(commandSigners, CakeContract.Commands.Request())
        output(CakeContract.ID, "request", cakeRequest)
        verifies()
      }

      transaction {
        command(commandSigners, CakeContract.Commands.Bake())
        input("request")
        output(CakeContract.ID, "cake", cake)
        verifies()
      }

      transaction {
        command(listOf(baker.publicKey), CakeContract.Commands.Eat())
        input("cake")
        fails()
      }

    }
  }

}
