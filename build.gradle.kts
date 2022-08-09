import io.reflekt.plugin.reflekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("com.github.johnrengelman.shadow") version "5.2.0"
    kotlin("jvm") version "1.5.30"
    id("io.reflekt") version "1.5.30"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://packages.jetbrains.team/maven/p/reflekt/reflekt")
    maven(url = "https://hub.spigotmc.org/nexus/content/groups/public/")
    maven(url = "https://repo.mrkirby153.com/repository/maven-public/")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    maven(url = "https://repo.dmulloy2.net/content/groups/public/")
    maven(url = "https://repo.aikar.co/nexus/content/groups/aikar/")
    maven(url = "https://repo.maven.apache.org/maven2")
    maven(url = "https://m2.dv8tion.net/releases")
    maven(url = "https://repo.opencollab.dev/maven-snapshots/")
}

dependencies {
    implementation("org.json:json:20220320")
    implementation("me.mrkirby153:KirbyUtils-Bukkit:3.5-SNAPSHOT")
    implementation("co.aikar:acf-bukkit:0.5.0-SNAPSHOT")
    implementation("net.dv8tion:JDA:4.3.0_277")
    implementation("com.mrkirby153:bot-core:3.1-SNAPSHOT")

    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.19.2-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib-API:4.4.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.reflekt:reflekt-dsl:1.5.30")
}

group = "me.mrkirby153"
version = "3.0-SNAPSHOT"
description = "KC UHC"
java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

tasks {
    shadowJar {
        relocate("co.aikar.commands", "com.mrkirby153.kcuhc.thirdparty.acf")
        relocate("co.aikar.locales", "com.mrkirby153.kcuhc.thirdparty.acflocales")
        relocate("me.mrkirby153.kcutils", "com.mrkirby153.kcuhc.thirdparty.kirbyutils")
        dependencies {
            include(dependency("co.aikar:acf-bukkit"))
            include(dependency("me.mrkirby153:KirbyUtils-Bukkit"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
    register<Copy>("deployTestServer") {
        dependsOn(build)
        from(shadowJar)
        into("testServer/plugins")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-java-parameters")
        jvmTarget = "11"
        incremental = false
    }
}

reflekt {
    enabled = true
}