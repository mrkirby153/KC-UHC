import xyz.jpenilla.runtask.task.AbstractRun

plugins {
    kotlin("jvm") version "2.2.10"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.mrkirby153"
version = "3.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven {
        url = uri("https://repo.mrkirby153.com/public")
    }
    maven {
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("me.mrkirby153:KirbyUtils-Bukkit:7.0-SNAPSHOT")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation(kotlin("reflect"))
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.8")
        downloadPlugins {
            modrinth("luckperms", "v5.5.0-bukkit")
        }
    }
    shadowJar {
        archiveClassifier = ""
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
    compilerOptions {
        freeCompilerArgs = listOf("-java-parameters")
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.withType<AbstractRun> {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}