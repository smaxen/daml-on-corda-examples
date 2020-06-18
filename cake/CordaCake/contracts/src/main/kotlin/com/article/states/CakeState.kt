package com.article.states

import com.article.contracts.CakeContract
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class CakeType { Chocolate, Cheese, Banana, Eccles, UpsideDown }

@BelongsToContract(CakeContract::class)
data class CakeState(val baker: Party, val type: CakeType, val owner: Party) : ContractState {
  override val participants: List<AbstractParty> = listOf(baker, owner)
}
