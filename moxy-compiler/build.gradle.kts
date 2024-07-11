plugins {
    id("java")
    id("kotlin")
    id("kotlin-kapt")
    id("com.vanniktech.maven.publish")
    id("com.google.devtools.ksp") version "2.0.0-1.0.22"
}

dependencies {
    implementation(project(":moxy"))

    implementation(Deps.kotlinStdlibForCompiler)

    implementation(Deps.javapoet)

    implementation(Deps.gradleIncapHelperAnnotations)

    compileOnly(Deps.autocommon)
    compileOnly(Deps.autoservice)

    kapt(Deps.gradleIncapHelperProcessor)
    kapt(Deps.autoservice)

    testImplementation(project.project(":moxy").sourceSets.test.get().output)
    testImplementation(Deps.junit)
    testImplementation(Deps.truth)
    testImplementation(Deps.compiletesting)
    testImplementation(Deps.asm)
    testImplementation(Deps.asmUtil)
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.0-1.0.23")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-common"))
    testImplementation(kotlin("test-annotations-common"))
    implementation("com.squareup:kotlinpoet:1.17.0")
    implementation("com.squareup:kotlinpoet-metadata:1.17.0")
    implementation("com.squareup:kotlinpoet-ksp:1.17.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.6.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.6.0")

    //workaround to use test resources (https://stackoverflow.com/q/24870464)
    testRuntimeOnly(files(sourceSets.test.get().output.resourcesDir))
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}