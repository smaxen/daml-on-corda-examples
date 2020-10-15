
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

## Running 

The application can be run locally using the DAML sandbox and on DAML-on-Corda
runtimes provisioned by Chainstack.

### Running Locally

In a new terminal start the DAML sandbox:

    daml start
    
The DAML navigator should open in a browser session once the sandbox has started.    

In a new terminal session run the server:

    (cd server;./gradlew runLocalServer)
    
By default, this will run the server application on localhost:7070    

Next [create a contract using the navigator](#creating-a-contract-using-daml-navigator)

### Running on Chainstack

TBC

## Creating a contract using DAML Navigator

By default, this will run running on http://localhost:7500

The first contract must be created via the DAML Navigator this can be done by selecting 
the Ticket:Distribution template and submitting the distribution details.

## Using the Webapp

Once the first contract has been created via the navigator the webapp can be used

By default, the server will be running on http://localhost:7070

