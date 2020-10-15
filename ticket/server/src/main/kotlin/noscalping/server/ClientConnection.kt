package noscalping.server

import com.daml.ledger.javaapi.data.*
import com.daml.ledger.rxjava.DamlLedgerClient
import noscalping.api.ticket.Distribution
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import java.util.UUID
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private const val RPC_HOST = "config.rpc.host"
private const val RPC_PORT = "config.rpc.port"
private const val RPC_TOKEN = "config.rpc.token"
private const val DISTRIBUTOR = "config.distributor"

/**
 * Wraps a node RPC proxy.
 *
 * The RPC proxy is configured based on the properties in `application.properties`.
 * You can leave `applications.properties` blank and put connection details in `clients/build.gradle`
 * under the `runDistributor` task.
 *
 * @property host The host of the ledger node we are connecting to.
 * @property port The port of the ledger node we are connecting to.
 * @property token Token to uses for ledger authentication (optional).
 * @property distributor The distributing DAML party.
 */
@Suppress("unused")
@Component
open class ClientConnection(
  @Value("\${$RPC_HOST}") private val host: String,
  @Value("\${$RPC_PORT}") private val port: Int,
  @Value("\${$DISTRIBUTOR}") private val distributor: String,
  @Value("\${$RPC_TOKEN:#{null}}") private val token: String?
) {

    private val logger: Logger = LoggerFactory.getLogger(ClientConnection::class.java)

    private lateinit var client: DamlLedgerClient

    @PostConstruct
    fun initialiseClient() {
        client = DamlLedgerClient.newBuilder(host, port).build()!!
        client.connect()
    }

    fun party(): String = distributor

    fun getPeers(): List<String> = getDistributions().flatMap{listOf(it.fromDistributor, it.toDistributor)}.filterNot { it == distributor }

    fun getDistributions(): List<Distribution> {
        val flow = client.activeContractSetClient.getActiveContracts(filterFor(Distribution.TEMPLATE_ID), false)!!
        val responses = flow.blockingIterable(1000)!!
        return responses.flatMap { it.createdEvents }.map { Distribution.fromValue(it.arguments) }
    }

    fun createDistribution(eventName: String, ticketQuantity: Long, toDistributor: String): Transaction  {
        val distribution = Distribution(distributor, toDistributor, eventName, ticketQuantity)
        return submit(distribution.create())
    }

    private fun submit(command: Command): Transaction {
        return client.commandClient.submitAndWaitForTransaction(
          UUID.randomUUID().toString(),
          "NoScalpingApp",
          UUID.randomUUID().toString(),
          distributor,
          Optional.empty(),
          Optional.empty(),
          Optional.empty(),
          Collections.singletonList(command)
        ).blockingGet()
    }

    @Suppress("SameParameterValue")
    private fun filterFor(templateId: Identifier): TransactionFilter {
        val inclusiveFilter = InclusiveFilter(Collections.singleton(templateId))
        val filter: Map<String, Filter> = Collections.singletonMap(distributor, inclusiveFilter)
        return FiltersByParty(filter)
    }

    @PreDestroy
    fun close() {
        client.close()
    }
}
