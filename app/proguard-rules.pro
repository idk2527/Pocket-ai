# Proguard/R8 rules for PocketAI

# 1. Keep JNI Native Methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# 2. Keep LlamaCppService and its methods (accessed by JNI via reflection)
-keep class com.pocketai.app.services.LlamaCppService { *; }

# 3. Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class com.pocketai.app.PocketAIApplication { *; }

# 4. Room Database
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }

# 5. Gson (Used for receipt parsing)
-keep class com.google.gson.** { *; }
-keep class com.pocketai.app.data.model.** { *; }

# 6. llama.cpp specific - do not strip native bindings
-keep class com.pocketai.app.services.** { *; }

# Optimization settings
-repackageclasses ''
-allowaccessmodification
