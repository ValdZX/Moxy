plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlinx-serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

repositories {
    google()
    mavenCentral()
}

android {
    compileSdk = 34
    namespace = "moxy.sample"

    defaultConfig {
        applicationId = "moxy.sample"
        minSdk = 23
        versionCode = 1
        versionName = "1.0"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments.putAll(mapOf(
                    "disableEmptyStrategyCheck" to "false",
                    "enableEmptyStrategyHelper" to "true",
                    "defaultMoxyStrategy" to "moxy.viewstate.strategy.AddToEndSingleStrategy",
                    "moxyEnableIsolatingProcessing" to "true"
                ))
            }
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = "Moxy"
            keyPassword = "MoxyRelease"
            storePassword = "MoxyRelease"
            storeFile = file("DemoReleaseKeystore")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")

    // AndroidX
    implementation("androidx.appcompat:appcompat:1.7.0")

    // AndroidX KTX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.8.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // HTTP client
    implementation("io.ktor:ktor-client-android:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")

    // Image loader
    implementation("io.coil-kt:coil:2.5.0")

    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")

    // java.time and other stuff without third-party libraries
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Moxy
    // uncomment to test library from local sources
    implementation(project(":moxy-androidx"))
    implementation(project(":moxy-ktx"))
    ksp(project(":moxy-compiler"))

    // uncomment to test library from maven
//    val moxyVersion = "2.2.2"
//    implementation("com.github.moxy-community:moxy-androidx:$moxyVersion")
//    implementation("com.github.moxy-community:moxy-ktx:$moxyVersion")
//    kapt("com.github.moxy-community:moxy-compiler:$moxyVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.2")
}
