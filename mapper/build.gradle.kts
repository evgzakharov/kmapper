dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:${libs.versions.ksp.get()}")

    implementation("com.squareup:kotlinpoet:${libs.versions.kotlinpoet.get()}")
    implementation("com.squareup:kotlinpoet-ksp:${libs.versions.kotlinpoet.get()}")
}