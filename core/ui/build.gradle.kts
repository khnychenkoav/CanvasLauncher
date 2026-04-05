plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    jacoco
}

android {
    namespace = "com.darksok.canvaslauncher.core.ui"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:model"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation("org.robolectric:robolectric:4.12.2")
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

val jacocoExcludes = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    "**/*\$Companion*.*",
    "**/*\$Lambda$*.*",
    "**/*\$inlined$*.*",
    "**/*Hilt*.*",
    "**/*_Factory*.*",
    "**/*_MembersInjector*.*",
    "**/*_HiltModules*.*",
    "**/*_GeneratedInjector*.*",
    "**/hilt_aggregated_deps/**",
    "**/*ComposableSingletons*.*",
    "**/*Kt$*.*",
)
tasks.matching { it.name == "testDebugUnitTest" }.configureEach {
    finalizedBy("jacocoTestReport")
}

tasks.withType<Test>().configureEach {
    extensions.configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest", ":app:connectedDebugAndroidTest")
    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.required.set(true)
    }

    val javaClasses = fileTree("${layout.buildDirectory.asFile.get()}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") {
        exclude(jacocoExcludes)
    }
    val kotlinClasses = fileTree("${layout.buildDirectory.asFile.get()}/tmp/kotlin-classes/debug") {
        exclude(jacocoExcludes)
    }

    classDirectories.setFrom(files(javaClasses, kotlinClasses))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        files(
            fileTree(layout.buildDirectory.asFile.get()) {
                include(
                    "jacoco/testDebugUnitTest.exec",
                    "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                    "outputs/unit_test_code_coverage/debugUnitTest/*.exec",
                    "outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
                    "outputs/code_coverage/debugAndroidTest/connected/*.ec",
                )
            },
            fileTree("${rootProject.projectDir}/app/build") {
                include(
                    "outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
                    "outputs/code_coverage/debugAndroidTest/connected/*.ec",
                )
            },
        ),
    )
}

