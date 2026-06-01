# Retrofit a OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface retrofit2.** { *; }
-keep class okhttp3.internal.** { *; }

# Gson
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }

# Zachování datových modelů (velmi důležité pro release build)
-keepclassmembers class xyz.jo6r.sam.sammeal2.** {
    <fields>;
}
-keep class xyz.jo6r.sam.sammeal2.MealData { *; }
-keep class xyz.jo6r.sam.sammeal2.MealResponse { *; }
-keep class xyz.jo6r.sam.sammeal2.MealRequest { *; }
-keep class xyz.jo6r.sam.sammeal2.UIState { *; }

# Zachování MealApi interface (Retrofit ho potřebuje pro dynamickou proxy)
-keep interface xyz.jo6r.sam.sammeal2.MealApi { *; }

# CameraX a ML Kit
-keep class androidx.camera.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn androidx.camera.**
-dontwarn com.google.mlkit.**
