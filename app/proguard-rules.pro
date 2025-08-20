###############################################################################
# ProGuard / R8 rules for TopScanner
# - Keeps Apache POI + XMLBeans + OOXML types (required by POI at runtime)
# - Keeps Google ML Kit & Play Services scanning classes
# - Keeps Coil (image loader)
# - Keeps some Compose runtime entries (optional, avoids runtime reflection problems)
#
# Paste this into: app/proguard-rules.pro
###############################################################################

# ---------------------
# Apache POI (XWPF / OOXML) - keep everything under org.apache.poi
# POI uses reflection and many helpers; stripping causes runtime crashes.
# If you want to reduce size later, replace broad keep with more selective rules.
# ---------------------
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

# XMLBeans and OOXML schemas used by POI
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**
-keep class org.openxmlformats.** { *; }
-dontwarn org.openxmlformats.**

# Commons-compress used by POI
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**

# ---------------------
# Google ML Kit / GMS (Document Scanning)
# Keep ML Kit classes used by scanning and GMS wrapper classes.
# ---------------------
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# If you use any com.google.android.gms.* APIs via reflection, keep them too:
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.android.gms.vision.**

# ---------------------
# Coil (image loading)
# Usually not necessary to keep Coil, but keep to be safe if using reflection/plugins.
# ---------------------
-keep class coil.** { *; }
-dontwarn coil.**

# ---------------------
# AndroidX Compose runtime (optional safe-guard)
# Compose normally works fine, but this keeps runtime classes from being removed.
# ---------------------
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**

# Keep Kotlin metadata annotations so reflection-based code still works
-keepclassmembers class kotlin.Metadata { *; }

# ---------------------
# Keep app classes you know are referenced via reflection (example: ViewModels,
# or classes you call from non-Java code). Add more keeps below as needed:
# ---------------------
#-keep class com.sami.topscanner.** { *; }

# ---------------------
# Misc - avoid warnings from libraries you depend on
# ---------------------
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**
-dontwarn org.xmlpull.v1.**
-dontwarn org.joda.time.**

-dontwarn aQute.bnd.annotation.baseline.BaselineIgnore
-dontwarn aQute.bnd.annotation.spi.ServiceConsumer
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn java.awt.Shape
-dontwarn org.osgi.framework.Bundle
-dontwarn org.osgi.framework.BundleContext
-dontwarn org.osgi.framework.FrameworkUtil
-dontwarn org.osgi.framework.ServiceReference
-dontwarn org.osgi.framework.wiring.BundleRevision
###############################################################################
# End of rules
###############################################################################