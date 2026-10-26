plugins {
    // Applies the correct Loom variant based on the active Minecraft version
    id("dev.kikugie.loom-back-compat")
    id("maven-publish")
}

// DO NOT set group = ... here - see https://codeberg.org/KikuGie/loom-back-compat docs
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("diehardpractice") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    runConfigs.all {
        runDirectory = rootProject.file("run") // Shares the run directory between versions
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    // Use `mod{dependency type}` even on 26.1+ - loom-back-compat converts them
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
}

tasks {
    processResources {
        val props = mapOf(
            "version" to project.version.toString(),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "loader" to Regex("\\d+\\.\\d+").find(sc.properties.get<String>("deps.fabric_loader"))!!.value,
            "java" to requiredJava.majorVersion
        )
        inputs.properties(props)

        filesMatching("fabric.mod.json") { expand(props) }
        filesMatching("*.mixins.json") { expand("java" to requiredJava.majorVersion) }
    }

    named<ProcessResources>("processClientResources") {
        filesMatching("*.mixins.json") { expand("java" to requiredJava.majorVersion) }
    }

    withType<JavaCompile>().configureEach {
        options.release = requiredJava.majorVersion.toInt()
    }

    withType<Jar>().configureEach {
        val modId = project.property("mod.id")
        inputs.property("mod_id", modId)
        from("../../LICENSE") { rename { "${it}_$modId" } }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
