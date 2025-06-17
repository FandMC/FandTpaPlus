import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    kotlin("jvm") version "2.2.0-RC3"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "cn.fandmc.fandtpaplus"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("net.kyori:adventure-api:4.16.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.16.0")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    shadowJar {
        relocate("kotlin", "cn.fandmc.fandtpaplus.lib.kotlin")
        relocate("com.google.gson", "cn.fandmc.fandtpaplus.lib.gson")
        relocate("org.yaml.snakeyaml", "cn.fandmc.fandtpaplus.lib.snakeyaml")

        exclude("META-INF/LICENSE")
        exclude("META-INF/NOTICE")
        exclude("META-INF/*.kotlin_module")

        minimize {
            exclude(dependency("org.jetbrains.kotlin:.*"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
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
