package com.article.states

import com.article.contracts.CakeContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class CakeType { Chocolate, Cheese, Banana, Eccles, UpsideDown }

@BelongsToContract(CakeContract::class)
data class CakeRequestState(val baker: Party, val type: CakeType, val customer: Party) : ContractState {
  override val participants: List<AbstractParty> = listOf(baker, customer)
  fun accept(): CakeState = CakeState(baker, type, customer)
}

@BelongsToContract(CakeContract::class)
data class CakeState(val baker: Party, val type: CakeType, val customer: Party) : ContractState {
  override val participants: List<AbstractParty> = listOf(baker, customer)
}
