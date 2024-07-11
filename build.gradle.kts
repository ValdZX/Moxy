plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" apply false
    id("com.android.application") version "8.5.1" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.vanniktech.maven.publish") version "0.29.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.23" apply false
}

subprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://kotlin.bintray.com/kotlinx")
            content {
                includeGroup("org.jetbrains.kotlinx")
            }
        }

        val repo = maven {
            url = rootProject.file("build/localMavenPublish").toURI()
            content {
                includeGroup("com.github.moxy-community")
            }
        }
        // The only way to put the repository to the top using public api
        remove(repo)
        addFirst(repo)
    }
}

subprojects {
    apply(plugin = "checkstyle")

    tasks.register<Checkstyle>("checkstyle") {
        description = "Runs Checkstyle inspection"
        group = "moxy"
        configFile = rootProject.file("checkstyle.xml")
        ignoreFailures = false
        isShowViolations = true
        classpath = files()
        exclude("**/*.kt")
        source("src/main/java")
    }

    // check code style after project evaluation
    afterEvaluate {
        tasks.named("check").configure { dependsOn("checkstyle") }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates").configure {
    rejectVersionIf { isNonStable(candidate.version) }
}