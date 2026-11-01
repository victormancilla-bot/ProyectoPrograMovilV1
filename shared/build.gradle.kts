import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.wire)
    alias(libs.plugins.detekt)
}

// Secrets.kt se genera desde local.properties (nunca se sube a git).
// Para agregar una clave nueva: añádela aquí y en local.properties.
val secretKeys = listOf("MAPS_API_KEY", "SENTRY_DSN", "VISION_API_KEY", "GRPC_LOGS_URL")
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val generateSecrets = tasks.register("generateSecrets") {
    val values = secretKeys.associateWith { localProperties.getProperty(it, "") }
    val outputDir = layout.buildDirectory.dir("generated/secrets/kotlin")
    inputs.properties(values)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("bo/edu/ucb/encuentra/core/config/Secrets.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            buildString {
                appendLine("// Archivo generado por :shared:generateSecrets. No editar.")
                appendLine("package bo.edu.ucb.encuentra.core.config")
                appendLine()
                appendLine("object Secrets {")
                values.forEach { (key, value) ->
                    val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
                    appendLine("    const val $key: String = \"$escaped\"")
                }
                appendLine("}")
            }
        )
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
       namespace = "bo.edu.ucb.encuentra.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.work.runtime)
        }
        commonMain {
            kotlin.srcDir(generateSecrets)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.bundled)

            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.storage)
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.config)

            implementation(libs.ucb.designsystem)

            implementation(libs.wire.runtime)
            implementation(libs.wire.grpc.client)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)

    // KSP de Room 3: uno por cada target del proyecto
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

// Los .proto van en src/commonMain/proto (G01: logs remotos por gRPC)
wire {
    kotlin {
        rpcRole = "client"
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom(
        "src/commonMain/kotlin",
        "src/androidMain/kotlin",
        "src/iosMain/kotlin",
        "src/commonTest/kotlin"
    )
    // Solo reporta, no rompe el build. Quitar cuando el reporte esté limpio (F10).
    ignoreFailures = true
}
