plugins {
    `java-library`
    id("io.freefair.lombok")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

group = "me.whereareiam.conditia"

// Compilation and tests use Java 21 whichever Java runs Gradle.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    add("compileOnly", libs.findLibrary("annotations").get())

    add("testImplementation", libs.findLibrary("junit-jupiter").get())
    add("testImplementation", libs.findLibrary("annotations").get())

    add("testRuntimeOnly", libs.findLibrary("junit-platform").get())
}

// Constructors keep their parameters' names, so a host reads the settings records of the built-in types through
// their constructor also where they were downgraded to an older Java and are plain classes.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
