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
            srcDir("${layout.buildDirectory.dir("generated/ksp").get()}")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}