
val serverConf = "server.conf"
val uiConf = "ui.conf"

plugins {
    kotlin("jvm") version "1.3.72"
}

repositories {
    jcenter()
}

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.slf4j:slf4j-api:1.7.26")
    implementation("com.daml:bindings-rxjava:1.5.0")
    implementation("org.springframework.boot:spring-boot-starter-websocket:1.5.7.RELEASE")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")

}

// open class SetConfig @javax.inject.Inject constructor(private val ops: FileSystemOperations): DefaultTask() {
open class SetConfig : DefaultTask() {

    @set:Option(option = "serverPort", description = "Port web service will be served on")
    var serverPort: String? = null

    @set:Option(option = "ledgerHost", description = "The DAML Ledger API Host")
    var ledgerHost: String? = null

    @set:Option(option = "ledgerPort", description = "The DAML Ledger API Port")
    var ledgerPort: String? = null

    @set:Option(option = "distributor", description = "The DAML distributor party the service connects as")
    var distributor: String? = null

    @set:Option(option = "ledgerToken", description = "The DAML Ledger Token")
    var ledgerToken: String? = null

    fun serverArgs(): List<String> {
        val main = listOf(
          "--server.port=$serverPort",
          "--ledger.host=$ledgerHost",
          "--ledger.port=$ledgerPort",
          "--distributor=$distributor"
        )
        return when(ledgerToken) {
            null -> main
            else -> main + "--ledger.token=$ledgerToken"
        }
    }

}

fun writeServerConfig(args: List<String>): Unit {
    File(serverConf).writeText(args.joinToString(System.lineSeparator()))
    logger.info("Wrote $args to $serverConf")
}

fun writeUIConf(distributor: String): Unit {
    File(uiConf).writeText("""users { 
    |   "$distributor" { party = "$distributor" }
    |}""".trimMargin())
    logger.info("Wrote $distributor to $uiConf")
}

fun readServerConfig(): List<String> {
    return if (File(serverConf).exists()) {
        File(serverConf).readLines()
    } else {
        emptyList()
    }
}

tasks.register<SetConfig>("setConfig") {
    doLast {
        writeUIConf(distributor!!)
        writeServerConfig(serverArgs())
    }
}

task<JavaExec>("runServer")
{
    dependsOn("jar")
    classpath = sourceSets["main"].runtimeClasspath
    main = "noscalping.server.ServerKt"
    args = readServerConfig()
}
