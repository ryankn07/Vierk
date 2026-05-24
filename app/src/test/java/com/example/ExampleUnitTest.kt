package com.example

import com.example.data.analyzer.FileAnalyzer
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExampleUnitTest {
  @Test
  fun `detect real pdf by magic bytes`() {
    val file = createTempFile("sample", ".pdf")
    file.writeText("%PDF-1.7\nbody")

    assertEquals("PDF", detectRealFileType(file, "pdf"))
    file.delete()
  }

  @Test
  fun `detect pe binary even with text extension`() {
    val file = createTempFile("payload", ".txt")
    file.writeBytes(byteArrayOf(0x4D, 0x5A, 0x00, 0x00))

    assertEquals("PE_BINARY", detectRealFileType(file, "txt"))
    assertTrue(isMismatched("PE_BINARY", "txt"))
    file.delete()
  }

  @Test
  fun `extract urls from suspicious text`() {
    val urls = extractUrlsFromString("open https://192.168.1.10/dropper and https://example.com/path")

    assertEquals(listOf("https://192.168.1.10/dropper", "https://example.com/path"), urls)
  }

  private fun detectRealFileType(file: File, extension: String): String {
    val method = FileAnalyzer::class.java.getDeclaredMethod(
      "detectRealFileType",
      File::class.java,
      String::class.java
    )
    method.isAccessible = true
    return method.invoke(FileAnalyzer, file, extension) as String
  }

  private fun isMismatched(realType: String, extension: String): Boolean {
    val method = FileAnalyzer::class.java.getDeclaredMethod(
      "isMismatched",
      String::class.java,
      String::class.java
    )
    method.isAccessible = true
    return method.invoke(FileAnalyzer, realType, extension) as Boolean
  }

  @Suppress("UNCHECKED_CAST")
  private fun extractUrlsFromString(input: String): List<String> {
    val method = FileAnalyzer::class.java.getDeclaredMethod("extractUrlsFromString", String::class.java)
    method.isAccessible = true
    return method.invoke(FileAnalyzer, input) as List<String>
  }
}
