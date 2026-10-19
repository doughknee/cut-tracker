package com.relentnet.cuttracker

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every activity, receiver and service the manifest names must exist as a class. A widget whose receiver class is
 * gone compiles fine and spins forever on the home screen; this is the test that would have caught it.
 */
class ManifestTest {
    @Test fun manifestComponentsExist() {
        val manifest = listOf("app/src/main/AndroidManifest.xml", "src/main/AndroidManifest.xml").map(::File).first { it.exists() }.readText()
        val pkg = "com.relentnet.cuttracker"
        val names = Regex("""<(?:activity|receiver|service|provider)[^>]*android:name="([^"]+)"""").findAll(manifest).map { it.groupValues[1] }.toList()
        assertTrue(names.size >= 4, "found $names")
        val missing = names.map { if (it.startsWith(".")) pkg + it else it }.filter { runCatching { Class.forName(it) }.isFailure }
        assertTrue(missing.isEmpty(), "manifest names classes that do not exist: $missing")
    }
}
