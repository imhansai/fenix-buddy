import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(25)
}

dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        intellijIdea("2026.2")
        testFramework(TestFrameworkType.Platform)

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("version")
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
            <li>支持 2026.2.x 版本</li>
        </ul>
        """.trimIndent()
        ideaVersion {
            sinceBuild = providers.gradleProperty("sinceBuild")
            untilBuild = providers.gradleProperty("untilBuild")
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
