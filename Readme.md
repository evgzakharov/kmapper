# KMapper

Kotlin mapper for mapping objects

### Principles of work

* based on ksp
* mapper generate code on annotation processing faze
* generate code based on kotlin poet

### Usage

- add ksp plugin to build.gradle.kts

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.0-1.0.11"
}
``` 

- add generated source sets to project

```kotlin
sourceSets {
    main {
        java {
            srcDir("${buildDir.absolutePath}/generated/ksp/")
        }
    }
}
```

- and finally add dependencies to mapper

```kotlin
dependencies {
    ksp("io.github.evgzakharov:kmapper:0.0.1")
    implementation("io.github.evgzakharov:kmapper:0.0.1")
}
```