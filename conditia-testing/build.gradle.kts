plugins {
    id("module-java")
    id("packaging-publication")
}

description = "Test helpers for Conditia hosts and condition types: requirements written in code and settings read without a config library"

dependencies {
    api(projects.conditiaApi)

    implementation(libs.snakeyaml)
}
