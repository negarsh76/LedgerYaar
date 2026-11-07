rootProject.name = "LedgerYaar"

// سرور Maven گوگل (dl.google.com) از ایران مسدود است؛
// اول از میرورهای ایرانی می‌گیریم و اگر چیزی نبود، سراغ منبع اصلی می‌رویم.
pluginManagement {
    repositories {
        maven("https://maven.myket.ir")
        maven("https://en-mirror.ir")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.myket.ir")
        maven("https://en-mirror.ir")
        google()
        mavenCentral()
    }
}

include(":composeApp")
include(":androidApp")
