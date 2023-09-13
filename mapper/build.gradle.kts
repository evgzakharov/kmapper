plugins {
    `maven-publish`
    signing
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:${libs.versions.ksp.get()}")

    implementation("com.squareup:kotlinpoet:${libs.versions.kotlinpoet.get()}")
    implementation("com.squareup:kotlinpoet-ksp:${libs.versions.kotlinpoet.get()}")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "io.github.evgzakharov"
            artifactId = "kmapper"

            from(components["java"])
            pom {
                packaging = "jar"
                name.set("Kotlin mapper for objects")
                url.set("https://github.com/evgzakharov/kmapper")
                description.set("Kotlin mapper for objects")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    connection.set("scm:https://github.com/evgzakharov/kmapper.git")
                    developerConnection.set("scm:git@github.com:evgzakharov/kmapper.git")
                    url.set("https://github.com/evgzakharov/kmapper")
                }

                developers {
                    developer {
                        id.set("evgzakharov")
                        name.set("Evgeniy Zakharov")
                        email.set("evgzakharov88@gmail.com")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl

            credentials {
                username = project.properties["ossrhUsername"].toString()
                password = project.properties["ossrhPassword"].toString()
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}