plugins {
    // 26.x needs the fully-qualified plugin id (the short "fabric-loom" alias is the old remapping plugin)
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
}

base {
    archivesName.set(property("archives_base_name") as String)
}

version = property("mod_version") as String
group = property("maven_group") as String

repositories {
    mavenCentral()
    maven("https://maven.meteordev.org/releases")
    maven("https://maven.meteordev.org/snapshots")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")

    // Unobfuscated Minecraft: plain `implementation`, no `mappings`, no `modImplementation`
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fapi_version")}")
    implementation("meteordevelopment:meteor-client:${property("meteor_version")}-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("fabric.mod.json") {
            expand(props)
        }
    }
}
