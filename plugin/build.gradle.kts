plugins {
    java
    `maven-publish`
}

val artisanApiVersion: String by project

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.artisanmc.net/releases") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    // Artisan is required and provides its API at runtime: never shaded.
    compileOnly("net.artisanmc:artisan-core-api:$artisanApiVersion")
    // Optional at runtime (softdepend): the bank needs an economy.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }

    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("net.artisanmc:artisan-core-api:$artisanApiVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:3.5.1")
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
    archiveFileName.set("ArtisanGuilds-${project.version}.jar")
}

// Plugins that integrate with ArtisanGuilds (MSLands, ArtisanGuildQuests) compile against this jar:
//   ./gradlew :artisan-guilds-plugin:publishToMavenLocal
// It is the plugin itself: they use com.github.ibanetchep.artisanguilds.api only.
publishing {
    publications {
        create<MavenPublication>("plugin") {
            artifactId = "artisan-guilds-plugin"
            from(components["java"])
        }
    }
}
