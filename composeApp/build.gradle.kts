import org.jetbrains.compose.desktop.application.dsl.TargetFormat

version = "1.0.0"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()
    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
            implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
            implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
            implementation("com.formdev:flatlaf:3.2")
            implementation(compose.materialIconsExtended)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.crawkatt.mrpackinstaller.MainKt"
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe, TargetFormat.Rpm)
            packageName = "MrpackInstaller"
            packageVersion = "1.0.0"

            description = "Instalador de modpacks .mrpack para Minecraft"
            vendor = "CrawKatt"

            linux {
                debPackageVersion = "1.0.0"
                debMaintainer = "CrawKatt"
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
            windows {
                menu = true
                menuGroup = "CrawKatt"
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
                upgradeUuid = "d39cb164-6294-4440-86e2-bb5736bfad3d"
            }
        }
    }
}