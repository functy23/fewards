pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        // miuix 0.9.4（含 PR #423 新增的 miuix-glass）从未发布到 Maven Central：
        // Central 上最新只到 0.9.4-rc01。本机构建并 publishToMavenLocal 后从这里取，
        // 构建步骤见 AGENTS.md「miuix 版本」一节。
        mavenLocal()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "Fewards"
include(":app")
