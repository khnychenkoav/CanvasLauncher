plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    jacoco
}

android {
    namespace = "com.darksok.canvaslauncher.feature.apps"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:packages"))
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)

    ksp(libs.hilt.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
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

