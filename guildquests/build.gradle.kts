plugins {
    java
}

val artisanApiVersion: String by project
val msquestsVersion: String by project

repositories {
    mavenCentral()
    // MSQuests is consumed from the local Maven repo: see gradle.properties.
    mavenLocal()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.artisanmc.net/releases") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // Provided at runtime by the MSQuests and ArtisanGuilds plugins, never shaded.
    compileOnly("com.github.ibanetchep.msquests:core:$msquestsVersion")
    // Its own runtime dependencies (database module, FoliaLib) are not needed here.
    compileOnly("com.github.ibanetchep.msquests:bukkit:$msquestsVersion") { isTransitive = false }
    compileOnly(project(":artisan-guilds-plugin"))

    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("com.github.ibanetchep.msquests:core:$msquestsVersion")
    testImplementation("com.github.ibanetchep.msquests:bukkit:$msquestsVersion") { isTransitive = false }
    testImplementation("net.artisanmc:artisan-core-api:$artisanApiVersion")
    testImplementation(project(":artisan-guilds-plugin"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.jar {
    archiveFileName.set("ArtisanGuildQuests-${project.version}.jar")
}
