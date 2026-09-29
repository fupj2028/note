// 根构建脚本：只声明插件，不应用。
// AGP 9 起 Kotlin 编译内置，不再需要（也不兼容）org.jetbrains.kotlin.android 插件。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}
