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
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
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
    ksp("io.github.evgzakharov:kmapper:0.0.2")
    implementation("io.github.evgzakharov:kmapper:0.0.2")
}
```

### Examples 

```kotlin
data class SrcDto(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val surname: String,
    val age2: Int,
    val dto: SrcDto2 = SrcDto2("test dto"),
    val array: MutableList<String> = mutableListOf("1", "2", "3"),
    val wrapper: String = "5",
    val wrapper2: SrcWrapper = SrcWrapper("5"),
    val enum: SrcEnum = SrcEnum.B
)

enum class SrcEnum {
    A, B, C
}

data class SrcWrapper(
    val value: String
)

data class SrcDto2(
    val source: String,
    val source2: String = "src"
)

data class SrcSourceDto(
    val name2: String,
    val surname: String,
    val enum: SrcEnum = SrcEnum.B,
    val default2: String = "5",
    val dto: SrcDto2 = SrcDto2("test dto"),
)

data class DstWrapper(
    val value: Int
)

data class DstDto(
    var id: UUID,
    val age2: String = "3", // `age` from source have Int type + changing name
    var name2: String,
    var surname: String,
    val default: String = "default", // used to check default overriding
    var default2: Int = 3,  // default will be used
    var dto: DstDto2, // subclass 
    val array: List<Int> = listOf(3, 2, 1), // collecting mapping both with type conversion String -> Int
    val wrapper: DstWrapper = DstWrapper(10), // wrapper for src type
    val wrapper2: Int = 10, // unwrapper for src type
    var enum: DstEnum = DstEnum.A // enum case
)

enum class DstEnum {
    A, B, C
}

data class DstDto2(
    val source: String,
    val source2: String = "dst"
)

@MapperConverter
interface Src2ToDst2Converter: Converter<SrcDto2, DstDto2>

@MapperConverter(dynamicFields = ["name2", "surname", "default"])
interface SrcToDstConverter: Converter<SrcDto, DstDto>

fun SrcDto.toDst(): DstDto = SrcToDstConverterImpl.convert(this) {
    DstDto::name2 from SrcDto::name
    DstDto::surname from { this@toDst.surname + "333555" }
    DstDto::default from { "override default" }
}

@MapperSetter(ignoreFields = ["name2"])
interface SrcToDstSetter: Setter<SrcSourceDto, DstDto>

fun DstDto.setFrom(from: SrcSourceDto): DstDto = SrcToDstSetterImpl.setFrom(from, this)
```
