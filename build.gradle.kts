plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
    kotlin("jvm") version "1.9.23"
}

group = "dev.fromnowon"
version = "1.6"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2024.1")
    type.set("IC") // Target IDE Platform

    plugins.set(
        listOf(
            "com.intellij.java",
            "org.jetbrains.kotlin"
        )
    )
}

tasks {
    // Set the JVM compatibility versions
    compileJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    compileTestJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        // untilBuild.set("241.*")
    }

    signPlugin {
        certificateChainFile.set(file("/Users/hansai/Documents/fenix-buddy/chain.crt"))
        privateKeyFile.set(file("/Users/hansai/Documents/fenix-buddy/private.pem"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
