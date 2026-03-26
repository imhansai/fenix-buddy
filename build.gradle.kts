
import com.github.javaparser.printer.concretesyntaxmodel.CsmElement.token
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.13.1"
    kotlin("jvm") version "2.3.20"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.1")

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()

        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
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
            <li>支持 2026.1.x 版本</li>
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
