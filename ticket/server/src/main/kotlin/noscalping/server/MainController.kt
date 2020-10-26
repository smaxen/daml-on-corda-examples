package noscalping.server

import noscalping.api.ticket.Distribution
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

/**
 *  A Spring Boot Server API controller for interacting with the node via RPC.
 */

@Suppress("FoldInitializerAndIfToElvis", "unused")
@RestController
@RequestMapping("/api/noScalping/") // The paths for GET and POST requests are relative to this base path.
class MainController(private val client: ClientConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val myLegalName = client.party()

    /**
     * Returns the distributing party.
     */
    @GetMapping(value = [ "me" ], produces = [ APPLICATION_JSON_VALUE ])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all the legal names of all nodes in this Corda network.
     */
    @GetMapping(value = [ "peers" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getPeers(): Map<String, Set<String>> {
        return mapOf("peers" to client.getPeers())
    }

    /**
     * Displays all distribution states that exist in the node's vault.
     */
    @GetMapping(value = [ "distributions" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getDistributions() : ResponseEntity<List<Distribution>> {
        return ResponseEntity.ok(client.getDistributions())
    }

    /**
     * Initiates a flow to agree a distribution between two distributors.
     *
     * Once the flow finishes, it will have written the distribution to ledger. Both nodes will be able to
     * see it when calling /api/noScalping/distributions on their respective nodes.
     *
     * This endpoint takes a distributor name parameter as part of the path.
     */

    @PostMapping(value = [ "create-distribution" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ "Content-Type=application/x-www-form-urlencoded" ])
    fun createDistribution(request: HttpServletRequest): ResponseEntity<String> {
        val eventName = request.getParameter("eventName").toString()
        val ticketQuantity = request.getParameter("ticketQuantity").toLong()
        val distributorName = request.getParameter("distributorName")
        if(distributorName == null) {
            return ResponseEntity.badRequest().body("Query parameter 'distributorName' must not be null.\n")
        }
        if (ticketQuantity <= 0 ) {
            return ResponseEntity.badRequest().body("Query parameter 'ticketQuantity' must be non-negative.\n")
        }
        if (eventName.isBlank()) {
            return ResponseEntity.badRequest().body("Query parameter 'eventName' must not be blank.\n")
        }
        val tx = client.createDistribution(eventName, ticketQuantity, distributorName)
        return ResponseEntity.status(HttpStatus.CREATED).body("Distribution id ${tx.transactionId} committed to ledger.\n")
    }

}
