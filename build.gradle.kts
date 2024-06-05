import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	alias(libs.plugins.kotlin)
}

subprojects {
	apply(plugin = rootProject.libs.plugins.kotlin.get().pluginId)

	group = "co.github.evgzakharov"
	version = "0.0.2"

	java.sourceCompatibility = JavaVersion.VERSION_17

	repositories {
		mavenCentral()
	}

	dependencies {
		implementation("org.jetbrains.kotlin:kotlin-reflect")
		implementation("org.jetbrains.kotlin:kotlin-stdlib")

		implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.libs.versions.kotlinCoroutines.get()}")
	}

	tasks.withType<KotlinCompile> {
		compilerOptions {
			freeCompilerArgs = listOf("-Xjsr305=strict")
			jvmTarget = JvmTarget.JVM_17
		}
	}

	tasks.withType<Test> {
		useJUnitPlatform()
	}
}