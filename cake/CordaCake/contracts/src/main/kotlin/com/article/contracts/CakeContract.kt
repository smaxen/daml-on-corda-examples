package com.article.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class CakeContract : Contract {

    companion object {
        const val ID = "com.article.contracts.CakeContract"
    }

    override fun verify(tx: LedgerTransaction) {

    }

    interface Commands : CommandData {
        class Bake : Commands
        class Eat : Commands
    }

}