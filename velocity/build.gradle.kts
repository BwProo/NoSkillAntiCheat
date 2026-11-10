// NoSkillAntiCheat Velocity add-on: a small, separate proxy plugin (it is NOT installed on backend
// servers). It hides the proxy software: rewrites the brand shown in F3 and the server-list version.
plugins {
    java
    id("com.gradleup.shadow")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    maven("https://repo.codemc.io/repository/maven-releases/") { name = "codemc" }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.3.1")
    annotationProcessor("com.velocitypowered:velocity-api:4.3.1")
    compileOnly("org.slf4j:slf4j-api:2.0.17") // provided by Velocity
    implementation("com.github.retrooper:packetevents-velocity:2.14.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveFileName = "noskillanticheat-velocity-${rootProject.version}.jar"
    relocate("com.github.retrooper.packetevents", "net.noskillanticheat.velocity.shaded.packetevents")
    relocate("io.github.retrooper.packetevents", "net.noskillanticheat.velocity.shaded.packetevents-impl")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}
