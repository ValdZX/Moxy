plugins {
    id("java-library")
    id("com.vanniktech.maven.publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(Deps.android)

    testImplementation(Deps.junit)
    testImplementation(Deps.mockito)
    testImplementation(Deps.truth)
    testImplementation(Deps.compiletesting)
    testImplementation(Deps.javapoet)
    testAnnotationProcessor(project(":moxy-compiler"))
}
