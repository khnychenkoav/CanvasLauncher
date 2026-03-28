plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
}


