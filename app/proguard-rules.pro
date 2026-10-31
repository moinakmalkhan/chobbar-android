# kotlinx.serialization: keep generated serializers for our DTOs.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class shop.chobbar.app.**$$serializer { *; }
-keepclassmembers class shop.chobbar.app.** { *** Companion; }
-keepclasseswithmembers class shop.chobbar.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit: keep service interfaces and generic signatures used by suspend functions.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepclasseswithmembers interface * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Credential Manager / Google ID
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
