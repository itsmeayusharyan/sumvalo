# Sumvalo release rules.

# Room entities and DAOs (generated code references them by name).
-keep class com.acuranza.app.data.** { *; }
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# WorkManager instantiates workers reflectively.
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }

# Manifest-registered components.
-keep class com.acuranza.app.AcuranzaApp { *; }

# Investment specs are serialised with org.json by field names in code (no reflection), but keep the
# finance and SMS engines' data classes readable in crash traces.
-keepnames class com.acuranza.app.finance.** { *; }
-keepnames class com.acuranza.app.sms.** { *; }

# Coroutines.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.debug.**

# Kotlin metadata for enums used with valueOf().
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn org.slf4j.**
-dontwarn javax.annotation.**

# SQLCipher (JNI and the Room SupportOpenHelperFactory).
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**
