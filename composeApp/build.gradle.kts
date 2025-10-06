import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

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
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "MrpackInstaller"
            packageVersion = "1.0.0"

            description = "Instalador de modpacks .mrpack para Minecraft"
            vendor = "CrawKatt"

            linux {
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

tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Crea un JAR ejecutable con todas las dependencias incluidas"

    archiveBaseName.set("MrpackInstaller")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("all")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Main-Class" to "org.crawkatt.mrpackinstaller.MainKt",
            "Implementation-Title" to "Mrpack Installer",
            "Implementation-Version" to "1.0.0"
        )
    }

    from(configurations.getByName("jvmRuntimeClasspath").map {
        if (it.isDirectory) it else zipTree(it)
    })

    from(kotlin.jvm().compilations.getByName("main").output.classesDirs)
    from(kotlin.jvm().compilations.getByName("main").output.resourcesDir)
}

tasks.register("buildJar") {
    group = "build"
    description = "Alias para fatJar"
    dependsOn("fatJar")
}