plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.13.0"
}

group = "com.xiaodingsiren"
version = "1.0.5-RELEASE"

repositories {
    mavenCentral()
}



dependencies {
    implementation("cn.hutool:hutool-all:5.8.25")
    compileOnly("org.projectlombok:lombok:1.18.22")
    implementation("commons-beanutils:commons-beanutils:1.9.4")
    implementation("org.springframework:spring-beans:6.1.5")
    annotationProcessor("org.projectlombok:lombok:1.18.22")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
//    version.set("2022.3.3")
    type.set("IC") // Target IDE Platform
    type.set("IU") // Target IDE Platform
    localPath.set("/Applications/IntelliJ IDEA.app/Contents")

    plugins.set(listOf("com.intellij.java"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("243.*")
    }

//    compileJava {
//        options.annotationProcessorPath = project.configurations.getByName("annotationProcessor").asFileTree
//    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }


    runIde {
        jvmArgs(
                "--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED",
                "-javaagent:/Applications/mac2022-2023/ja-netfilter.jar=jetbrains")
    }
}
