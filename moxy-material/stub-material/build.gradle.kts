plugins {
    java
}

dependencies {
    compileOnly(Deps.android)
    compileOnly(project(":stub-androidx"))
}