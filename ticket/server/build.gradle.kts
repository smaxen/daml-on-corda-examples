
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

task<JavaExec>("runLocalServer") {
    dependsOn("jar")
    classpath = sourceSets["main"].runtimeClasspath
    main = "noscalping.server.ServerKt"
    args = listOf("--server.port=7070", "--config.rpc.host=localhost", "--config.rpc.port=6865", "--config.distributor=Alice")
}
