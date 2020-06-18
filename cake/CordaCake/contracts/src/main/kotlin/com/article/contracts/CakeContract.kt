package com.article.contracts

import com.article.states.CakeRequestState
import com.article.states.CakeState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction

class CakeContract : Contract {

    companion object {
        const val ID = "com.article.contracts.CakeContract"
    }

    interface Commands : CommandData {
        class Request : Commands
        class Bake : Commands
        class Eat : Commands
    }

  override fun verify(tx: LedgerTransaction) {

    val totalSize = tx.inputs.size + tx.outputs.size

    val command = tx.commands.requireSingleCommand<Commands>()
    when (command.value) {
      is Commands.Request -> {
        require(totalSize == 1)
        val request = tx.outRefsOfType<CakeRequestState>().single()
        require(command.signers.contains(request.state.data.customer.owningKey)) {
          "Request not signed by customer"
        }
      }
      is Commands.Bake -> {
        require(totalSize == 2)
        val request = tx.inRefsOfType<CakeRequestState>().single()
        val cake = tx.outRefsOfType<CakeState>().single()
        require(cake.state.data == request.state.data.accept()) {
          "Delivered cake does not match that requested"
        }
        require(command.signers.contains(cake.state.data.baker.owningKey)) {
          "Bake not signed by baker"
        }
      }
      is Commands.Eat -> {
        require(totalSize == 1)
        val cake = tx.inRefsOfType<CakeState>().single()
        require(command.signers.contains(cake.state.data.customer.owningKey)) {
          "Eat not signed by customer"
        }

      }
      else -> error("Expected Request, Bake or Eat")
    }
  }

}