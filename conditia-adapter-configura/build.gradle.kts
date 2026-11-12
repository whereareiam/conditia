plugins {
    id("module-java")
    id("packaging-publication")
}

description = "Reads Conditia's requirements and condition settings through Configura"

dependencies {
    api(projects.conditiaApi)

    compileOnly(libs.configura.api)
    compileOnly(libs.jackson.databind)

    testImplementation(projects.conditiaCommon)
    testImplementation(projects.conditiaTesting)
    testImplementation(libs.configura)
}
