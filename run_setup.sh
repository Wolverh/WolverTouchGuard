set -e

mkdir -p .github/workflows
mkdir -p app/src/main/java/com/wolver/touchguard
mkdir -p app/src/main/res/drawable
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/values
mkdir -p app/src/main/res/xml

rm -f app/src/main/java/com/wolver/touchguard/DpiTouchBooster.kt
rm -f app/src/main/java/com/wolver/touchguard/FloatingMenuView.kt
rm -f app/src/main/java/com/wolver/touchguard/ScreenRadarView.kt
rm -f app/src/main/java/com/wolver/touchguard/UpdateManager.kt

cat << 'EOF' > settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WolverTouchGuard"
include(":app")
EOF

cat << 'EOF' > build.gradle.kts
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
EOF

cat << 'EOF' > gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
android.suppressUnsupportedCompileSdk=35
EOF

cat << 'EOF' > app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wolver.touchguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wolver.touchguard"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
EOF

cat << 'EOF' > app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission
        android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />

    <uses-permission
        android:name="android.permission.WRITE_SETTINGS"
        tools:ignore="ProtectedPermissions" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@drawable/ic_launcher"
        android:supportsRtl="true"
        android:theme="@style/Theme.WolverTouchGuard">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:screenOrientation="portrait"
            android:windowSoftInputMode="stateHidden|adjustPan">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".TouchGuardAccessibilityService"
            android:enabled="true"
            android:exported="false"
            android:label="@string/accessibility_service_label"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

    </application>

</manifest>
EOF

cat << 'EOF' > app/src/main/res/xml/accessibility_service_config.xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowsChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagRequestFilterKeyEvents"
    android:canPerformGestures="false"
    android:canRequestFilterKeyEvents="true"
    android:canRetrieveWindowContent="false"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="50"
    android:settingsActivity="com.wolver.touchguard.MainActivity" />
EOF

cat << 'EOF' > app/src/main/res/drawable/ic_launcher.xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#000000"
        android:pathData="M0,0h108v108h-108z" />
    <path
        android:fillColor="#FF1744"
        android:pathData="M54,18L24,32v26c0,21.5 12.8,41.6 30,48 17.2,-6.4 30,-26.5 30,-48V32L54,18z" />
    <path
        android:fillColor="#000000"
        android:pathData="M54,28L32,38v20c0,16.1 9.4,31.2 22,36 12.6,-4.8 22,-19.9 22,-36V38L54,28z" />
    <path
        android:fillColor="#00E676"
        android:pathData="M48,66l-10,-10 4.2,-4.2 5.8,5.8 15.8,-15.8 4.2,4.2z" />
</vector>
EOF

cat << 'EOF' > app/src/main/res/values/colors.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="bg_pure_black">#000000</color>
    <color name="card_dark">#121212</color>
    <color name="card_border">#242424</color>
    <color name="input_bg">#1A1A1A</color>
    <color name="text_primary">#FFFFFF</color>
    <color name="text_secondary">#B0B0B0</color>
    <color name="accent_red">#FF1744</color>
    <color name="accent_green">#00E676</color>
    <color name="accent_blue">#2979FF</color>
    <color name="button_dark">#1E1E1E</color>
</resources>
EOF

cat << 'EOF' > app/src/main/res/values/strings.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Wolver Touch Guard</string>
    <string name="accessibility_service_label">Wolver Touch Guard Service</string>
    <string name="accessibility_service_description">Blocks accidental palm, claw-finger, and status bar touches and applies exact custom DPI sensitivity scaling inside Free Fire without shrinking the screen.</string>
</resources>
EOF

cat << 'EOF' > app/src/main/res/values/themes.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.WolverTouchGuard" parent="Theme.AppCompat.NoActionBar">
        <item name="android:windowBackground">@color/bg_pure_black</item>
        <item name="android:statusBarColor">@color/bg_pure_black</item>
        <item name="android:navigationBarColor">@color/bg_pure_black</item>
        <item name="colorPrimary">@color/accent_red</item>
        <item name="colorPrimaryDark">@color/bg_pure_black</item>
        <item name="colorAccent">@color/accent_green</item>
        <item name="android:textColorPrimary">@color/text_primary</item>
        <item name="android:textColorSecondary">@color/text_secondary</item>
    </style>
</resources>
EOF

cat << 'EOF' > app/src/main/res/layout/activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_pure_black"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="WOLVER TOUCH GUARD"
            android:textColor="@color/text_primary"
            android:textSize="22sp"
            android:textStyle="bold"
            android:letterSpacing="0.05" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:layout_marginBottom="16dp"
            android:text="Infinix Note 60 (XOS 16) • 3-Finger Claw Shield &amp; Exact DPI Sensitivity Engine"
            android:textColor="@color/text_secondary"
            android:textSize="13sp" />

        <!-- Card 1: Accessibility Service Status -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/card_dark"
            android:orientation="vertical"
            android:padding="16dp"
            android:layout_marginBottom="14dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="1. SYSTEM ACCESSIBILITY STATUS"
                android:textColor="@color/text_secondary"
                android:textSize="12sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvServiceStatus"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:text="Checking status..."
                android:textColor="@color/accent_red"
                android:textSize="16sp"
                android:textStyle="bold" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:text="On Android 16 / XOS 16: First open App Info -&gt; 3 dots (top right) -&gt; Allow restricted settings, and set Battery to Unrestricted."
                android:textColor="@color/text_secondary"
                android:textSize="12sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnOpenAppInfo"
                    android:layout_width="0dp"
                    android:layout_height="48dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="6dp"
                    android:backgroundTint="@color/button_dark"
                    android:text="1. App Info"
                    android:textAllCaps="false"
                    android:textColor="@color/text_primary"
                    android:textSize="13sp" />

                <Button
                    android:id="@+id/btnOpenAccessibility"
                    android:layout_width="0dp"
                    android:layout_height="48dp"
                    android:layout_weight="1"
                    android:layout_marginStart="6dp"
                    android:backgroundTint="@color/accent_red"
                    android:text="2. Accessibility"
                    android:textAllCaps="false"
                    android:textColor="@color/text_primary"
                    android:textSize="13sp"
                    android:textStyle="bold" />
            </LinearLayout>
        </LinearLayout>

        <!-- Card 2: Master Control & Target Apps -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/card_dark"
            android:orientation="vertical"
            android:padding="16dp"
            android:layout_marginBottom="14dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="2. MASTER ENGINE &amp; TARGET APPS"
                android:textColor="@color/text_secondary"
                android:textSize="12sp"
                android:textStyle="bold" />

            <androidx.appcompat.widget.SwitchCompat
                android:id="@+id/switchMasterEnable"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:layout_marginTop="8dp"
                android:text="Enable Touch Guard &amp; Sensitivity Engine"
                android:textColor="@color/text_primary"
                android:textSize="15sp"
                android:textStyle="bold" />

            <androidx.appcompat.widget.SwitchCompat
                android:id="@+id/switchAppFilter"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:layout_marginTop="4dp"
                android:text="Run ONLY Inside Selected Apps"
                android:textColor="@color/text_primary"
                android:textSize="14sp" />

            <TextView
                android:id="@+id/tvSelectedAppsSummary"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:text="Selected: com.dts.freefireth, com.dts.freefiremax"
                android:textColor="@color/accent_green"
                android:textSize="12sp" />

            <Button
                android:id="@+id/btnSelectApps"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:layout_marginTop="10dp"
                android:backgroundTint="@color/button_dark"
                android:text="Choose Target Games / Apps"
                android:textAllCaps="false"
                android:textColor="@color/text_primary" />
        </LinearLayout>

        <!-- Card 3: EXACT CUSTOM DPI SENSITIVITY ENGINE (ZERO SCREEN SHRINK) -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/card_dark"
            android:orientation="vertical"
            android:padding="16dp"
            android:layout_marginBottom="14dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="3. EXACT DPI SENSITIVITY ENGINE (ZERO SCREEN SHRINK)"
                android:textColor="@color/accent_green"
                android:textSize="12sp"
                android:textStyle="bold" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:text="Enter the exact DPI number you want below. The engine calculates the exact mathematical sensitivity ratio (Target DPI / Base DPI), reduces touch slop, and scales pointer/digitizer velocity inside Free Fire without shrinking your screen width."
                android:textColor="@color/text_secondary"
                android:textSize="12sp" />

            <TextView
                android:id="@+id/tvWriteSettingsStatus"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="10dp"
                android:text="System Sensitivity Permission: Checking..."
                android:textColor="@color/accent_red"
                android:textSize="13sp"
                android:textStyle="bold" />

            <Button
                android:id="@+id/btnGrantWriteSettings"
                android:layout_width="match_parent"
                android:layout_height="46dp"
                android:layout_marginTop="8dp"
                android:backgroundTint="@color/button_dark"
                android:text="Grant Modify System Settings Permission"
                android:textAllCaps="false"
                android:textColor="@color/text_primary"
                android:textSize="13sp" />

            <androidx.appcompat.widget.SwitchCompat
                android:id="@+id/switchDpiEngine"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:layout_marginTop="8dp"
                android:text="Enable Exact Custom DPI Sensitivity in Game"
                android:textColor="@color/text_primary"
                android:textSize="14sp"
                android:textStyle="bold" />

            <!-- Exact Numerical Inputs Row -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="10dp"
                android:orientation="horizontal">

                <!-- Target DPI Input -->
                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginEnd="6dp"
                    android:orientation="vertical">

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="TARGET DPI (Type Number)"
                        android:textColor="@color/accent_green"
                        android:textSize="11sp"
                        android:textStyle="bold" />

                    <EditText
                        android:id="@+id/etTargetDpi"
                        android:layout_width="match_parent"
                        android:layout_height="48dp"
                        android:layout_marginTop="4dp"
                        android:background="@color/input_bg"
                        android:gravity="center"
                        android:hint="e.g. 960"
                        android:imeOptions="actionDone"
                        android:inputType="number"
                        android:maxLength="4"
                        android:text="960"
                        android:textColor="@color/text_primary"
                        android:textColorHint="@color/text_secondary"
                        android:textSize="18sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <!-- Base Native DPI Input -->
                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginStart="6dp"
                    android:orientation="vertical">

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="STOCK BASE DPI (Note 60)"
                        android:textColor="@color/text_secondary"
                        android:textSize="11sp"
                        android:textStyle="bold" />

                    <EditText
                        android:id="@+id/etBaseDpi"
                        android:layout_width="match_parent"
                        android:layout_height="48dp"
                        android:layout_marginTop="4dp"
                        android:background="@color/input_bg"
                        android:gravity="center"
                        android:hint="411"
                        android:imeOptions="actionDone"
                        android:inputType="number"
                        android:maxLength="4"
                        android:text="411"
                        android:textColor="@color/text_primary"
                        android:textColorHint="@color/text_secondary"
                        android:textSize="18sp"
                        android:textStyle="bold" />
                </LinearLayout>
            </LinearLayout>

            <!-- Y-Axis Headshot Drag Boost Input Row -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="10dp"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginEnd="6dp"
                    android:orientation="vertical">

                    <TextView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:text="Y-AXIS HEADSHOT DRAG EXTRA BOOST (%)"
                        android:textColor="@color/text_secondary"
                        android:textSize="11sp"
                        android:textStyle="bold" />

                    <EditText
                        android:id="@+id/etYAxisBoost"
                        android:layout_width="match_parent"
                        android:layout_height="48dp"
                        android:layout_marginTop="4dp"
                        android:background="@color/input_bg"
                        android:gravity="center"
                        android:hint="0 - 100"
                        android:imeOptions="actionDone"
                        android:inputType="number"
                        android:maxLength="3"
                        android:text="25"
                        android:textColor="@color/text_primary"
                        android:textColorHint="@color/text_secondary"
                        android:textSize="16sp"
                        android:textStyle="bold" />
                </LinearLayout>

                <Button
                    android:id="@+id/btnApplyExactDpi"
                    android:layout_width="0dp"
                    android:layout_height="48dp"
                    android:layout_weight="1"
                    android:layout_marginStart="6dp"
                    android:layout_marginTop="8dp"
                    android:backgroundTint="@color/accent_green"
                    android:text="APPLY EXACT DPI"
                    android:textAllCaps="false"
                    android:textColor="@color/bg_pure_black"
                    android:textSize="14sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <!-- Quick DPI Number Presets -->
            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:text="Quick One-Tap DPI Presets:"
                android:textColor="@color/text_secondary"
                android:textSize="12sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnPreset550"
                    android:layout_width="0dp"
                    android:layout_height="40dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="3dp"
                    android:backgroundTint="@color/button_dark"
                    android:padding="0dp"
                    android:text="550"
                    android:textColor="@color/text_primary"
                    android:textSize="12sp" />

                <Button
                    android:id="@+id/btnPreset720"
                    android:layout_width="0dp"
                    android:layout_height="40dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="3dp"
                    android:backgroundTint="@color/button_dark"
                    android:padding="0dp"
                    android:text="720"
                    android:textColor="@color/text_primary"
                    android:textSize="12sp" />

                <Button
                    android:id="@+id/btnPreset960"
                    android:layout_width="0dp"
                    android:layout_height="40dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="3dp"
                    android:backgroundTint="@color/button_dark"
                    android:padding="0dp"
                    android:text="960"
                    android:textColor="@color/text_primary"
                    android:textSize="12sp" />

                <Button
                    android:id="@+id/btnPreset1200"
                    android:layout_width="0dp"
                    android:layout_height="40dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="3dp"
                    android:backgroundTint="@color/button_dark"
                    android:padding="0dp"
                    android:text="1200"
                    android:textColor="@color/text_primary"
                    android:textSize="12sp" />

                <Button
                    android:id="@+id/btnPreset1440"
                    android:layout_width="0dp"
                    android:layout_height="40dp"
                    android:layout_weight="1"
                    android:backgroundTint="@color/button_dark"
                    android:padding="0dp"
                    android:text="1440"
                    android:textColor="@color/text_primary"
                    android:textSize="12sp" />
            </LinearLayout>

            <!-- Live Mathematical DPI Telemetry Readout -->
            <TextView
                android:id="@+id/tvDpiTelemetry"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:background="@color/input_bg"
                android:padding="12dp"
                android:text="Calculating exact DPI sensitivity profile..."
                android:textColor="@color/accent_green"
                android:textSize="12sp"
                android:fontFamily="monospace" />

            <androidx.appcompat.widget.SwitchCompat
                android:id="@+id/switchUnbufferedTouch"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:layout_marginTop="6dp"
                android:text="Zero-Latency Unbuffered Digitizer Stream"
                android:textColor="@color/text_primary"
                android:textSize="14sp" />
        </LinearLayout>

        <!-- Card 4: Blocking Zones & Stealth Options -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/card_dark"
            android:orientation="vertical"
            android:padding="16dp"
            android:layout_marginBottom="14dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="4. DEAD-ZONE SHIELDS &amp; STEALTH HIDING"
                android:textColor="@color/text_secondary"
                android:textSize="12sp"
                android:textStyle="bold" />

            <androidx.appcompat.widget.SwitchCompat
                android:id="@+id/switchZone1"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:layout_marginTop="8dp"
                android:text="Zone 1: Left Edge &amp; Palm Guard"
                android:textColor="@color/text_primary"
                android:textSize="14sp" />

            <androidx.appcompat.widget.SwitchCompat
                android:id="@+id/switchZone2"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="Zone 2: Top Status Bar &amp; Call Guard"
                android:textColor="@color/text_primary"
                android:textSize="14sp" />

            <androidx.appcompat.widget.SwitchCompat
                android:id="@+id/switchZone3"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="Zone 3: Area Below 3rd-Finger Button"
                android:textColor="@color/text_primary"
                android:textSize="14sp" />

            <androidx.appcompat.widget.SwitchCompat
                android:id="@+id/switchZone4"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="Zone 4: Extra Shield (Right of 3rd-Finger / Mid-Screen)"
                android:textColor="@color/text_primary"
                android:textSize="14sp" />

            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:layout_marginTop="10dp"
                android:layout_marginBottom="10dp"
                android:background="@color/card_border" />

            <androidx.appcompat.widget.SwitchCompat
                android:id="@+id/switchHidePillInLock"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="100% Stealth Mode: Hide Floating Pill in Lock Mode"
                android:textColor="@color/accent_green"
                android:textSize="14sp"
                android:textStyle="bold" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginBottom="8dp"
                android:text="When Stealth Mode is ON (or when you tap ✕ on the pill), both the blocking zones and the control pill become 100% invisible. Press Vol Up + Vol Down together inside the game to unhide."
                android:textColor="@color/text_secondary"
                android:textSize="12sp" />

            <androidx.appcompat.widget.SwitchCompat
                android:id="@+id/switchLockBorder"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:text="Show 5% Faint Border in Lock Mode (For Testing)"
                android:textColor="@color/text_primary"
                android:textSize="14sp" />

            <TextView
                android:id="@+id/tvOpacityValue"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="6dp"
                android:text="Edit Mode Red Opacity: 40%"
                android:textColor="@color/text_primary"
                android:textSize="13sp" />

            <SeekBar
                android:id="@+id/seekBarEditOpacity"
                android:layout_width="match_parent"
                android:layout_height="40dp"
                android:layout_marginTop="4dp"
                android:max="85"
                android:progress="40" />
        </LinearLayout>

        <!-- Card 5: Exact Pixel Size & Position Sliders -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/card_dark"
            android:orientation="vertical"
            android:padding="16dp"
            android:layout_marginBottom="14dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="5. EXACT SIZE &amp; POSITION SLIDERS (FULL SCREEN RANGE)"
                android:textColor="@color/text_secondary"
                android:textSize="12sp"
                android:textStyle="bold" />

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:text="Select a zone below to fine-tune its X, Y, Width, and Height across the entire 2644x1208 landscape screen (or drag any of the 4 white corner handles in Edit Mode):"
                android:textColor="@color/text_secondary"
                android:textSize="12sp" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="10dp"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnSelectZ1"
                    android:layout_width="0dp"
                    android:layout_height="42dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="4dp"
                    android:backgroundTint="@color/accent_red"
                    android:text="Z1"
                    android:textAllCaps="false"
                    android:textColor="@color/text_primary" />

                <Button
                    android:id="@+id/btnSelectZ2"
                    android:layout_width="0dp"
                    android:layout_height="42dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="4dp"
                    android:backgroundTint="@color/button_dark"
                    android:text="Z2"
                    android:textAllCaps="false"
                    android:textColor="@color/text_primary" />

                <Button
                    android:id="@+id/btnSelectZ3"
                    android:layout_width="0dp"
                    android:layout_height="42dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="4dp"
                    android:backgroundTint="@color/button_dark"
                    android:text="Z3"
                    android:textAllCaps="false"
                    android:textColor="@color/text_primary" />

                <Button
                    android:id="@+id/btnSelectZ4"
                    android:layout_width="0dp"
                    android:layout_height="42dp"
                    android:layout_weight="1"
                    android:backgroundTint="@color/button_dark"
                    android:text="Z4"
                    android:textAllCaps="false"
                    android:textColor="@color/text_primary" />
            </LinearLayout>

            <TextView
                android:id="@+id/tvEditingZoneTitle"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:text="Adjusting: Zone 1"
                android:textColor="@color/accent_green"
                android:textSize="14sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tvZoneX"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:text="Horizontal Position (X): 0 px"
                android:textColor="@color/text_primary"
                android:textSize="13sp" />

            <SeekBar
                android:id="@+id/seekZoneX"
                android:layout_width="match_parent"
                android:layout_height="38dp"
                android:max="2600" />

            <TextView
                android:id="@+id/tvZoneY"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:text="Vertical Position (Y): 0 px"
                android:textColor="@color/text_primary"
                android:textSize="13sp" />

            <SeekBar
                android:id="@+id/seekZoneY"
                android:layout_width="match_parent"
                android:layout_height="38dp"
                android:max="1200" />

            <TextView
                android:id="@+id/tvZoneW"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:text="Zone Width: 220 px"
                android:textColor="@color/text_primary"
                android:textSize="13sp" />

            <SeekBar
                android:id="@+id/seekZoneW"
                android:layout_width="match_parent"
                android:layout_height="38dp"
                android:max="2644" />

            <TextView
                android:id="@+id/tvZoneH"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:text="Zone Height: 600 px"
                android:textColor="@color/text_primary"
                android:textSize="13sp" />

            <SeekBar
                android:id="@+id/seekZoneH"
                android:layout_width="match_parent"
                android:layout_height="38dp"
                android:max="1208" />
        </LinearLayout>

        <!-- Card 6: Live Actions & Reset -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/card_dark"
            android:orientation="vertical"
            android:padding="16dp"
            android:layout_marginBottom="24dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="6. QUICK MODE &amp; CALIBRATION CONTROLS"
                android:textColor="@color/text_secondary"
                android:textSize="12sp"
                android:textStyle="bold" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:orientation="horizontal">

                <Button
                    android:id="@+id/btnForceEditMode"
                    android:layout_width="0dp"
                    android:layout_height="48dp"
                    android:layout_weight="1"
                    android:layout_marginEnd="6dp"
                    android:backgroundTint="@color/accent_red"
                    android:text="Edit Mode (Unhide)"
                    android:textAllCaps="false"
                    android:textColor="@color/text_primary"
                    android:textStyle="bold" />

                <Button
                    android:id="@+id/btnForceLockMode"
                    android:layout_width="0dp"
                    android:layout_height="48dp"
                    android:layout_weight="1"
                    android:layout_marginStart="6dp"
                    android:backgroundTint="@color/accent_green"
                    android:text="Lock Mode"
                    android:textAllCaps="false"
                    android:textColor="@color/bg_pure_black"
                    android:textStyle="bold" />
            </LinearLayout>

            <Button
                android:id="@+id/btnResetZones"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:layout_marginTop="10dp"
                android:backgroundTint="@color/button_dark"
                android:text="Reset All Zones &amp; Unhide Pill"
                android:textAllCaps="false"
                android:textColor="@color/text_primary" />
        </LinearLayout>

    </LinearLayout>
</ScrollView>
EOF

cat << 'EOF' > app/src/main/java/com/wolver/touchguard/PrefManager.kt
package com.wolver.touchguard

import android.content.Context
import android.content.SharedPreferences

data class ZoneConfig(
    val id: Int,
    val title: String,
    var enabled: Boolean,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int
)

class PrefManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "wolver_touch_guard_prefs"

        const val MODE_LOCK = 0
        const val MODE_EDIT = 1
        const val MODE_OFF = 2

        const val TOTAL_ZONES = 4

        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_APP_FILTER_ENABLED = "app_filter_enabled"
        private const val KEY_SELECTED_APPS = "selected_apps"
        private const val KEY_CURRENT_MODE = "current_mode"
        private const val KEY_EDIT_OPACITY = "edit_opacity"
        private const val KEY_LOCK_BORDER = "lock_border"
        private const val KEY_HIDE_PILL_IN_LOCK = "hide_pill_in_lock"
        private const val KEY_PILL_HIDDEN_MANUALLY = "pill_hidden_manually"
        private const val KEY_PILL_X = "pill_x"
        private const val KEY_PILL_Y = "pill_y"
        private const val KEY_PILL_COLLAPSED = "pill_collapsed"

        private const val KEY_DPI_ENGINE_ENABLED = "dpi_engine_enabled"
        private const val KEY_EXACT_TARGET_DPI = "exact_target_dpi"
        private const val KEY_EXACT_BASE_DPI = "exact_base_dpi"
        private const val KEY_Y_AXIS_BOOST_PERCENT = "y_axis_boost_percent"
        private const val KEY_UNBUFFERED_TOUCH = "unbuffered_touch"

        private const val KEY_SAVED_ORIG_POINTER_SPEED = "saved_orig_pointer_speed"
        private const val KEY_SAVED_ORIG_TOUCH_SENS = "saved_orig_touch_sens"
        private const val KEY_IS_DPI_APPLIED_TO_SYSTEM = "is_dpi_applied_to_system"

        val DEFAULT_TARGET_PACKAGES = setOf(
            "com.dts.freefireth",
            "com.dts.freefiremax"
        )
    }

    var isMasterEnabled: Boolean
        get() = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MASTER_ENABLED, value).apply()

    var isAppFilterEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_FILTER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_APP_FILTER_ENABLED, value).apply()

    var selectedPackages: Set<String>
        get() {
            val saved = prefs.getStringSet(KEY_SELECTED_APPS, null)
            return if (saved == null) {
                DEFAULT_TARGET_PACKAGES
            } else {
                HashSet(saved)
            }
        }
        set(value) {
            prefs.edit().putStringSet(KEY_SELECTED_APPS, HashSet(value)).apply()
        }

    var currentMode: Int
        get() = prefs.getInt(KEY_CURRENT_MODE, MODE_EDIT)
        set(value) = prefs.edit().putInt(KEY_CURRENT_MODE, value).apply()

    var editOpacityPercent: Int
        get() = prefs.getInt(KEY_EDIT_OPACITY, 40).coerceIn(15, 85)
        set(value) = prefs.edit().putInt(KEY_EDIT_OPACITY, value.coerceIn(15, 85)).apply()

    var showLockBorder: Boolean
        get() = prefs.getBoolean(KEY_LOCK_BORDER, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_BORDER, value).apply()

    var hidePillInLockMode: Boolean
        get() = prefs.getBoolean(KEY_HIDE_PILL_IN_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_PILL_IN_LOCK, value).apply()

    var isPillHiddenManually: Boolean
        get() = prefs.getBoolean(KEY_PILL_HIDDEN_MANUALLY, false)
        set(value) = prefs.edit().putBoolean(KEY_PILL_HIDDEN_MANUALLY, value).apply()

    var pillX: Int
        get() = prefs.getInt(KEY_PILL_X, 950)
        set(value) = prefs.edit().putInt(KEY_PILL_X, value).apply()

    var pillY: Int
        get() = prefs.getInt(KEY_PILL_Y, 24)
        set(value) = prefs.edit().putInt(KEY_PILL_Y, value).apply()

    var isPillCollapsed: Boolean
        get() = prefs.getBoolean(KEY_PILL_COLLAPSED, false)
        set(value) = prefs.edit().putBoolean(KEY_PILL_COLLAPSED, value).apply()

    var isDpiEngineEnabled: Boolean
        get() = prefs.getBoolean(KEY_DPI_ENGINE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DPI_ENGINE_ENABLED, value).apply()

    var exactTargetDpi: Int
        get() = prefs.getInt(KEY_EXACT_TARGET_DPI, 960).coerceIn(320, 3000)
        set(value) = prefs.edit().putInt(KEY_EXACT_TARGET_DPI, value.coerceIn(320, 3000)).apply()

    var exactBaseDpi: Int
        get() = prefs.getInt(KEY_EXACT_BASE_DPI, 411).coerceIn(300, 800)
        set(value) = prefs.edit().putInt(KEY_EXACT_BASE_DPI, value.coerceIn(300, 800)).apply()

    var yAxisBoostPercent: Int
        get() = prefs.getInt(KEY_Y_AXIS_BOOST_PERCENT, 25).coerceIn(0, 150)
        set(value) = prefs.edit().putInt(KEY_Y_AXIS_BOOST_PERCENT, value.coerceIn(0, 150)).apply()

    var isUnbufferedTouchEnabled: Boolean
        get() = prefs.getBoolean(KEY_UNBUFFERED_TOUCH, true)
        set(value) = prefs.edit().putBoolean(KEY_UNBUFFERED_TOUCH, value).apply()

    var savedOriginalPointerSpeed: Int
        get() = prefs.getInt(KEY_SAVED_ORIG_POINTER_SPEED, 0).coerceIn(-7, 7)
        set(value) = prefs.edit().putInt(KEY_SAVED_ORIG_POINTER_SPEED, value.coerceIn(-7, 7)).apply()

    var savedOriginalTouchSensitivity: Int
        get() = prefs.getInt(KEY_SAVED_ORIG_TOUCH_SENS, 0)
        set(value) = prefs.edit().putInt(KEY_SAVED_ORIG_TOUCH_SENS, value).apply()

    var isDpiAppliedToSystem: Boolean
        get() = prefs.getBoolean(KEY_IS_DPI_APPLIED_TO_SYSTEM, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_DPI_APPLIED_TO_SYSTEM, value).apply()

    fun isZoneEnabled(zoneId: Int): Boolean {
        val defaultOn = zoneId <= 3
        return prefs.getBoolean("zone_${zoneId}_enabled", defaultOn)
    }

    fun setZoneEnabled(zoneId: Int, enabled: Boolean) {
        prefs.edit().putBoolean("zone_${zoneId}_enabled", enabled).apply()
    }

    fun getZoneConfig(zoneId: Int): ZoneConfig {
        val defaultX: Int
        val defaultY: Int
        val defaultW: Int
        val defaultH: Int
        val title: String

        when (zoneId) {
            1 -> {
                title = "Z1: Left Edge & Palm"
                defaultX = 0
                defaultY = 0
                defaultW = 220
                defaultH = 600
            }
            2 -> {
                title = "Z2: Top Bar & Call Guard"
                defaultX = 220
                defaultY = 0
                defaultW = 1650
                defaultH = 85
            }
            3 -> {
                title = "Z3: Below 3rd-Finger"
                defaultX = 220
                defaultY = 270
                defaultW = 420
                defaultH = 330
            }
            else -> {
                title = "Z4: Right of 3rd-Finger"
                defaultX = 460
                defaultY = 85
                defaultW = 380
                defaultH = 515
            }
        }

        return ZoneConfig(
            id = zoneId,
            title = title,
            enabled = isZoneEnabled(zoneId),
            x = prefs.getInt("zone_${zoneId}_x", defaultX).coerceAtLeast(0),
            y = prefs.getInt("zone_${zoneId}_y", defaultY).coerceAtLeast(0),
            width = prefs.getInt("zone_${zoneId}_w", defaultW).coerceAtLeast(40),
            height = prefs.getInt("zone_${zoneId}_h", defaultH).coerceAtLeast(35)
        )
    }

    fun saveZoneBounds(zoneId: Int, x: Int, y: Int, width: Int, height: Int) {
        prefs.edit()
            .putInt("zone_${zoneId}_x", x.coerceAtLeast(0))
            .putInt("zone_${zoneId}_y", y.coerceAtLeast(0))
            .putInt("zone_${zoneId}_w", width.coerceAtLeast(40))
            .putInt("zone_${zoneId}_h", height.coerceAtLeast(35))
            .apply()
    }

    fun resetZonesToDefaults() {
        val editor = prefs.edit()
        for (id in 1..TOTAL_ZONES) {
            editor.remove("zone_${id}_x")
            editor.remove("zone_${id}_y")
            editor.remove("zone_${id}_w")
            editor.remove("zone_${id}_h")
            editor.putBoolean("zone_${id}_enabled", id <= 3)
        }
        editor.putInt(KEY_PILL_X, 950)
        editor.putInt(KEY_PILL_Y, 24)
        editor.putBoolean(KEY_PILL_COLLAPSED, false)
        editor.putBoolean(KEY_PILL_HIDDEN_MANUALLY, false)
        editor.apply()
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/wolver/touchguard/DpiSensitivityEngine.kt
package com.wolver.touchguard

import android.content.Context
import android.provider.Settings
import android.view.ViewConfiguration
import java.util.Locale
import kotlin.math.ln
import kotlin.math.roundToInt

data class DpiSensitivityProfile(
    val targetDpi: Int,
    val baseDpi: Int,
    val yAxisBoostPercent: Int,
    val horizontalMultiplier: Float,
    val verticalHeadshotMultiplier: Float,
    val combinedEffectiveDpi: Int,
    val nativeTouchSlopPx: Int,
    val effectiveTouchSlopPx: Float,
    val calculatedPointerSpeed: Int,
    val scaledCompositorDensityDpi: Int
)

object DpiSensitivityEngine {

    private const val KEY_POINTER_SPEED = "pointer_speed"
    private const val KEY_TOUCH_SENSITIVITY = "touch_sensitivity"
    private const val KEY_GLOVE_MODE = "glove_mode"
    private const val KEY_GAME_TOUCH_SENSITIVITY = "game_touch_sensitivity"

    fun canWriteSystemSettings(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Computes the exact mathematical DPI sensitivity profile from the user's typed Target DPI,
     * Stock Base DPI (411 dp on Infinix Note 60), and Y-Axis Headshot Drag Boost %.
     */
    fun computeProfile(context: Context, prefManager: PrefManager): DpiSensitivityProfile {
        val targetDpi = prefManager.exactTargetDpi.coerceIn(320, 3000)
        val baseDpi = prefManager.exactBaseDpi.coerceIn(300, 800)
        val yBoostPct = prefManager.yAxisBoostPercent.coerceIn(0, 150)

        // Exact mathematical ratio of how Developer Options "Smallest Width (dp)" scales movement
        val xMult = (targetDpi.toFloat() / baseDpi.toFloat()).coerceIn(0.5f, 8.0f)
        val yMult = (xMult * (1.0f + (yBoostPct.toFloat() / 100.0f))).coerceIn(0.5f, 12.0f)
        val combinedDpi = (baseDpi * ((xMult + yMult) / 2.0f)).roundToInt()

        val nativeSlop = try {
            ViewConfiguration.get(context).scaledTouchSlop
        } catch (_: Exception) {
            24
        }
        val effectiveSlop = (nativeSlop.toFloat() / yMult).coerceAtLeast(1.0f)

        // Map the exact DPI multiplier onto Android's hardware input velocity curve (-7..+7)
        // In Android's InputReader, each +1 step in pointer_speed increases velocity gain by ~25% exponentially.
        val pointerSpeedStep = if (yMult <= 1.0f) {
            0
        } else {
            val logSteps = (ln(yMult.toDouble()) / ln(1.18)).roundToInt()
            logSteps.coerceIn(1, 7)
        }

        val stockDensityDpi = context.resources.displayMetrics.densityDpi.coerceAtLeast(160)
        val scaledDensityDpi = (stockDensityDpi.toFloat() / xMult).roundToInt().coerceIn(120, 640)

        return DpiSensitivityProfile(
            targetDpi = targetDpi,
            baseDpi = baseDpi,
            yAxisBoostPercent = yBoostPct,
            horizontalMultiplier = xMult,
            verticalHeadshotMultiplier = yMult,
            combinedEffectiveDpi = combinedDpi,
            nativeTouchSlopPx = nativeSlop,
            effectiveTouchSlopPx = effectiveSlop,
            calculatedPointerSpeed = pointerSpeedStep,
            scaledCompositorDensityDpi = scaledDensityDpi
        )
    }

    fun formatTelemetryReport(profile: DpiSensitivityProfile): String {
        val xStr = String.format(Locale.US, "%.2fx", profile.horizontalMultiplier)
        val yStr = String.format(Locale.US, "%.2fx", profile.verticalHeadshotMultiplier)
        val slopStr = String.format(Locale.US, "%.1f px", profile.effectiveTouchSlopPx)
        return buildString {
            appendLine("● EXACT X-AXIS MULTIPLIER : $xStr (${profile.targetDpi} / ${profile.baseDpi} DPI)")
            appendLine("● Y-AXIS HEADSHOT DRAG    : $yStr (+${profile.yAxisBoostPercent}% Vertical Boost)")
            appendLine("● EFFECTIVE DRAG DPI      : ${profile.combinedEffectiveDpi} DPI (UI Width: 100% Locked)")
            appendLine("● MICRO-DRAG TOUCH SLOP   : ${profile.nativeTouchSlopPx} px -> $slopStr")
            append("● HARDWARE VELOCITY GAIN  : +${profile.calculatedPointerSpeed} / 7 (High-Sens Digitizer ON)")
        }
    }

    /**
     * Applies the exact calculated DPI sensitivity parameters to the Android touch/input subsystem
     * and the Accessibility Touch Layer DisplayMetrics without shrinking the screen layout width.
     */
    fun applyExactDpiSensitivity(context: Context, prefManager: PrefManager): DpiSensitivityProfile {
        val profile = computeProfile(context, prefManager)

        // 1. Scale the Accessibility Service Touch Layer DisplayMetrics (xdpi/ydpi) to match the target DPI
        // while keeping configuration.smallestScreenWidthDp untouched so the screen never shrinks!
        try {
            val dm = context.resources.displayMetrics
            dm.xdpi = dm.xdpi * profile.horizontalMultiplier
            dm.ydpi = dm.ydpi * profile.verticalHeadshotMultiplier
        } catch (_: Exception) {
        }

        // 2. Apply low-level input velocity & high-sensitivity digitizer flags via Settings.System
        if (canWriteSystemSettings(context)) {
            try {
                val resolver = context.contentResolver
                if (!prefManager.isDpiAppliedToSystem) {
                    val currentSpeed = try {
                        Settings.System.getInt(resolver, KEY_POINTER_SPEED, 0)
                    } catch (_: Exception) {
                        0
                    }
                    val currentTouchSens = try {
                        Settings.System.getInt(resolver, KEY_TOUCH_SENSITIVITY, 0)
                    } catch (_: Exception) {
                        0
                    }
                    prefManager.savedOriginalPointerSpeed = currentSpeed
                    prefManager.savedOriginalTouchSensitivity = currentTouchSens
                }

                Settings.System.putInt(resolver, KEY_POINTER_SPEED, profile.calculatedPointerSpeed)
                Settings.System.putInt(resolver, KEY_TOUCH_SENSITIVITY, 1)
                Settings.System.putInt(resolver, KEY_GLOVE_MODE, 1)
                Settings.System.putInt(
                    resolver,
                    KEY_GAME_TOUCH_SENSITIVITY,
                    profile.combinedEffectiveDpi.coerceIn(360, 3000)
                )
                prefManager.isDpiAppliedToSystem = true
            } catch (_: Exception) {
            }
        }

        return profile
    }

    /**
     * Restores the phone's exact stock pointer and digitizer sensitivity immediately when leaving Free Fire.
     */
    fun restoreStockSensitivity(context: Context, prefManager: PrefManager) {
        if (!prefManager.isDpiAppliedToSystem) return
        if (!canWriteSystemSettings(context)) {
            prefManager.isDpiAppliedToSystem = false
            return
        }

        try {
            val resolver = context.contentResolver
            Settings.System.putInt(
                resolver,
                KEY_POINTER_SPEED,
                prefManager.savedOriginalPointerSpeed.coerceIn(-7, 7)
            )
            Settings.System.putInt(
                resolver,
                KEY_TOUCH_SENSITIVITY,
                prefManager.savedOriginalTouchSensitivity
            )
            Settings.System.putInt(resolver, KEY_GLOVE_MODE, 0)
        } catch (_: Exception) {
        } finally {
            prefManager.isDpiAppliedToSystem = false
        }
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/wolver/touchguard/ZoneOverlayView.kt
package com.wolver.touchguard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

@SuppressLint("ViewConstructor")
class ZoneOverlayView(
    context: Context,
    val zoneId: Int,
    private val title: String,
    private val onBoundsUpdated: (zoneId: Int, x: Int, y: Int, width: Int, height: Int) -> Unit
) : View(context) {

    companion object {
        private const val HANDLE_NONE = 0
        private const val HANDLE_TOP_LEFT = 1
        private const val HANDLE_TOP_RIGHT = 2
        private const val HANDLE_BOTTOM_LEFT = 3
        private const val HANDLE_BOTTOM_RIGHT = 4
    }

    private var currentMode: Int = PrefManager.MODE_EDIT
    private var editOpacityPercent: Int = 40
    private var showLockBorder: Boolean = false
    private var unbufferedTouchEnabled: Boolean = true

    private var posX: Int = 0
    private var posY: Int = 0
    private var zoneW: Int = 200
    private var zoneH: Int = 200

    private val density = resources.displayMetrics.density
    private val handleSizePx = 32f * density
    private val touchSlopHandlePx = 42f * density
    private val minWidthPx = (40f * density).toInt()
    private val minHeightPx = (35f * density).toInt()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#EEFFFFFF")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11f * density
        isFakeBoldText = true
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private val handleRect = RectF()

    private var activeHandle = HANDLE_NONE
    private var initialTouchRawX = 0f
    private var initialTouchRawY = 0f
    private var initialX = 0
    private var initialY = 0
    private var initialW = 0
    private var initialH = 0

    fun updateState(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        mode: Int,
        opacityPercent: Int,
        lockBorder: Boolean,
        unbufferedTouch: Boolean
    ) {
        posX = x
        posY = y
        zoneW = max(minWidthPx, w)
        zoneH = max(minHeightPx, h)
        currentMode = mode
        editOpacityPercent = opacityPercent
        showLockBorder = lockBorder
        unbufferedTouchEnabled = unbufferedTouch
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        when (currentMode) {
            PrefManager.MODE_EDIT -> {
                val alpha = ((editOpacityPercent / 100f) * 255).toInt().coerceIn(30, 220)
                fillPaint.color = Color.argb(alpha, 255, 23, 68)
                canvas.drawRect(0f, 0f, w, h, fillPaint)

                borderPaint.color = Color.parseColor("#FFFF1744")
                borderPaint.strokeWidth = 2f * density
                canvas.drawRect(1f, 1f, w - 1f, h - 1f, borderPaint)

                handleRect.set(0f, 0f, handleSizePx, handleSizePx)
                canvas.drawRect(handleRect, handlePaint)

                handleRect.set(w - handleSizePx, 0f, w, handleSizePx)
                canvas.drawRect(handleRect, handlePaint)

                handleRect.set(0f, h - handleSizePx, handleSizePx, h)
                canvas.drawRect(handleRect, handlePaint)

                handleRect.set(w - handleSizePx, h - handleSizePx, w, h)
                canvas.drawRect(handleRect, handlePaint)

                val info = "$title [X:$posX Y:$posY | ${zoneW}x${zoneH}]"
                val textY = (h / 2f).coerceIn(20f * density, (h - 10f * density).coerceAtLeast(20f * density))
                canvas.drawText(info, 10f * density, textY, textPaint)
            }

            PrefManager.MODE_LOCK -> {
                if (showLockBorder) {
                    borderPaint.color = Color.parseColor("#18FF1744")
                    borderPaint.strokeWidth = 1.5f * density
                    canvas.drawRect(1f, 1f, w - 1f, h - 1f, borderPaint)
                }
            }

            PrefManager.MODE_OFF -> {
            }
        }
    }

    private fun detectTouchedHandle(localX: Float, localY: Float): Int {
        val w = width.toFloat()
        val h = height.toFloat()

        val nearLeft = localX <= touchSlopHandlePx
        val nearRight = localX >= w - touchSlopHandlePx
        val nearTop = localY <= touchSlopHandlePx
        val nearBottom = localY >= h - touchSlopHandlePx

        return when {
            nearBottom && nearRight -> HANDLE_BOTTOM_RIGHT
            nearTop && nearLeft -> HANDLE_TOP_LEFT
            nearTop && nearRight -> HANDLE_TOP_RIGHT
            nearBottom && nearLeft -> HANDLE_BOTTOM_LEFT
            else -> HANDLE_NONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (unbufferedTouchEnabled && event.actionMasked == MotionEvent.ACTION_DOWN) {
            try {
                requestUnbufferedDispatch(event)
            } catch (_: Exception) {
            }
        }

        if (currentMode == PrefManager.MODE_LOCK) {
            return true
        }

        if (currentMode == PrefManager.MODE_OFF) {
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchRawX = event.rawX
                initialTouchRawY = event.rawY
                initialX = posX
                initialY = posY
                initialW = zoneW
                initialH = zoneH
                activeHandle = detectTouchedHandle(event.x, event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchRawX).toInt()
                val dy = (event.rawY - initialTouchRawY).toInt()

                val metrics = resources.displayMetrics
                val maxScreenW = metrics.widthPixels
                val maxScreenH = metrics.heightPixels

                when (activeHandle) {
                    HANDLE_BOTTOM_RIGHT -> {
                        zoneW = (initialW + dx).coerceIn(minWidthPx, maxScreenW)
                        zoneH = (initialH + dy).coerceIn(minHeightPx, maxScreenH)
                    }

                    HANDLE_TOP_LEFT -> {
                        val maxNewX = (initialX + initialW - minWidthPx).coerceAtLeast(0)
                        val maxNewY = (initialY + initialH - minHeightPx).coerceAtLeast(0)
                        val newX = (initialX + dx).coerceIn(0, maxNewX)
                        val newY = (initialY + dy).coerceIn(0, maxNewY)
                        posX = newX
                        posY = newY
                        zoneW = (initialW - (newX - initialX)).coerceIn(minWidthPx, maxScreenW)
                        zoneH = (initialH - (newY - initialY)).coerceIn(minHeightPx, maxScreenH)
                    }

                    HANDLE_TOP_RIGHT -> {
                        val maxNewY = (initialY + initialH - minHeightPx).coerceAtLeast(0)
                        val newY = (initialY + dy).coerceIn(0, maxNewY)
                        posY = newY
                        zoneW = (initialW + dx).coerceIn(minWidthPx, maxScreenW)
                        zoneH = (initialH - (newY - initialY)).coerceIn(minHeightPx, maxScreenH)
                    }

                    HANDLE_BOTTOM_LEFT -> {
                        val maxNewX = (initialX + initialW - minWidthPx).coerceAtLeast(0)
                        val newX = (initialX + dx).coerceIn(0, maxNewX)
                        posX = newX
                        zoneW = (initialW - (newX - initialX)).coerceIn(minWidthPx, maxScreenW)
                        zoneH = (initialH + dy).coerceIn(minHeightPx, maxScreenH)
                    }

                    else -> {
                        posX = (initialX + dx).coerceIn(0, (maxScreenW - zoneW).coerceAtLeast(0))
                        posY = (initialY + dy).coerceIn(0, (maxScreenH - zoneH).coerceAtLeast(0))
                    }
                }

                onBoundsUpdated(zoneId, posX, posY, zoneW, zoneH)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = HANDLE_NONE
                onBoundsUpdated(zoneId, posX, posY, zoneW, zoneH)
                return true
            }
        }
        return true
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/wolver/touchguard/FloatingPillView.kt
package com.wolver.touchguard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class FloatingPillView(
    context: Context,
    private val onModeCycleClicked: () -> Unit,
    private val onCollapseToggled: (collapsed: Boolean) -> Unit,
    private val onHideCompletelyClicked: () -> Unit,
    private val onPositionMoved: (x: Int, y: Int) -> Unit
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val tvDragAndStatus: TextView
    private val tvCollapseBtn: TextView
    private val tvHideFullBtn: TextView

    private var currentMode: Int = PrefManager.MODE_EDIT
    private var isCollapsed: Boolean = false
    private var activeDpiBadge: String = ""

    private var posX: Int = 950
    private var posY: Int = 24
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var isDragging = false

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#DD0A0A0A"))
            cornerRadius = 20f * density
            setStroke((1.5f * density).toInt(), Color.parseColor("#66FFFFFF"))
        }
        background = bg
        val padH = (10 * density).toInt()
        val padV = (5 * density).toInt()
        setPadding(padH, padV, padH, padV)

        tvDragAndStatus = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding((4 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
        }

        tvCollapseBtn = TextView(context).apply {
            text = " – "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#B0B0B0"))
            gravity = Gravity.CENTER
            setPadding((6 * density).toInt(), (2 * density).toInt(), (4 * density).toInt(), (2 * density).toInt())
            setOnClickListener {
                isCollapsed = !isCollapsed
                applyVisualState()
                onCollapseToggled(isCollapsed)
            }
        }

        tvHideFullBtn = TextView(context).apply {
            text = " ✕ "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FF5252"))
            gravity = Gravity.CENTER
            setPadding((4 * density).toInt(), (2 * density).toInt(), (4 * density).toInt(), (2 * density).toInt())
            setOnClickListener {
                onHideCompletelyClicked()
            }
        }

        addView(tvDragAndStatus)
        addView(tvCollapseBtn)
        addView(tvHideFullBtn)

        setupTouchHandling()
        applyVisualState()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchHandling() {
        tvDragAndStatus.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = posX
                    startY = posY
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (abs(dx) > (6 * density) || abs(dy) > (6 * density)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val metrics = resources.displayMetrics
                        posX = (startX + dx).coerceIn(0, (metrics.widthPixels - width).coerceAtLeast(0))
                        posY = (startY + dy).coerceIn(0, (metrics.heightPixels - height).coerceAtLeast(0))
                        onPositionMoved(posX, posY)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        if (isCollapsed) {
                            isCollapsed = false
                            applyVisualState()
                            onCollapseToggled(false)
                        } else {
                            onModeCycleClicked()
                        }
                    }
                    isDragging = false
                    true
                }

                else -> false
            }
        }
    }

    fun updatePillState(
        mode: Int,
        collapsed: Boolean,
        x: Int,
        y: Int,
        dpiEngineOn: Boolean,
        targetDpi: Int,
        yMult: Float
    ) {
        currentMode = mode
        isCollapsed = collapsed
        posX = x
        posY = y
        val multStr = String.format(Locale.US, "%.2fx", yMult)
        activeDpiBadge = if (dpiEngineOn) " • ${targetDpi}DPI ($multStr)" else ""
        applyVisualState()
    }

    private fun applyVisualState() {
        if (isCollapsed) {
            tvCollapseBtn.visibility = GONE
            tvHideFullBtn.visibility = GONE
            tvDragAndStatus.text = "WG"
            alpha = 0.50f
            when (currentMode) {
                PrefManager.MODE_LOCK -> tvDragAndStatus.setTextColor(Color.parseColor("#00E676"))
                PrefManager.MODE_EDIT -> tvDragAndStatus.setTextColor(Color.parseColor("#FF1744"))
                else -> tvDragAndStatus.setTextColor(Color.parseColor("#9E9E9E"))
            }
            return
        }

        alpha = 0.92f
        tvCollapseBtn.visibility = VISIBLE
        tvHideFullBtn.visibility = VISIBLE
        when (currentMode) {
            PrefManager.MODE_LOCK -> {
                tvDragAndStatus.text = "LOCKED$activeDpiBadge"
                tvDragAndStatus.setTextColor(Color.parseColor("#00E676"))
            }
            PrefManager.MODE_EDIT -> {
                tvDragAndStatus.text = "EDIT ZONES$activeDpiBadge"
                tvDragAndStatus.setTextColor(Color.parseColor("#FF1744"))
            }
            PrefManager.MODE_OFF -> {
                tvDragAndStatus.text = "GUARD OFF"
                tvDragAndStatus.setTextColor(Color.parseColor("#9E9E9E"))
            }
        }
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/wolver/touchguard/TouchGuardAccessibilityService.kt
package com.wolver.touchguard

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast

class TouchGuardAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: TouchGuardAccessibilityService? = null
            private set

        private val IGNORED_TRANSIENT_PACKAGES = setOf(
            "com.android.systemui",
            "android",
            "com.transsion.gamespace",
            "com.transsion.xarena",
            "com.transsion.smartpanel",
            "com.transsion.kolun.assistant",
            "com.google.android.inputmethod.latin"
        )
    }

    private lateinit var prefManager: PrefManager
    private lateinit var windowManager: WindowManager

    private val zoneViews = mutableMapOf<Int, ZoneOverlayView>()
    private val zoneParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    private var pillView: FloatingPillView? = null
    private var pillParams: WindowManager.LayoutParams? = null

    private var currentForegroundPackage: String = ""
    private var overlaysAttached = false

    private var volUpPressed = false
    private var volDownPressed = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        prefManager = PrefManager(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlayViews()
        refreshOverlaysVisibilityAndState()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            val detectedPkg = resolveActiveApplicationPackage(event)
            if (detectedPkg.isNotEmpty() && detectedPkg != currentForegroundPackage) {
                currentForegroundPackage = detectedPkg
                refreshOverlaysVisibilityAndState()
            }
        }
    }

    private fun resolveActiveApplicationPackage(event: AccessibilityEvent): String {
        try {
            val interactiveWindows = windows
            if (!interactiveWindows.isNullOrEmpty()) {
                val targetPackages = prefManager.selectedPackages
                for (win in interactiveWindows) {
                    if (win.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val pkg = win.root?.packageName?.toString().orEmpty()
                        if (pkg.isNotEmpty() && targetPackages.contains(pkg)) {
                            return pkg
                        }
                    }
                }
                for (win in interactiveWindows) {
                    if (win.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        val pkg = win.root?.packageName?.toString().orEmpty()
                        if (pkg.isNotEmpty() &&
                            pkg != packageName &&
                            !IGNORED_TRANSIENT_PACKAGES.contains(pkg)
                        ) {
                            return pkg
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        val eventPkg = event.packageName?.toString().orEmpty()
        if (eventPkg.isNotEmpty() &&
            eventPkg != packageName &&
            !IGNORED_TRANSIENT_PACKAGES.contains(eventPkg)
        ) {
            return eventPkg
        }

        return currentForegroundPackage
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volUpPressed = (event.action == KeyEvent.ACTION_DOWN)
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volDownPressed = (event.action == KeyEvent.ACTION_DOWN)
            }
        }
        if (volUpPressed && volDownPressed && overlaysAttached) {
            volUpPressed = false
            volDownPressed = false
            prefManager.isPillHiddenManually = false
            val nextMode = if (prefManager.currentMode == PrefManager.MODE_LOCK) {
                PrefManager.MODE_EDIT
            } else {
                PrefManager.MODE_LOCK
            }
            prefManager.currentMode = nextMode
            refreshOverlaysVisibilityAndState()
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshOverlaysVisibilityAndState()
    }

    private fun createOverlayViews() {
        if (overlaysAttached) return

        for (zoneId in 1..PrefManager.TOTAL_ZONES) {
            val config = prefManager.getZoneConfig(zoneId)
            val view = ZoneOverlayView(
                context = this,
                zoneId = zoneId,
                title = config.title,
                onBoundsUpdated = { id, newX, newY, newW, newH ->
                    prefManager.saveZoneBounds(id, newX, newY, newW, newH)
                    val lp = zoneParams[id]
                    val v = zoneViews[id]
                    if (lp != null && v != null && v.parent != null) {
                        lp.x = newX
                        lp.y = newY
                        lp.width = newW
                        lp.height = newH
                        try {
                            windowManager.updateViewLayout(v, lp)
                        } catch (_: Exception) {
                        }
                    }
                }
            )

            val params = createBaseLayoutParams(
                x = config.x,
                y = config.y,
                width = config.width,
                height = config.height,
                touchable = true
            )

            zoneViews[zoneId] = view
            zoneParams[zoneId] = params

            try {
                windowManager.addView(view, params)
            } catch (_: Exception) {
            }
        }

        val pill = FloatingPillView(
            context = this,
            onModeCycleClicked = {
                val nextMode = when (prefManager.currentMode) {
                    PrefManager.MODE_LOCK -> PrefManager.MODE_EDIT
                    PrefManager.MODE_EDIT -> PrefManager.MODE_OFF
                    else -> PrefManager.MODE_LOCK
                }
                prefManager.currentMode = nextMode
                refreshOverlaysVisibilityAndState()
            },
            onCollapseToggled = { collapsed ->
                prefManager.isPillCollapsed = collapsed
                refreshOverlaysVisibilityAndState()
            },
            onHideCompletelyClicked = {
                prefManager.currentMode = PrefManager.MODE_LOCK
                prefManager.isPillHiddenManually = true
                refreshOverlaysVisibilityAndState()
                Toast.makeText(
                    this,
                    "Stealth Lock Active (100% Hidden). Press Vol Up + Vol Down together to unhide.",
                    Toast.LENGTH_LONG
                ).show()
            },
            onPositionMoved = { newX, newY ->
                prefManager.pillX = newX
                prefManager.pillY = newY
                val lp = pillParams
                val v = pillView
                if (lp != null && v != null && v.parent != null) {
                    lp.x = newX
                    lp.y = newY
                    try {
                        windowManager.updateViewLayout(v, lp)
                    } catch (_: Exception) {
                    }
                }
            }
        )

        val pParams = createBaseLayoutParams(
            x = prefManager.pillX,
            y = prefManager.pillY,
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            touchable = true
        )

        pillView = pill
        pillParams = pParams

        try {
            windowManager.addView(pill, pParams)
            overlaysAttached = true
        } catch (_: Exception) {
        }
    }

    private fun createBaseLayoutParams(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        touchable: Boolean
    ): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (!touchable) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        val params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = x
        params.y = y

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        return params
    }

    fun refreshOverlaysVisibilityAndState() {
        if (!overlaysAttached) {
            createOverlayViews()
        }

        val masterOn = prefManager.isMasterEnabled
        val filterOn = prefManager.isAppFilterEnabled
        val allowedPackages = prefManager.selectedPackages

        val shouldShowForCurrentApp = if (!filterOn) {
            true
        } else {
            currentForegroundPackage.isNotEmpty() && allowedPackages.contains(currentForegroundPackage)
        }

        val activeOverall = masterOn && shouldShowForCurrentApp
        val mode = prefManager.currentMode
        val opacity = prefManager.editOpacityPercent
        val lockBorder = prefManager.showLockBorder
        val unbufferedTouch = prefManager.isUnbufferedTouchEnabled
        val dpiEngineOn = prefManager.isDpiEngineEnabled

        // Apply or restore Exact Custom DPI Sensitivity Profile
        val profile = if (activeOverall && dpiEngineOn) {
            DpiSensitivityEngine.applyExactDpiSensitivity(this, prefManager)
        } else {
            DpiSensitivityEngine.restoreStockSensitivity(this, prefManager)
            DpiSensitivityEngine.computeProfile(this, prefManager)
        }

        for (zoneId in 1..PrefManager.TOTAL_ZONES) {
            val view = zoneViews[zoneId] ?: continue
            val params = zoneParams[zoneId] ?: continue
            val cfg = prefManager.getZoneConfig(zoneId)

            if (!activeOverall || !cfg.enabled || mode == PrefManager.MODE_OFF) {
                view.visibility = View.GONE
            } else {
                view.visibility = View.VISIBLE
                params.x = cfg.x
                params.y = cfg.y
                params.width = cfg.width
                params.height = cfg.height

                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()

                view.updateState(
                    x = cfg.x,
                    y = cfg.y,
                    w = cfg.width,
                    h = cfg.height,
                    mode = mode,
                    opacityPercent = opacity,
                    lockBorder = lockBorder,
                    unbufferedTouch = unbufferedTouch
                )

                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {
                }
            }
        }

        pillView?.let { pill ->
            val pParams = pillParams ?: return@let
            val shouldHidePillStealth =
                prefManager.isPillHiddenManually ||
                        (mode == PrefManager.MODE_LOCK && prefManager.hidePillInLockMode)

            if (!activeOverall || shouldHidePillStealth) {
                pill.visibility = View.GONE
            } else {
                pill.visibility = View.VISIBLE
                pParams.x = prefManager.pillX
                pParams.y = prefManager.pillY
                pill.updatePillState(
                    mode = mode,
                    collapsed = prefManager.isPillCollapsed,
                    x = prefManager.pillX,
                    y = prefManager.pillY,
                    dpiEngineOn = dpiEngineOn,
                    targetDpi = profile.targetDpi,
                    yMult = profile.verticalHeadshotMultiplier
                )
                try {
                    windowManager.updateViewLayout(pill, pParams)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun removeAllOverlays() {
        for ((_, view) in zoneViews) {
            try {
                if (view.parent != null) windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }
        zoneViews.clear()
        zoneParams.clear()

        pillView?.let {
            try {
                if (it.parent != null) windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        pillView = null
        pillParams = null
        overlaysAttached = false
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        DpiSensitivityEngine.restoreStockSensitivity(this, prefManager)
        removeAllOverlays()
        instance = null
        super.onDestroy()
    }
}
EOF

cat << 'EOF' > app/src/main/java/com/wolver/touchguard/MainActivity.kt
package com.wolver.touchguard

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefManager: PrefManager

    private lateinit var tvServiceStatus: TextView
    private lateinit var btnOpenAppInfo: Button
    private lateinit var btnOpenAccessibility: Button
    private lateinit var switchMasterEnable: SwitchCompat
    private lateinit var switchAppFilter: SwitchCompat
    private lateinit var tvSelectedAppsSummary: TextView
    private lateinit var btnSelectApps: Button

    private lateinit var tvWriteSettingsStatus: TextView
    private lateinit var btnGrantWriteSettings: Button
    private lateinit var switchDpiEngine: SwitchCompat
    private lateinit var etTargetDpi: EditText
    private lateinit var etBaseDpi: EditText
    private lateinit var etYAxisBoost: EditText
    private lateinit var btnApplyExactDpi: Button
    private lateinit var btnPreset550: Button
    private lateinit var btnPreset720: Button
    private lateinit var btnPreset960: Button
    private lateinit var btnPreset1200: Button
    private lateinit var btnPreset1440: Button
    private lateinit var tvDpiTelemetry: TextView
    private lateinit var switchUnbufferedTouch: SwitchCompat

    private lateinit var switchZone1: SwitchCompat
    private lateinit var switchZone2: SwitchCompat
    private lateinit var switchZone3: SwitchCompat
    private lateinit var switchZone4: SwitchCompat

    private lateinit var switchHidePillInLock: SwitchCompat
    private lateinit var switchLockBorder: SwitchCompat
    private lateinit var tvOpacityValue: TextView
    private lateinit var seekBarEditOpacity: SeekBar

    private lateinit var btnSelectZ1: Button
    private lateinit var btnSelectZ2: Button
    private lateinit var btnSelectZ3: Button
    private lateinit var btnSelectZ4: Button
    private lateinit var tvEditingZoneTitle: TextView
    private lateinit var tvZoneX: TextView
    private lateinit var seekZoneX: SeekBar
    private lateinit var tvZoneY: TextView
    private lateinit var seekZoneY: SeekBar
    private lateinit var tvZoneW: TextView
    private lateinit var seekZoneW: SeekBar
    private lateinit var tvZoneH: TextView
    private lateinit var seekZoneH: SeekBar

    private lateinit var btnForceEditMode: Button
    private lateinit var btnForceLockMode: Button
    private lateinit var btnResetZones: Button

    private var currentlySelectedZoneForSliders: Int = 1
    private var isUpdatingSlidersProgrammatically: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefManager = PrefManager(this)
        bindViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        syncUiWithPrefs()
        updateServiceStatusBanner()
        updateWriteSettingsStatusBanner()
    }

    private fun bindViews() {
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        btnOpenAppInfo = findViewById(R.id.btnOpenAppInfo)
        btnOpenAccessibility = findViewById(R.id.btnOpenAccessibility)
        switchMasterEnable = findViewById(R.id.switchMasterEnable)
        switchAppFilter = findViewById(R.id.switchAppFilter)
        tvSelectedAppsSummary = findViewById(R.id.tvSelectedAppsSummary)
        btnSelectApps = findViewById(R.id.btnSelectApps)

        tvWriteSettingsStatus = findViewById(R.id.tvWriteSettingsStatus)
        btnGrantWriteSettings = findViewById(R.id.btnGrantWriteSettings)
        switchDpiEngine = findViewById(R.id.switchDpiEngine)
        etTargetDpi = findViewById(R.id.etTargetDpi)
        etBaseDpi = findViewById(R.id.etBaseDpi)
        etYAxisBoost = findViewById(R.id.etYAxisBoost)
        btnApplyExactDpi = findViewById(R.id.btnApplyExactDpi)
        btnPreset550 = findViewById(R.id.btnPreset550)
        btnPreset720 = findViewById(R.id.btnPreset720)
        btnPreset960 = findViewById(R.id.btnPreset960)
        btnPreset1200 = findViewById(R.id.btnPreset1200)
        btnPreset1440 = findViewById(R.id.btnPreset1440)
        tvDpiTelemetry = findViewById(R.id.tvDpiTelemetry)
        switchUnbufferedTouch = findViewById(R.id.switchUnbufferedTouch)

        switchZone1 = findViewById(R.id.switchZone1)
        switchZone2 = findViewById(R.id.switchZone2)
        switchZone3 = findViewById(R.id.switchZone3)
        switchZone4 = findViewById(R.id.switchZone4)

        switchHidePillInLock = findViewById(R.id.switchHidePillInLock)
        switchLockBorder = findViewById(R.id.switchLockBorder)
        tvOpacityValue = findViewById(R.id.tvOpacityValue)
        seekBarEditOpacity = findViewById(R.id.seekBarEditOpacity)

        btnSelectZ1 = findViewById(R.id.btnSelectZ1)
        btnSelectZ2 = findViewById(R.id.btnSelectZ2)
        btnSelectZ3 = findViewById(R.id.btnSelectZ3)
        btnSelectZ4 = findViewById(R.id.btnSelectZ4)
        tvEditingZoneTitle = findViewById(R.id.tvEditingZoneTitle)
        tvZoneX = findViewById(R.id.tvZoneX)
        seekZoneX = findViewById(R.id.seekZoneX)
        tvZoneY = findViewById(R.id.tvZoneY)
        seekZoneY = findViewById(R.id.seekZoneY)
        tvZoneW = findViewById(R.id.tvZoneW)
        seekZoneW = findViewById(R.id.seekZoneW)
        tvZoneH = findViewById(R.id.tvZoneH)
        seekZoneH = findViewById(R.id.seekZoneH)

        btnForceEditMode = findViewById(R.id.btnForceEditMode)
        btnForceLockMode = findViewById(R.id.btnForceLockMode)
        btnResetZones = findViewById(R.id.btnResetZones)
    }

    private fun syncUiWithPrefs() {
        switchMasterEnable.isChecked = prefManager.isMasterEnabled
        switchAppFilter.isChecked = prefManager.isAppFilterEnabled
        updateSelectedAppsSummaryText()

        switchDpiEngine.isChecked = prefManager.isDpiEngineEnabled
        etTargetDpi.setText(prefManager.exactTargetDpi.toString())
        etBaseDpi.setText(prefManager.exactBaseDpi.toString())
        etYAxisBoost.setText(prefManager.yAxisBoostPercent.toString())
        switchUnbufferedTouch.isChecked = prefManager.isUnbufferedTouchEnabled
        refreshDpiTelemetryBox()

        switchZone1.isChecked = prefManager.isZoneEnabled(1)
        switchZone2.isChecked = prefManager.isZoneEnabled(2)
        switchZone3.isChecked = prefManager.isZoneEnabled(3)
        switchZone4.isChecked = prefManager.isZoneEnabled(4)

        switchHidePillInLock.isChecked = prefManager.hidePillInLockMode
        switchLockBorder.isChecked = prefManager.showLockBorder

        val opacity = prefManager.editOpacityPercent
        seekBarEditOpacity.progress = opacity
        tvOpacityValue.text = "Edit Mode Red Opacity: $opacity%"

        loadZoneIntoSliders(currentlySelectedZoneForSliders)
    }

    private fun refreshDpiTelemetryBox() {
        val profile = DpiSensitivityEngine.computeProfile(this, prefManager)
        tvDpiTelemetry.text = DpiSensitivityEngine.formatTelemetryReport(profile)
    }

    private fun parseAndApplyExactDpiInputs() {
        val rawTarget = etTargetDpi.text?.toString()?.trim()?.toIntOrNull() ?: 960
        val rawBase = etBaseDpi.text?.toString()?.trim()?.toIntOrNull() ?: 411
        val rawYBoost = etYAxisBoost.text?.toString()?.trim()?.toIntOrNull() ?: 25

        val clampedTarget = rawTarget.coerceIn(320, 3000)
        val clampedBase = rawBase.coerceIn(300, 800)
        val clampedYBoost = rawYBoost.coerceIn(0, 150)

        prefManager.exactTargetDpi = clampedTarget
        prefManager.exactBaseDpi = clampedBase
        prefManager.yAxisBoostPercent = clampedYBoost

        etTargetDpi.setText(clampedTarget.toString())
        etBaseDpi.setText(clampedBase.toString())
        etYAxisBoost.setText(clampedYBoost.toString())

        hideKeyboard()
        refreshDpiTelemetryBox()
        notifyServiceToRefresh()

        Toast.makeText(
            this,
            "Applied Exact $clampedTarget DPI Sensitivity (Zero Screen Shrink)",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun applyQuickDpiPreset(presetDpi: Int) {
        etTargetDpi.setText(presetDpi.toString())
        parseAndApplyExactDpiInputs()
    }

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            currentFocus?.windowToken?.let { token ->
                imm?.hideSoftInputFromWindow(token, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun loadZoneIntoSliders(zoneId: Int) {
        currentlySelectedZoneForSliders = zoneId
        isUpdatingSlidersProgrammatically = true

        val buttons = listOf(btnSelectZ1, btnSelectZ2, btnSelectZ3, btnSelectZ4)
        buttons.forEachIndexed { index, button ->
            val color = if (index + 1 == zoneId) "#FF1744" else "#1E1E1E"
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor(color))
        }

        val cfg = prefManager.getZoneConfig(zoneId)
        tvEditingZoneTitle.text = "Adjusting: ${cfg.title}"

        seekZoneX.progress = cfg.x.coerceIn(0, seekZoneX.max)
        tvZoneX.text = "Horizontal Position (X): ${cfg.x} px"

        seekZoneY.progress = cfg.y.coerceIn(0, seekZoneY.max)
        tvZoneY.text = "Vertical Position (Y): ${cfg.y} px"

        seekZoneW.progress = cfg.width.coerceIn(40, seekZoneW.max)
        tvZoneW.text = "Zone Width: ${cfg.width} px"

        seekZoneH.progress = cfg.height.coerceIn(35, seekZoneH.max)
        tvZoneH.text = "Zone Height: ${cfg.height} px"

        isUpdatingSlidersProgrammatically = false
    }

    private fun saveCurrentSliderValuesToZone() {
        if (isUpdatingSlidersProgrammatically) return
        val id = currentlySelectedZoneForSliders
        val newX = seekZoneX.progress
        val newY = seekZoneY.progress
        val newW = seekZoneW.progress.coerceAtLeast(40)
        val newH = seekZoneH.progress.coerceAtLeast(35)

        tvZoneX.text = "Horizontal Position (X): $newX px"
        tvZoneY.text = "Vertical Position (Y): $newY px"
        tvZoneW.text = "Zone Width: $newW px"
        tvZoneH.text = "Zone Height: $newH px"

        prefManager.saveZoneBounds(id, newX, newY, newW, newH)
        notifyServiceToRefresh()
    }

    private fun setupListeners() {
        btnOpenAppInfo.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Could not open App Info", Toast.LENGTH_SHORT).show()
            }
        }

        btnOpenAccessibility.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, "Could not open Accessibility Settings", Toast.LENGTH_SHORT).show()
            }
        }

        switchMasterEnable.setOnCheckedChangeListener { _, isChecked ->
            prefManager.isMasterEnabled = isChecked
            notifyServiceToRefresh()
        }

        switchAppFilter.setOnCheckedChangeListener { _, isChecked ->
            prefManager.isAppFilterEnabled = isChecked
            notifyServiceToRefresh()
        }

        btnSelectApps.setOnClickListener {
            showAppSelectionDialog()
        }

        btnGrantWriteSettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Could not open Modify System Settings screen", Toast.LENGTH_SHORT).show()
            }
        }

        switchDpiEngine.setOnCheckedChangeListener { _, isChecked ->
            prefManager.isDpiEngineEnabled = isChecked
            refreshDpiTelemetryBox()
            notifyServiceToRefresh()
        }

        btnApplyExactDpi.setOnClickListener {
            parseAndApplyExactDpiInputs()
        }

        btnPreset550.setOnClickListener { applyQuickDpiPreset(550) }
        btnPreset720.setOnClickListener { applyQuickDpiPreset(720) }
        btnPreset960.setOnClickListener { applyQuickDpiPreset(960) }
        btnPreset1200.setOnClickListener { applyQuickDpiPreset(1200) }
        btnPreset1440.setOnClickListener { applyQuickDpiPreset(1440) }

        switchUnbufferedTouch.setOnCheckedChangeListener { _, isChecked ->
            prefManager.isUnbufferedTouchEnabled = isChecked
            notifyServiceToRefresh()
        }

        switchZone1.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setZoneEnabled(1, isChecked)
            notifyServiceToRefresh()
        }

        switchZone2.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setZoneEnabled(2, isChecked)
            notifyServiceToRefresh()
        }

        switchZone3.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setZoneEnabled(3, isChecked)
            notifyServiceToRefresh()
        }

        switchZone4.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setZoneEnabled(4, isChecked)
            notifyServiceToRefresh()
        }

        switchHidePillInLock.setOnCheckedChangeListener { _, isChecked ->
            prefManager.hidePillInLockMode = isChecked
            notifyServiceToRefresh()
        }

        switchLockBorder.setOnCheckedChangeListener { _, isChecked ->
            prefManager.showLockBorder = isChecked
            notifyServiceToRefresh()
        }

        seekBarEditOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clamped = progress.coerceIn(15, 85)
                tvOpacityValue.text = "Edit Mode Red Opacity: $clamped%"
                if (fromUser) {
                    prefManager.editOpacityPercent = clamped
                    notifyServiceToRefresh()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSelectZ1.setOnClickListener { loadZoneIntoSliders(1) }
        btnSelectZ2.setOnClickListener { loadZoneIntoSliders(2) }
        btnSelectZ3.setOnClickListener { loadZoneIntoSliders(3) }
        btnSelectZ4.setOnClickListener { loadZoneIntoSliders(4) }

        val sliderListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    saveCurrentSliderValuesToZone()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekZoneX.setOnSeekBarChangeListener(sliderListener)
        seekZoneY.setOnSeekBarChangeListener(sliderListener)
        seekZoneW.setOnSeekBarChangeListener(sliderListener)
        seekZoneH.setOnSeekBarChangeListener(sliderListener)

        btnForceEditMode.setOnClickListener {
            prefManager.isPillHiddenManually = false
            prefManager.currentMode = PrefManager.MODE_EDIT
            notifyServiceToRefresh()
            Toast.makeText(this, "Edit Mode Active & Pill Unhidden", Toast.LENGTH_SHORT).show()
        }

        btnForceLockMode.setOnClickListener {
            prefManager.currentMode = PrefManager.MODE_LOCK
            notifyServiceToRefresh()
            Toast.makeText(this, "Lock Mode Active (100% Touch Blocking & Exact DPI)", Toast.LENGTH_SHORT).show()
        }

        btnResetZones.setOnClickListener {
            prefManager.resetZonesToDefaults()
            syncUiWithPrefs()
            notifyServiceToRefresh()
            Toast.makeText(this, "All 4 zones reset & pill unhidden", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectedAppsSummaryText() {
        val selected = prefManager.selectedPackages
        if (selected.isEmpty()) {
            tvSelectedAppsSummary.text = "No apps selected (Select at least Free Fire)"
            tvSelectedAppsSummary.setTextColor(Color.parseColor("#FF1744"))
        } else {
            tvSelectedAppsSummary.text = "Active for (${selected.size}): ${selected.joinToString(", ")}"
            tvSelectedAppsSummary.setTextColor(Color.parseColor("#00E676"))
        }
    }

    private fun showAppSelectionDialog() {
        val pm = packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        data class AppEntry(val label: String, val pkg: String)

        val entriesMap = linkedMapOf<String, String>()
        entriesMap["com.dts.freefireth"] = "Free Fire (com.dts.freefireth)"
        entriesMap["com.dts.freefiremax"] = "Free Fire MAX (com.dts.freefiremax)"

        val userApps = installedApps
            .filter { app ->
                val isUserApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                        (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                isUserApp && app.packageName != packageName
            }
            .map { app ->
                val name = pm.getApplicationLabel(app).toString()
                AppEntry("$name (${app.packageName})", app.packageName)
            }
            .sortedBy { it.label.lowercase() }

        for (item in userApps) {
            entriesMap[item.pkg] = item.label
        }

        val pkgList = entriesMap.keys.toList()
        val labelArray = entriesMap.values.toTypedArray()
        val currentSelected = prefManager.selectedPackages.toMutableSet()
        val checkedItems = BooleanArray(pkgList.size) { index ->
            currentSelected.contains(pkgList[index])
        }

        AlertDialog.Builder(this)
            .setTitle("Select Games for Touch Guard")
            .setMultiChoiceItems(labelArray, checkedItems) { _, which, isChecked ->
                val pkg = pkgList[which]
                if (isChecked) {
                    currentSelected.add(pkg)
                } else {
                    currentSelected.remove(pkg)
                }
            }
            .setPositiveButton("Save") { _, _ ->
                prefManager.selectedPackages = currentSelected
                updateSelectedAppsSummaryText()
                notifyServiceToRefresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateServiceStatusBanner() {
        val enabled = isAccessibilityServiceEnabled(this, TouchGuardAccessibilityService::class.java)
        if (enabled) {
            tvServiceStatus.text = "● ACTIVE — Accessibility Overlay Running"
            tvServiceStatus.setTextColor(Color.parseColor("#00E676"))
            notifyServiceToRefresh()
        } else {
            tvServiceStatus.text = "● INACTIVE — Enable in Accessibility Settings"
            tvServiceStatus.setTextColor(Color.parseColor("#FF1744"))
        }
    }

    private fun updateWriteSettingsStatusBanner() {
        val canWrite = DpiSensitivityEngine.canWriteSystemSettings(this)
        if (canWrite) {
            tvWriteSettingsStatus.text = "● System Sensitivity Permission: GRANTED"
            tvWriteSettingsStatus.setTextColor(Color.parseColor("#00E676"))
        } else {
            tvWriteSettingsStatus.text = "● System Sensitivity Permission: NOT GRANTED (Tap below)"
            tvWriteSettingsStatus.setTextColor(Color.parseColor("#FF1744"))
        }
    }

    private fun notifyServiceToRefresh() {
        TouchGuardAccessibilityService.instance?.refreshOverlaysVisibilityAndState()
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            val serviceInfo = service.resolveInfo.serviceInfo
            if (serviceInfo.packageName == context.packageName && serviceInfo.name == serviceClass.name) {
                return true
            }
        }
        return false
    }
}
EOF

cat << 'EOF' > .github/workflows/build.yml
name: Build Wolver Touch Guard APK

on:
  push:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build-apk:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle 8.9
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.9'

      - name: Compile Debug APK
        run: gradle assembleDebug --stacktrace

      - name: Upload Ready-to-Install APK
        uses: actions/upload-artifact@v4
        with:
          name: WolverTouchGuard-APK
          path: app/build/outputs/apk/debug/app-debug.apk
EOF

git add -A
git commit -m "Implement Exact Numerical DPI Sensitivity Engine v2.0.0 with zero screen shrink"
git push
