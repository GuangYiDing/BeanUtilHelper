import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
}

//group = "com.xiaodingsiren"
//version = "1.0.6-RELEASE"

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("cn.hutool:hutool-all:5.8.25")
    compileOnly("org.projectlombok:lombok:1.18.22")
    implementation("commons-beanutils:commons-beanutils:1.9.4")
    implementation("org.springframework:spring-beans:6.1.5")
    annotationProcessor("org.projectlombok:lombok:1.18.22")

    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
//        local("/Applications/IntelliJ\\ IDEA.app")

        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        testFramework(TestFrameworkType.Platform)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {

    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")


        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
//            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }

}

intellijPlatformTesting {
    runIde {
//        register("runIde") {
//            task {
//                jvmArgumentProviders += CommandLineArgumentProvider {
//                    listOf(
//                        "--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
//                        "--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED",
//                        "-javaagent:/Applications/mac2022-2023/ja-netfilter.jar=jetbrains"
//                    )
//                }
//            }
//
//            plugins {
//                robotServerPlugin()
//            }
//        }

        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                        "--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
                        "--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED",
                        "-javaagent:/Applications/mac2022-2023/ja-netfilter.jar=jetbrains"
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}
