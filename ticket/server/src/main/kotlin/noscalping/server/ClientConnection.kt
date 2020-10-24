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

private const val CFG_HOST = "ledger.host"
private const val CFG_PORT = "ledger.port"
private const val CFG_TOKEN = "ledger.token"
private const val CFG_DISTRIBUTOR = "distributor"

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
  @Value("\${$CFG_HOST}") private val host: String,
  @Value("\${$CFG_PORT}") private val port: Int,
  @Value("\${$CFG_DISTRIBUTOR}") private val distributor: String,
  @Value("\${$CFG_TOKEN:#{null}}") private val token: String?
) {

    private val logger: Logger = LoggerFactory.getLogger(ClientConnection::class.java)

    init {
        logger.warn("${javaClass.simpleName} created with host: $host, port: $port, distributor: $distributor, token: $token")
    }

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
