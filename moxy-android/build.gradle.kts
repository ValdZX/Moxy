plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
}

android {
    compileSdk = 34

    defaultConfig {
        minSdk = 14

        proguardFiles(
            file("/moxy/src/main/resources/META-INF/proguard/moxy.pro")
        )
    }
}

dependencies {
    api(project(":moxy"))

    compileOnly(Deps.android)
    compileOnly(project(":stub-android"))
}
