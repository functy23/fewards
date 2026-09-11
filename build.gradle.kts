plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.compose.compiler) apply false
}

extra["androidMinSdkVersion"] = 31
extra["androidTargetSdkVersion"] = 37
extra["androidCompileSdkVersion"] = 37
extra["androidBuildToolsVersion"] = "37.0.0"
extra["androidSourceCompatibility"] = JavaVersion.VERSION_21
extra["androidTargetCompatibility"] = JavaVersion.VERSION_21
extra["fewardsVersionCode"] = 22
extra["fewardsVersionName"] = "1.3.12"
