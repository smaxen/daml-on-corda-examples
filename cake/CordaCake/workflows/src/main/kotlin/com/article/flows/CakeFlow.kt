package com.article.flows

import co.paralleluniverse.fibers.Suspendable
import com.article.contracts.CakeContract
import com.article.states.CakeRequestState
import com.article.states.CakeState
import com.article.states.CakeType
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

private fun ServiceHub.notary() = networkMapCache.notaryIdentities.first()

object CakeFlow {

    @InitiatingFlow
    @StartableByRPC
    class Request(val type: CakeType, val baker: Party) : FlowLogic<StateAndRef<CakeRequestState>>() {

        @Suspendable
        override fun call(): StateAndRef<CakeRequestState> {

            val state = CakeRequestState(baker, type, ourIdentity)

            val command = Command(CakeContract.Commands.Request(), state.participants.map{it.owningKey})

            val txBuilder = TransactionBuilder(serviceHub.notary())
                .addOutputState(state, CakeContract.ID)
                .addCommand(command)

            val sessions: List<FlowSession> = listOf(initiateFlow(baker))

            val initialTx = serviceHub.signInitialTransaction(txBuilder)

            val signedTx: SignedTransaction = subFlow(CollectSignaturesFlow(initialTx, sessions))

            val finalTx = subFlow(FinalityFlow(signedTx, sessions))

            return finalTx.tx.outRefsOfType<CakeRequestState>().single()
        }
    }

    @InitiatedBy(Request::class)
    class RequestResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
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
    class Bake(val requestRef: StateAndRef<CakeRequestState>) : FlowLogic<StateAndRef<CakeState>>() {

        @Suspendable
        override fun call(): StateAndRef<CakeState> {

            val state = requestRef.state.data.accept()

            val command = Command(CakeContract.Commands.Bake(), state.participants.map{it.owningKey})

            val txBuilder = TransactionBuilder(serviceHub.notary())
                .addInputState(requestRef)
                .addOutputState(state, CakeContract.ID)
                .addCommand(command)

            val sessions: List<FlowSession> = listOf(initiateFlow(state.customer))

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