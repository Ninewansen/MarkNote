import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.legacy.kapt)
}

// prism4j 依赖旧版 org.jetbrains:annotations-java5，与 Compose 的 annotations 冲突，统一排除
configurations.all {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

android {
    namespace = "com.marknote.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.marknote.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 16
        versionName = "1.0.5"
    }

    // 签名信息放在根目录 keystore.properties（已加入 .gitignore，不会上传），
    // 公开仓库里没有该文件时 release 构建为未签名 APK。
    val keystoreProperties = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val releaseKeystoreFile = keystoreProperties.getProperty("storeFile")
        ?.let { rootProject.file(it) }
    val hasReleaseKeystore = releaseKeystoreFile != null && releaseKeystoreFile.exists()

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = releaseKeystoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// 输出文件按版本号命名：MarkNote-1.0.1.apk（之后每次升版本自动跟随）
val appExtension = extensions.getByType<com.android.build.api.dsl.ApplicationExtension>()
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName?.set("MarkNote-${appExtension.defaultConfig.versionName}.apk")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.sora.editor)
    implementation(libs.sora.language.textmate)

    implementation(libs.androidx.documentfile)

    // Markwon：native Markdown 渲染（预览）
    implementation(libs.markwon.core)
    implementation(libs.markwon.image)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.ext.tasklist)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.linkify)
    implementation(libs.markwon.syntax.highlight)
    implementation(libs.prism4j) {
        // 与 Compose 依赖的 org.jetbrains:annotations 冲突
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }
    implementation(libs.okhttp)
    // prism4j-bundler 生成代码高亮语法定义（注解处理）
    compileOnly(libs.prism4j.bundler)
    kapt(libs.prism4j.bundler)
}
