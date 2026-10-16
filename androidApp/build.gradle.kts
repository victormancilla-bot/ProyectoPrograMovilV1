import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.net.URI
import java.net.HttpURLConnection
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.File

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            resValue("string", "app_name", "app-dev")
            buildConfigField("String", "APP_NAME", "\"app-dev\"")
            buildConfigField("String", "BASE_URL", "\"https://dev.api.calyrsoft.com\"")
        }
        create("prod") {
            dimension = "environment"
            applicationIdSuffix = ".app"
            resValue("string", "app_name", "app-prod")
            buildConfigField("String", "APP_NAME", "\"app-prod\"")
            buildConfigField("String", "BASE_URL", "\"https://prod.api.calyrsoft.com\"")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

abstract class DownloadTranslationsTask : DefaultTask() {
    @get:org.gradle.api.tasks.InputFile
    abstract val localPropertiesFile: org.gradle.api.file.RegularFileProperty

    @get:org.gradle.api.tasks.OutputDirectory
    abstract val resDir: org.gradle.api.file.DirectoryProperty

    init {
        group = "localization"
        description = "Descarga los archivos strings.xml desde Localise.biz y los guarda en las carpetas de recursos correspondientes."
        outputs.upToDateWhen { false }
    }

    @org.gradle.api.tasks.TaskAction
    fun download() {
        val propsFile = localPropertiesFile.get().asFile
        if (!propsFile.exists()) {
            throw GradleException("El archivo local.properties no existe en la raíz del proyecto. Asegúrate de crearlo e incluir 'localize.api.key'.")
        }

        val properties = Properties().apply {
            propsFile.inputStream().use { load(it) }
        }

        val apiKey = properties.getProperty("localize.api.key")
        if (apiKey.isNullOrEmpty()) {
            throw GradleException("No se encontró la propiedad 'localize.api.key' en local.properties.")
        }

        val locales = mapOf(
            "en-US" to "values",
            "es-BO" to "values-es"
        )

        val targetResDir = resDir.get().asFile

        locales.forEach { (locale, folderName) ->
            val urlString = "https://localise.biz/api/export/locale/$locale.xml?format=android&key=$apiKey"
            
            val targetDir = File(targetResDir, folderName)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            
            val targetFile = File(targetDir, "strings.xml")
            println("Descargando traducciones [$locale] desde Localise.biz...")
            
            try {
                val connection = URI(urlString).toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { inputStream ->
                        Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                    println("✅ Guardado exitosamente: ${targetFile.absolutePath}")
                } else {
                    println("⚠️ Advertencia: No se pudo descargar '$locale' (Código HTTP: ${connection.responseCode}). Es posible que el idioma aún no esté configurado en tu proyecto de Loco.")
                }
            } catch (e: Exception) {
                println("⚠️ Error al descargar el idioma '$locale': ${e.message}")
            }
        }
        println("🎉 Proceso finalizado.")
    }
}

tasks.register<DownloadTranslationsTask>("downloadTranslations") {
    localPropertiesFile.set(project.rootProject.layout.projectDirectory.file("local.properties"))
    resDir.set(project.layout.projectDirectory.dir("src/main/res"))
}