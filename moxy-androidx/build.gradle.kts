plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.github.moxy_community.moxy.androidx"
    compileSdk = 34

    defaultConfig {
        minSdk = 14

        consumerProguardFiles("../moxy/src/main/resources/META-INF/proguard/moxy.pro")
    }

    buildFeatures {
        buildConfig = false
    }
}

dependencies {
    api(project(":moxy"))

    compileOnly(Deps.android)
    compileOnly(project(":stub-androidx"))
}
