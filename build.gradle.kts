import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("fabric-loom") version "1.9.2"
    kotlin("jvm") version "2.2.20"
}

version = "1.0.0"
group = "com.example"

base {
    archivesName.set("cobblemonaddon")
}

repositories {
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.aliyun.com/repository/central")
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.20.1")

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.116.17+1.21.1")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.6+kotlin.2.2.20")

    modImplementation(files("libs/cobblemon-fabric-1.8.1+1.21.1.jar"))

    // Cobblemon 把 GraalVM 打成内嵌 jar（META-INF/jars），Loom 开发环境不会解包，
    // 必须显式声明，否则 showdown 线程启动即崩、客户端卡在启动阶段。
    implementation("org.graalvm.sdk:graal-sdk:22.3.0")
    implementation("org.graalvm.js:js:22.3.0")
    implementation("org.graalvm.truffle:truffle-api:22.3.0")
    implementation("org.graalvm.regex:regex:22.3.0")
    implementation("com.ibm.icu:icu4j:71.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
