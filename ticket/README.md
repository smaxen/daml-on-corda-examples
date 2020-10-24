
# No Ticket Scalping DAML Application

This is a port of Chainstack's No Ticket Scalping CorDapp to DAML.  

To find out more about the cordapp see:
https://docs.chainstack.com/tutorials/corda/no-ticket-scalping-cordapp


## Building

Follow these steps to build the application

### Prerequisites
- Version 1.5 the DAML SDK installed (includes 'daml' utility)
- The Gradle build tool

### Building the DAR file

    daml build

### Code generate the java bindings

    daml codegen java -o server/src/main/java .daml/dist/ticket-0.0.1.dar=noscalping.api

### Compile Server

    (cd server;./gradlew build)

## Start Ledger 

The DAML ledger can be run locally using the DAML sandbox or on a DAML-on-Corda
runtime provisioned by Chainstack.

### Running Locally

In a new terminal start the DAML sandbox:

    daml sandbox .daml/dist/ticket-0.0.1.dar
    
By default, this will start a sandbox ledger running with: ledgerHost=localhost, ledgerPort=6865   
    
The sandbox users 'Alice' and 'Bob' can be used as distributors.

Next [Start Web Application](#start-web-application)

### Running on Chainstack

Provision a DAML for Corda Chainstack node.  Once provisioned you will be provided with the ledgerHost, ledgerPort 
and ledgerToken which should be used in the commands below.

When created the DAML ledger will have no users or packages, so these need to be created added using the daml ledger 
utility.  To see what commands are available get help use:

    daml ledger --help

Load the DAR file created above:

    daml ledger upload-dar --host <ledgerHost> --port <ledgerPort> .daml/dist/ticket-0.0.1.dar
    
Create a local uses alice and bob:
        
    daml ledger allocate-parties --host <ledgerHost> --port <ledgerPort> Alice
    
    daml ledger allocate-parties --host <ledgerHost> --port <ledgerPort> Bob

Alice's name will be globally unique (e.g. D:Alice:OU:RG-4-1:O:RG-4:L:London:S:England:C:GB)

Next [Start Web Application](#start-web-application)

## Start Web Application 

### Configure the server

This step will generate the config files required for the navigator (ui.conf) and the server (server.conf)

    (cd server; ./gradlew setConfig --ledgerHost <ledgerHost> --ledgerPort <ledgerPort> --serverPort 7070 --distributor <alice>)    

### Creating a contract using DAML Navigator

In a new terminal start the DAML navigator

    daml navigator server <ledgerHost> <ledgerPort> -c server/ui.conf

This will by default run the navigator on http://localhost:4000

The first contract must be created via the DAML Navigator this can be done by selecting 
the Ticket:Distribution template and submitting the distribution details with Alice as the
fromDistributor and Bob as the toDistributor.

The created contract should appear under contracts.  

Parties running on other corda nodes can be used as the toDistributor.  Only parties for which
there is at least on distribution contract will appear in the application web service (below) 

### Running the server

In a new terminal session run the server:

    (cd server;./gradlew runServer)
    
Above we configured the server application to run on port 7070 so it will available on http://localhost:7070

