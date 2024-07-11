plugins {
    id("kotlin")
    id("com.vanniktech.maven.publish")
}

dependencies {
    api(project(":moxy"))

    compileOnly(Deps.coroutines)

    testImplementation(Deps.junit)
    testImplementation(Deps.coroutines)
    testImplementation(Deps.coroutinesTest)
}
