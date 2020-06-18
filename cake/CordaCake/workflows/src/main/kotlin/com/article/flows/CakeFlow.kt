package com.article.flows

import co.paralleluniverse.fibers.Suspendable
import com.article.contracts.CakeContract
import com.article.states.CakeState
import com.article.states.CakeType
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

private fun ServiceHub.notary() = networkMapCache.notaryIdentities.first()

object CakeFlow {

    @InitiatingFlow
    @StartableByRPC
    class Bake(val type: CakeType, val customer: Party) : FlowLogic<StateAndRef<CakeState>>() {

        @Suspendable
        override fun call(): StateAndRef<CakeState> {

            val state = CakeState(ourIdentity, type, customer)

            val command = Command(CakeContract.Commands.Bake(), state.participants.map{it.owningKey})

            val txBuilder = TransactionBuilder(serviceHub.notary())
                .addOutputState(state, CakeContract.ID)
                .addCommand(command)

            val sessions: List<FlowSession> = listOf(initiateFlow(customer))

            val initialTx = serviceHub.signInitialTransaction(txBuilder)

            val signedTx: SignedTransaction = subFlow(CollectSignaturesFlow(initialTx, sessions))

            val finalTx = subFlow(FinalityFlow(signedTx, sessions))

            return finalTx.tx.outRefsOfType<CakeState>().single()
        }
    }

    @InitiatedBy(Bake::class)
    class BakeResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signedTransactionFlow: FlowLogic<SignedTransaction> = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) = stx.verify(serviceHub, false)
            }
            val signedTransaction = subFlow(signedTransactionFlow)
            subFlow(ReceiveFinalityFlow(counterpartySession, signedTransaction.id))
        }
    }

    @InitiatingFlow
    @StartableByRPC
    class Eat(val cakeRef: StateAndRef<CakeState>) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {

            val cake = cakeRef.state.data

            val command = Command(CakeContract.Commands.Eat(), cake.participants.map{it.owningKey})

            val txBuilder = TransactionBuilder(serviceHub.notary())
                .addInputState(cakeRef)
                .addCommand(command)

            val sessions: List<FlowSession> = listOf(initiateFlow(cake.baker))

            val initialTx = serviceHub.signInitialTransaction(txBuilder)

            val signedTx: SignedTransaction = subFlow(CollectSignaturesFlow(initialTx, sessions))

            subFlow(FinalityFlow(signedTx, sessions))

        }
    }

    @InitiatedBy(Eat::class)
    class EatResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signedTransactionFlow: FlowLogic<SignedTransaction> = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) = stx.verify(serviceHub, false)
            }
            val signedTransaction = subFlow(signedTransactionFlow)
            subFlow(ReceiveFinalityFlow(counterpartySession, signedTransaction.id))
        }
    }

}