# Keep sora-editor (used by many apps, default rules are safe)
-keep class io.github.rosemoe.sora.** { *; }
-dontwarn io.github.rosemoe.**

# Markwon 图片模块的可选 SVG/GIF 解码器（未引入对应依赖，仅需忽略缺失类）
-dontwarn com.caverock.androidsvg.**
-dontwarn pl.droidsonroids.gif.**

# Markwon：本地图片异步加载依赖运行时类查找/回调，保持不混淆
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# WebView JS 桥：方法名由 JavaScript 反射调用，不能混淆
-keep class com.marknote.app.editor.VditorBridge { *; }
-keepclassmembers class com.marknote.app.editor.VditorBridge {
    @android.webkit.JavascriptInterface <methods>;
}
