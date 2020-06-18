package com.article.flows

import com.article.states.CakeType
import net.corda.core.flows.FlowLogic
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Duration

class CakeFlowTests {

    private val nodeCordapps = MockNetworkParameters(
        cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.article.contracts"),
            TestCordapp.findCordapp("com.article.flows")
        )
    )
    private val parameters = nodeCordapps.withNetworkParameters(nodeCordapps.networkParameters.copy(minimumPlatformVersion = 6))

    private val network = MockNetwork(parameters)

    private val baker = network.createNode()
    private val customer = network.createNode()

    init {
        baker.registerInitiatedFlow(CakeFlow.RequestResponder::class.java)
        customer.registerInitiatedFlow(CakeFlow.BakeResponder::class.java)
        baker.registerInitiatedFlow(CakeFlow.EatResponder::class.java)
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    private fun <T> runFlow(from: StartedMockNode, logic: FlowLogic<T>): T {
        val future = from.startFlow(logic)
        network.runNetwork()
        return future.getOrThrow(Duration.ofSeconds(60))
    }

    @Test
    fun requestBakeAndEat() {
        val requestRef = runFlow(customer, CakeFlow.Request(CakeType.Chocolate, baker.info.singleIdentity()))
        val cakeRef = runFlow(baker, CakeFlow.Bake(requestRef))
        runFlow(customer, CakeFlow.Eat(cakeRef))
    }

}