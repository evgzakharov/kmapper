plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(project(":mapper"))
    implementation(project(":mapper"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:${libs.versions.junit.get()}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${libs.versions.junit.get()}")
}

sourceSets {
    main {
        java {
            srcDir("${buildDir.absolutePath}/generated/ksp/")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}