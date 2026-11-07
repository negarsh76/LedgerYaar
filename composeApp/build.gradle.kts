import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// کد مشترک (Compose Multiplatform) — از AGP 9 به بعد یک «کتابخانه» است
// و خود اپ اندروید در ماژول جدای androidApp ساخته می‌شود.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// ساخت واقعیِ iOS فقط روی macOS با Xcode/CocoaPods ممکن است.
val isMac = false

kotlin {
    androidLibrary {
        namespace = "com.ledgeryaar.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        // لازم برای Compose Resources (لوگو و عکس صفحهٔ لودینگ)
        androidResources {
            enable = true
        }
    }

    if (isMac) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        commonMain.dependencies {
            // api: ماژول androidApp هم به این‌ها مستقیم نیاز دارد
            api(compose.runtime)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.coroutines.core)
            api(libs.koin.core)

            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.compose.icons.core)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.room.runtime)
            implementation(libs.datastore.preferences.core)
            implementation(libs.okio)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.datastore.preferences)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        if (isMac) {
            matching { it.name == "iosMain" }.configureEach {
                dependencies {
                    implementation(libs.sqlite.bundled)
                }
            }
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.ledgeryaar.resources"
    generateResClass = always
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    if (isMac) {
        add("kspIosX64", libs.room.compiler)
        add("kspIosArm64", libs.room.compiler)
        add("kspIosSimulatorArm64", libs.room.compiler)
    }
}
