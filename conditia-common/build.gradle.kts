plugins {
    id("module-java")
    id("packaging-publication")
}

description = "Conditia's engine: registrations, binding with startup checks, evaluation, any, not, participant blocks, named lists and the types time, date and chance"

dependencies {
    api(projects.conditiaApi)

    testImplementation(projects.conditiaTesting)
}
