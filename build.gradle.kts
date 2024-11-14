import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
    kotlin("jvm") version "2.0.21"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()
        instrumentationTools()

        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        name = "Fenix Buddy"
        description = """
            Helps developers work efficiently with <a href="https://blinkfox.github.io/fenix/">fenix</a>.

        <h2>Fenix Buddy delivers:</h2>
        <ul>
            <li>java/kotlin @QueryFenix annotation jumps to xml node</li>
            <li>xml node jumps to Java/kotlin @QueryFenix annotation</li>
        </ul>
        """.trimIndent()
        changeNotes = """
            <h2>新的:</h2>
        <ul>
            <li>support k2 mode</li>
        </ul>
        """.trimIndent()
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }
    signing {
        certificateChainFile = file("/Users/hansai/Documents/fenix-buddy/chain.crt")
        privateKeyFile = file("/Users/hansai/Documents/fenix-buddy/private.pem")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks.named<RunIdeTask>("runIde") {
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Didea.kotlin.plugin.use.k2=true")
    }
}

tasks.test {
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Didea.kotlin.plugin.use.k2=true")
    }
}