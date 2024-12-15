group = "fr.iban.guilds"
version = "1.0.1"
description = "MSGuilds"
java.sourceCompatibility = JavaVersion.VERSION_21

plugins {
    `java-library`
    `maven-publish`
    id("io.github.goooler.shadow") version "8.1.7"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://repo.extendedclip.com/releases/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    compileOnly(libs.io.papermc.paper.paper.api)
    compileOnly(libs.com.github.ibanetchep.servercore.core.paper)
    compileOnly(libs.com.github.milkbowl.vaultapi)
    compileOnly(libs.me.clip.placeholderapi)
    compileOnly(libs.com.github.maxlego08.zmenu.api)

    implementation(libs.dev.dejvokep.boosted.yaml)
    implementation(libs.folialib)
}

tasks.shadowJar {
    archiveClassifier.set("")

    relocate("dev.dejvokep.boostedyaml", "fr.iban.guilds.libs.boostedyaml")
    relocate("com.tcoded.folialib", "fr.iban.guilds.libs.folialib")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(
            "project_version" to project.version
        )
    }
}


tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}
