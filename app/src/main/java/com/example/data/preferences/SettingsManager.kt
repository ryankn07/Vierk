package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("download_guard_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_SCAN_DOWNLOADS_ONLY = "scan_downloads_only"
        private const val KEY_GRANTED_FOLDER_URI = "granted_folder_uri"
        private const val KEY_GRANTED_FOLDER_NAME = "granted_folder_name"

        private const val KEY_TYPE_APK = "type_apk"
        private const val KEY_TYPE_ZIP = "type_zip"
        private const val KEY_TYPE_PDF = "type_pdf"
        private const val KEY_TYPE_DOC = "type_doc"
        private const val KEY_TYPE_HTML_JS = "type_html_js"
        private const val KEY_TYPE_TXT = "type_txt"
    }

    var isMonitoringEnabled: Boolean
        get() = prefs.getBoolean(KEY_MONITORING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MONITORING_ENABLED, value).apply()

    var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    val scanDownloadsOnly: Boolean
        get() = prefs.getBoolean(KEY_SCAN_DOWNLOADS_ONLY, true) // Always locked to true as part of hard constraints

    var grantedFolderUri: String?
        get() = prefs.getString(KEY_GRANTED_FOLDER_URI, null)
        set(value) = prefs.edit().putString(KEY_GRANTED_FOLDER_URI, value).apply()

    var grantedFolderName: String?
        get() = prefs.getString(KEY_GRANTED_FOLDER_NAME, null)
        set(value) = prefs.edit().putString(KEY_GRANTED_FOLDER_NAME, value).apply()

    // File type filters
    var scanApk: Boolean
        get() = prefs.getBoolean(KEY_TYPE_APK, true)
        set(value) = prefs.edit().putBoolean(KEY_TYPE_APK, value).apply()

    var scanZip: Boolean
        get() = prefs.getBoolean(KEY_TYPE_ZIP, true)
        set(value) = prefs.edit().putBoolean(KEY_TYPE_ZIP, value).apply()

    var scanPdf: Boolean
        get() = prefs.getBoolean(KEY_TYPE_PDF, true)
        set(value) = prefs.edit().putBoolean(KEY_TYPE_PDF, value).apply()

    var scanDoc: Boolean
        get() = prefs.getBoolean(KEY_TYPE_DOC, true)
        set(value) = prefs.edit().putBoolean(KEY_TYPE_DOC, value).apply()

    var scanHtmlJs: Boolean
        get() = prefs.getBoolean(KEY_TYPE_HTML_JS, true)
        set(value) = prefs.edit().putBoolean(KEY_TYPE_HTML_JS, value).apply()

    var scanTxt: Boolean
        get() = prefs.getBoolean(KEY_TYPE_TXT, true)
        set(value) = prefs.edit().putBoolean(KEY_TYPE_TXT, value).apply()
}
