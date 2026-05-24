package com.example.data.analyzer

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object FileAnalyzer {
    private const val TAG = "FileAnalyzer"

    data class ScanResult(
        val fileName: String,
        val filePath: String,
        val realFileType: String,
        val declaredExtension: String,
        val hashSha256: String,
        val fileSize: Long,
        val riskLevel: String, // "Safe", "Suspicious", "High Risk", "Unknown"
        val riskReasons: List<String>,
        val packageId: String? = null,
        val permissions: List<String> = emptyList(),
        val extractedUrls: List<String> = emptyList()
    )

    fun scanUri(context: Context, uri: Uri, fileName: String): ScanResult {
        var tempFile: File? = null
        try {
            // 1. Copy the Uri stream to a secure temp file in the app's cache directory.
            // This bypasses low-level storage quirks and allows multiple scans.
            tempFile = File(context.cacheDir, "scan_temp_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw java.io.FileNotFoundException("Could not open input stream")

            val fileSize = tempFile.length()
            val extension = getFileExtension(fileName)

            // 2. Perform SHA-256 computation
            val hash = calculateSha256(tempFile)

            // 3. Detect Real File Type (Magic Bytes)
            val realType = detectRealFileType(tempFile, extension)

            val riskReasons = mutableListOf<String>()
            var riskLevel = "Safe"
            var packageId: String? = null
            val permissions = mutableListOf<String>()
            val extractedUrls = mutableSetOf<String>()

            // 4. Check for critical extension mismatches (Spoofing)
            if (isMismatched(realType, extension)) {
                riskLevel = "High Risk"
                riskReasons.add("Critical Spoofing: True file type is [$realType] but the extension is named [.$extension]")
            }

            // 5. Deep Scan depending on the real content type
            when (realType) {
                "APK" -> {
                    val apkMetadata = scanApkContents(tempFile)
                    packageId = apkMetadata.packageId
                    permissions.addAll(apkMetadata.permissions)
                    extractedUrls.addAll(apkMetadata.urls)
                    riskReasons.addAll(apkMetadata.riskReasons)
                    
                    if (apkMetadata.riskLevel == "High Risk") {
                        riskLevel = "High Risk"
                    } else if (apkMetadata.riskLevel == "Suspicious" && riskLevel != "High Risk") {
                        riskLevel = "Suspicious"
                    }
                }
                "ZIP" -> {
                    val zipMetadata = scanZipContents(tempFile)
                    riskReasons.addAll(zipMetadata.riskReasons)
                    extractedUrls.addAll(zipMetadata.urls)
                    if (zipMetadata.riskLevel == "High Risk") {
                        riskLevel = "High Risk"
                    } else if (zipMetadata.riskLevel == "Suspicious" && riskLevel != "High Risk") {
                        riskLevel = "Suspicious"
                    }
                }
                "PDF" -> {
                    val pdfMetadata = scanTextBasedFile(tempFile, "PDF")
                    riskReasons.addAll(pdfMetadata.riskReasons)
                    extractedUrls.addAll(pdfMetadata.urls)
                    if (pdfMetadata.riskLevel == "High Risk") {
                        riskLevel = "High Risk"
                    } else if (pdfMetadata.riskLevel == "Suspicious" && riskLevel != "High Risk") {
                        riskLevel = "Suspicious"
                    }
                }
                "HTML" -> {
                    val htmlMetadata = scanTextBasedFile(tempFile, "HTML")
                    riskReasons.addAll(htmlMetadata.riskReasons)
                    extractedUrls.addAll(htmlMetadata.urls)
                    if (htmlMetadata.riskLevel == "High Risk") {
                        riskLevel = "High Risk"
                    } else if (htmlMetadata.riskLevel == "Suspicious" && riskLevel != "High Risk") {
                        riskLevel = "Suspicious"
                    }
                }
                "JS" -> {
                    val jsMetadata = scanTextBasedFile(tempFile, "JS")
                    riskReasons.addAll(jsMetadata.riskReasons)
                    extractedUrls.addAll(jsMetadata.urls)
                    if (jsMetadata.riskLevel == "High Risk") {
                        riskLevel = "High Risk"
                    } else if (jsMetadata.riskLevel == "Suspicious" && riskLevel != "High Risk") {
                        riskLevel = "Suspicious"
                    }
                }
                "DOCX" -> {
                    val docxMetadata = scanDocxArchive(tempFile)
                    riskReasons.addAll(docxMetadata.riskReasons)
                    extractedUrls.addAll(docxMetadata.urls)
                    if (docxMetadata.riskLevel == "High Risk") {
                        riskLevel = "High Risk"
                    } else if (docxMetadata.riskLevel == "Suspicious" && riskLevel != "High Risk") {
                        riskLevel = "Suspicious"
                    }
                }
                "TXT" -> {
                    val txtMetadata = scanTextBasedFile(tempFile, "TXT")
                    riskReasons.addAll(txtMetadata.riskReasons)
                    extractedUrls.addAll(txtMetadata.urls)
                    if (txtMetadata.riskLevel == "High Risk") {
                        riskLevel = "High Risk"
                    } else if (txtMetadata.riskLevel == "Suspicious" && riskLevel != "High Risk") {
                        riskLevel = "Suspicious"
                    }
                }
                "PE_BINARY" -> {
                    riskLevel = "High Risk"
                    riskReasons.add("Dangerous executable: Windows PE binary (.exe/.dll) detected on Android.")
                }
                "ELF_BINARY" -> {
                    riskLevel = "Suspicious"
                    riskReasons.add("Native Linux ELF binary file detected. Generally unusual in standalone Downloads.")
                }
                "Unknown" -> {
                    riskLevel = "Unknown"
                    riskReasons.add("Unable to identify the inner structure of this binary. Treating with utmost caution.")
                }
            }

            // Fallback default message if safe
            if (riskReasons.isEmpty()) {
                riskLevel = "Safe"
                riskReasons.add("No immediate indicators of phishing, unrequested scripting, spoofing, or dangerous permission combos identified.")
            }

            return ScanResult(
                fileName = fileName,
                filePath = uri.toString(),
                realFileType = realType,
                declaredExtension = extension,
                hashSha256 = hash,
                fileSize = fileSize,
                riskLevel = riskLevel,
                riskReasons = riskReasons,
                packageId = packageId,
                permissions = permissions,
                extractedUrls = extractedUrls.toList()
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning URI $uri", e)
            return ScanResult(
                fileName = fileName,
                filePath = uri.toString(),
                realFileType = "Unknown",
                declaredExtension = getFileExtension(fileName),
                hashSha256 = "",
                fileSize = 0L,
                riskLevel = "Unknown",
                riskReasons = listOf("Scan failed or file inaccessible: ${e.localizedMessage}")
            )
        } finally {
            // Always diligently clean up cache resources
            tempFile?.delete()
        }
    }

    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        FileInputStream(file).use { input ->
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun detectRealFileType(file: File, extension: String): String {
        val bytes = ByteArray(4)
        try {
            FileInputStream(file).use { input ->
                input.read(bytes)
            }
        } catch (e: Exception) {
            return "Unknown"
        }

        if (bytes.size < 4) return "Unknown"

        // 1. Check ZIP-based structural container (0x50, 0x4B, 0x03, 0x04) -> "PK.."
        if (bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
        ) {
            // Is it APK, DOCX, or just standard ZIP? We check extension to help categorize,
            // but we will verify their inside matches too.
            return when (extension) {
                "apk", "xapk", "apks" -> "APK"
                "docx", "xlsx", "pptx" -> "DOCX"
                else -> {
                    // Let's do a quick peek inside the zip to see if it contains an AndroidManifest.xml
                    if (zipContainsEntry(file, "AndroidManifest.xml")) {
                        "APK"
                    } else if (zipContainsEntry(file, "word/document.xml")) {
                        "DOCX"
                    } else {
                        "ZIP"
                    }
                }
            }
        }

        // 2. Check PDF (0x25, 0x50, 0x44, 0x46) -> "%PDF"
        if (bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x44.toByte() && bytes[3] == 0x46.toByte()
        ) {
            return "PDF"
        }

        // 3. Check Windows PE Binary ("MZ" -> 0x4D, 0x5A)
        if (bytes[0] == 0x4D.toByte() && bytes[1] == 0x5A.toByte()) {
            return "PE_BINARY"
        }

        // 4. Check Linux ELF Binary (0x7F, 0x45, 0x4C, 0x46) -> ".ELF"
        if (bytes[0] == 0x7F.toByte() && bytes[1] == 0x45.toByte() &&
            bytes[2] == 0x4C.toByte() && bytes[3] == 0x46.toByte()
        ) {
            return "ELF_BINARY"
        }

        // Other matches
        return when (extension) {
            "html", "htm" -> "HTML"
            "js" -> "JS"
            "txt", "log", "json" -> "TXT"
            else -> "Unknown"
        }
    }

    private fun isMismatched(realType: String, extension: String): Boolean {
        if (realType == "Unknown") return false
        val cleanExt = extension.lowercase()

        val allowedExtensionsForType = when (realType) {
            "APK" -> listOf("apk", "xapk", "apks", "zip")
            "ZIP" -> listOf("zip", "rar", "tar", "gz", "7z")
            "PDF" -> listOf("pdf")
            "DOCX" -> listOf("docx", "xlsx", "pptx")
            "HTML" -> listOf("html", "htm", "txt")
            "JS" -> listOf("js", "txt")
            "PE_BINARY" -> listOf("exe", "dll", "sys", "scr")
            "ELF_BINARY" -> listOf("so", "bin", "elf")
            "TXT" -> listOf("txt", "log", "json", "csv", "md")
            else -> emptyList()
        }

        return allowedExtensionsForType.isNotEmpty() && cleanExt !in allowedExtensionsForType
    }

    private fun zipContainsEntry(file: File, entrySearch: String): Boolean {
        try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name.contains(entrySearch)) {
                        return true
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            // Ignore zip issues here
        }
        return false
    }

    data class AnalysisMetadata(
        var riskLevel: String = "Safe",
        val riskReasons: MutableList<String> = mutableListOf(),
        val urls: MutableSet<String> = mutableSetOf(),
        val permissions: MutableList<String> = mutableListOf(),
        var packageId: String? = null
    )

    private fun scanApkContents(file: File): AnalysisMetadata {
        val meta = AnalysisMetadata()
        val foundPermissions = mutableSetOf<String>()
        var foundManifest = false

        try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "AndroidManifest.xml") {
                        foundManifest = true
                        val bytes = zip.readBytes()
                        val extractedStrings = extractStringsFromBuffer(bytes)

                        // Parse package ID from string pool
                        val packageCandidate = extractedStrings.firstOrNull { 
                            it.contains('.') && it.length > 5 && !it.startsWith("android") && 
                            it.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+$")) 
                        }
                        meta.packageId = packageCandidate

                        // Extract Permissions
                        for (str in extractedStrings) {
                            if (str.contains("android.permission.")) {
                                foundPermissions.add(str.substringAfter("android.permission."))
                            }
                            // Extract URLs from manifest string pool too
                            val urlMatches = extractUrlsFromString(str)
                            meta.urls.addAll(urlMatches)
                        }
                    } else if (entry.name.endsWith(".dex")) {
                        // Scan DEX index for executable indicators/classes if possible
                        // But usually manifest suffices for metadata. Let's inspect entry for risk too.
                    } else if (entry.name.contains("../")) {
                        meta.riskLevel = "High Risk"
                        meta.riskReasons.add("Directory Traversal Vulnerability (Zip Slip exploit entry: ${entry.name})")
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            meta.riskLevel = "Suspicious"
            meta.riskReasons.add("Failed to inspect entire APK archive layout: ${e.localizedMessage}")
        }

        if (!foundManifest) {
            meta.riskLevel = "Suspicious"
            meta.riskReasons.add("Malformed APK: No AndroidManifest.xml exists.")
        }

        meta.permissions.addAll(foundPermissions)

        // Evaluate dangerous Android permissions
        val hasSmsSend = foundPermissions.contains("SEND_SMS")
        val hasSmsReceive = foundPermissions.contains("RECEIVE_SMS")
        val hasSmsRead = foundPermissions.contains("READ_SMS")
        val hasOverlays = foundPermissions.contains("SYSTEM_ALERT_WINDOW")
        val hasAccessibility = foundPermissions.contains("BIND_ACCESSIBILITY_SERVICE")
        val hasBoot = foundPermissions.contains("RECEIVE_BOOT_COMPLETED")
        val hasInstaller = foundPermissions.contains("REQUEST_INSTALL_PACKAGES") || foundPermissions.contains("INSTALL_PACKAGES")
        val hasAudio = foundPermissions.contains("RECORD_AUDIO")
        val hasCamera = foundPermissions.contains("CAMERA")
        val hasContacts = foundPermissions.contains("READ_CONTACTS")

        // Populate Reasons
        if (hasSmsSend) {
            meta.riskReasons.add("Dangerous Permission [SEND_SMS]: Allows background text dispatch (can invoke billing charges).")
        }
        if (hasSmsReceive) {
            meta.riskReasons.add("Dangerous Permission [RECEIVE_SMS]: Intercepts incoming SMS (commonly targets 2FA/OTPs).")
        }
        if (hasOverlays) {
            meta.riskReasons.add("Dangerous Privilege [SYSTEM_ALERT_WINDOW]: Allows screen drawing overlays (enables banking trojan phishing screens).")
        }
        if (hasAccessibility) {
            meta.riskReasons.add("Dangerous Privilege [BIND_ACCESSIBILITY_SERVICE]: Demands full user UI interaction monitoring. High indicator of accessibility logging/payload control.")
        }
        if (hasInstaller) {
            meta.riskReasons.add("Dangerous Privilege [INSTALL_PACKAGES]: Actively permits silent deployment of other payloads (Dropper capability).")
        }

        // Combinations Risk mapping
        if (hasAccessibility && hasOverlays) {
            meta.riskLevel = "High Risk"
            meta.riskReasons.add("High Risk Combo: Accessibility logging combined with Draw-Overlays (Standard Banking Trojan vector).")
        } else if ((hasSmsSend || hasSmsReceive || hasSmsRead) && hasBoot) {
            meta.riskLevel = "High Risk"
            meta.riskReasons.add("High Risk Combo: Boot startup persistence combined with active SMS Interception.")
        } else if (hasInstaller && hasBoot) {
            meta.riskLevel = "High Risk"
            meta.riskReasons.add("High Risk Combo: Auto-start on boot with App Installation capacity.")
        } else if (hasAccessibility || hasOverlays || hasSmsSend || hasSmsReceive) {
            meta.riskLevel = "Suspicious"
        } else if (meta.permissions.size > 15) {
            meta.riskLevel = "Suspicious"
            meta.riskReasons.add("Oversized permission list: Requests ${meta.permissions.size} permissions (suspicious complexity).")
        }

        return meta
    }

    private fun scanZipContents(file: File): AnalysisMetadata {
        val meta = AnalysisMetadata()
        var apkCount = 0
        var execCount = 0

        try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val nameLower = entry.name.lowercase()
                    if (nameLower.endsWith(".apk")) {
                        apkCount++
                        meta.riskReasons.add("Hidden payload: Nested installer APK found inside zip archive (${entry.name})")
                    }
                    if (nameLower.endsWith(".exe") || nameLower.endsWith(".bat") || nameLower.endsWith(".sh") || nameLower.endsWith(".cmd")) {
                        execCount++
                        meta.riskReasons.add("Executable content: Nested command/executable file found (${entry.name})")
                    }
                    if (entry.name.contains("../")) {
                        meta.riskLevel = "High Risk"
                        meta.riskReasons.add("Zip Slip: Directory traversal exploit payload detected (${entry.name})")
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            meta.riskLevel = "Suspicious"
            meta.riskReasons.add("Corrupted archive or analysis failed: ${e.localizedMessage}")
        }

        if (apkCount > 0 || execCount > 0) {
            meta.riskLevel = "High Risk"
        }

        return meta
    }

    private fun scanDocxArchive(file: File): AnalysisMetadata {
        val meta = AnalysisMetadata()
        var hasMacros = false

        try {
            ZipInputStream(FileInputStream(file)).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName.contains("vbaProject.bin") || entryName.contains("macro")) {
                        hasMacros = true
                        meta.riskReasons.add("VBA Macros detected: Microsoft Word visual basic script bin file found in docx container (${entryName}).")
                    }
                    // Docx contains document.xml, we can quickly scan it for links
                    if (entryName.endsWith(".xml") || entryName.endsWith(".rels")) {
                        val contentString = String(zip.readBytes(), Charsets.UTF_8)
                        meta.urls.addAll(extractUrlsFromString(contentString))
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            // Treat issues gracefully
        }

        if (hasMacros) {
            meta.riskLevel = "High Risk"
        }

        // Apply shared URLs validation
        validateExtractedUrls(meta)

        return meta
    }

    private fun scanTextBasedFile(file: File, fileType: String): AnalysisMetadata {
        val meta = AnalysisMetadata()
        val textBuilder = StringBuilder()

        try {
            FileInputStream(file).use { input ->
                val buffer = ByteArray(16384)
                var bytesRead: Int
                var readTotal = 0L
                // Read up to 2MB to keep scanning efficient and safe
                while (input.read(buffer).also { bytesRead = it } != -1 && readTotal < 2_000_000L) {
                    textBuilder.append(String(buffer, 0, bytesRead, Charsets.UTF_8))
                    readTotal += bytesRead
                }
            }
        } catch (e: Exception) {
            meta.riskLevel = "Suspicious"
            meta.riskReasons.add("Failure reading file stream: ${e.localizedMessage}")
            return meta
        }

        val text = textBuilder.toString()

        // 1. Scan for links
        val urls = extractUrlsFromString(text)
        meta.urls.addAll(urls)

        // 2. Scan specialized headers
        when (fileType) {
            "PDF" -> {
                if (text.contains("/JavaScript") || text.contains("/JS")) {
                    meta.riskLevel = "Suspicious"
                    meta.riskReasons.add("Suspicious PDF payload: Automated JavaScript commands detected inside document stream.")
                }
                if (text.contains("/OpenAction") || text.contains("/AA")) {
                    meta.riskLevel = "Suspicious"
                    meta.riskReasons.add("Suspicious PDF payload: Immediate OpenAction triggers found (executes logic instantly upon opening).")
                }
                if (text.contains("/Launch")) {
                    meta.riskLevel = "High Risk"
                    meta.riskReasons.add("Dangerous Document Instruction: Attempting to launch shell commands / file execution via PDF (/Launch flag).")
                }
            }
            "JS", "HTML" -> {
                val textLower = text.lowercase()
                var scriptIndicators = 0
                if (textLower.contains("eval(") || textLower.contains("eval (")) {
                    scriptIndicators++
                    meta.riskReasons.add("Dynamic code execution: Obfuscation keyword 'eval()' found. Commonly masks malicious script logic.")
                }
                if (textLower.contains("unescape(") || textLower.contains("string.fromcharcode")) {
                    scriptIndicators++
                    meta.riskReasons.add("Obfuscated scripts: Decoding keywords like 'unescape' or 'fromCharCode' are present.")
                }
                if (textLower.contains("atob(") || textLower.contains("btoa(")) {
                    scriptIndicators++
                    meta.riskReasons.add("Base64 encryption filters: Dynamic decoding mechanisms 'atob()' found.")
                }
                if (textLower.contains("document.write(<") || textLower.contains("document.write(\"<")) {
                    scriptIndicators++
                    meta.riskReasons.add("DOM manipulation: Document.write payload generators detected.")
                }

                if (scriptIndicators >= 2) {
                    meta.riskLevel = "High Risk"
                } else if (scriptIndicators == 1) {
                    meta.riskLevel = "Suspicious"
                }
            }
            "TXT" -> {
                // Look for phishing combinations
                val textLower = text.lowercase()
                var phishingFlags = 0
                if (textLower.contains("confirm billing") || textLower.contains("verify login") || textLower.contains("urgently confirm")) {
                    phishingFlags++
                }
                if (textLower.contains("password") || textLower.contains("credential") || textLower.contains("bank PIN") || textLower.contains("secure your account")) {
                    phishingFlags++
                }
                if (phishingFlags >= 2 && urls.isNotEmpty()) {
                    meta.riskLevel = "Suspicious"
                    meta.riskReasons.add("Suspicious wording combinations: Urgency keywords paired with login credentials and links (Phishing-like vector).")
                }
            }
        }

        validateExtractedUrls(meta)

        return meta
    }

    private fun validateExtractedUrls(meta: AnalysisMetadata) {
        for (url in meta.urls) {
            val lowerUrl = url.lowercase()
            // IP address checks in urls of scripts/documents
            val matchResult = Regex("https?://(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})").find(lowerUrl)
            if (matchResult != null) {
                meta.riskLevel = "High Risk"
                meta.riskReasons.add("Raw IP Command Connection: Direct numerical IP address URL found ($url). Bypasses standard DNS names (high threat indicator).")
            }
            // Link shortener checkers
            val shorteners = listOf("bit.ly", "tinyurl.com", "goo.gl", "t.co", "ow.ly", "is.gd", "buff.ly", "adf.ly")
            for (short in shorteners) {
                if (lowerUrl.contains(short)) {
                    if (meta.riskLevel != "High Risk") {
                        meta.riskLevel = "Suspicious"
                    }
                    meta.riskReasons.add("Obscured link: Employs a URL redirection shortener service ($short).")
                }
            }
        }
    }

    private fun extractUrlsFromString(input: String): List<String> {
        val urls = mutableListOf<String>()
        // Match URLs starting with http:// or https://
        val pattern = Regex("https?://[a-zA-Z0-9./_?=-]+", RegexOption.IGNORE_CASE)
        val matches = pattern.findAll(input)
        for (m in matches) {
            val rawUrl = m.value
            // Clean url of wrapping quotes or tags if parsed from xml or scripts
            val cleanUrl = rawUrl.trim('\"', '\'', '>', '<', ')')
            if (cleanUrl.length in 8..150) {
                urls.add(cleanUrl)
            }
        }
        return urls.distinct()
    }

    private fun extractStringsFromBuffer(bytes: ByteArray): Set<String> {
        val foundStrings = mutableSetOf<String>()

        // 1. Scan UTF-8 / Raw ASCII sequences
        var currentString = StringBuilder()
        for (i in bytes.indices) {
            val b = bytes[i].toInt() and 0xFF
            if (b in 32..126) {
                currentString.append(b.toChar())
            } else {
                if (currentString.length >= 4) {
                    foundStrings.add(currentString.toString())
                }
                currentString = StringBuilder()
            }
        }
        if (currentString.length >= 4) {
            foundStrings.add(currentString.toString())
        }

        // 2. Scan UTF-16LE / UTF-16BE sequences
        currentString = StringBuilder()
        var i = 0
        while (i < bytes.size - 1) {
            val b1 = bytes[i].toInt() and 0xFF
            val b2 = bytes[i + 1].toInt() and 0xFF
            if (b1 in 32..126 && b2 == 0) {
                currentString.append(b1.toChar())
                i += 2
            } else if (b1 == 0 && b2 in 32..126) {
                currentString.append(b2.toChar())
                i += 2
            } else {
                if (currentString.length >= 4) {
                    foundStrings.add(currentString.toString())
                }
                currentString = StringBuilder()
                i++
            }
        }
        if (currentString.length >= 4) {
            foundStrings.add(currentString.toString())
        }

        return foundStrings
    }
}
