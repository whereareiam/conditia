plugins {
    id("module-java")
    id("packaging-publication")
}

description = "Gives each Chassis extension a Conditia scope that closes when the extension stops"

dependencies {
    api(projects.conditiaApi)

    compileOnly(libs.chassis.api)

    testImplementation(projects.conditiaCommon)
    testImplementation(projects.conditiaTesting)
    testImplementation(libs.chassis.api)
}
