package com.example.expensetrackerpro.resources

import com.example.expensetrackerpro.domain.models.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Keeps every translation complete: each values-<language>/strings.xml must have the same
 * strings as values/strings.xml, with the same format arguments. Reads the files themselves,
 * so a missing translation fails here before it reaches a device.
 */
class StringResourcesTest {

    /** One string, or one quantity of a plural, by a key such as `transaction_count[one]`. */
    private data class Strings(val texts: Map<String, String>, val pluralQuantities: Map<String, Set<String>>)

    private val resDir = File("src/main/res")
    private val english = parse(File(resDir, "values/strings.xml"))
    private val translations: Map<String, Strings> = resDir.listFiles().orEmpty()
        .filter { it.isDirectory && it.name.startsWith("values-") && File(it, "strings.xml").exists() }
        .associate { it.name.removePrefix("values-") to parse(File(it, "strings.xml")) }

    private fun parse(file: File): Strings {
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val texts = mutableMapOf<String, String>()
        val quantities = mutableMapOf<String, Set<String>>()
        val nodes = root.childNodes

        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            // The same in every language, so it must not be translated
            if (element.getAttribute("translatable") == "false") continue
            val name = element.getAttribute("name")

            when (element.tagName) {
                "string" -> texts[name] = element.textContent
                "plurals" -> {
                    val items = element.getElementsByTagName("item")
                    val byQuantity = (0 until items.length).map { items.item(it) as Element }
                        .associate { it.getAttribute("quantity") to it.textContent }
                    quantities[name] = byQuantity.keys
                    byQuantity.forEach { (quantity, text) -> texts["$name[$quantity]"] = text }
                }
            }
        }
        return Strings(texts, quantities)
    }

    // %1$s, %2$d …; a literal %% is not an argument
    private fun placeholders(text: String): List<String> =
        PLACEHOLDER.findAll(text.replace("%%", "")).map { it.value }.sorted().toList()

    /** A plural's key without its quantity, so every quantity is compared with the same English text. */
    private fun String.baseKey(): String = substringBefore('[')

    @Test
    fun theTestFindsTheFiles() {
        assertTrue("values/strings.xml should have strings", english.texts.size > 100)
        assertEquals(setOf("hi", "es", "fr", "de", "pt-rBR"), translations.keys)
    }

    @Test
    fun everyTranslation_hasExactlyTheStringsEnglishHas() {
        val expected = english.texts.keys.map { it.baseKey() }.toSet()

        translations.forEach { (language, strings) ->
            val actual = strings.texts.keys.map { it.baseKey() }.toSet()

            assertEquals("Missing from values-$language", emptySet<String>(), expected - actual)
            assertEquals("Only in values-$language (deleted, or not translatable?)", emptySet<String>(), actual - expected)
        }
    }

    @Test
    fun everyTranslation_keepsTheFormatArgumentsOfEnglish() {
        // Every quantity of an English plural takes the same arguments
        val expected = english.texts.entries.groupBy({ it.key.baseKey() }, { placeholders(it.value) })
        expected.forEach { (key, variants) ->
            assertEquals("$key differs between its English quantities", 1, variants.toSet().size)
        }

        translations.forEach { (language, strings) ->
            strings.texts.forEach { (key, text) ->
                assertEquals(
                    "values-$language: $key has other format arguments than English",
                    expected[key.baseKey()]?.first(),
                    placeholders(text)
                )
            }
        }
    }

    @Test
    fun noTranslationIsEmpty() {
        translations.forEach { (language, strings) ->
            strings.texts.forEach { (key, text) ->
                assertTrue("values-$language: $key is blank", text.isNotBlank())
            }
        }
    }

    @Test
    fun everyPlural_hasTheQuantitiesItsLanguageUses() {
        assertEquals(REQUIRED_QUANTITIES.keys, translations.keys + "en")

        (translations + ("en" to english)).forEach { (language, strings) ->
            strings.pluralQuantities.forEach { (name, quantities) ->
                assertEquals("values-$language: quantities of $name", REQUIRED_QUANTITIES.getValue(language), quantities)
            }
        }
    }

    @Test
    fun builtInCategoryNames_areUniqueWithinEachLanguage() {
        (translations + ("en" to english)).forEach { (language, strings) ->
            val names = strings.texts.filterKeys { it.startsWith("category_default_") }.values.map { it.trim().lowercase() }

            assertEquals(25, names.size)
            assertEquals("values-$language: two built-in categories share a name", names.size, names.toSet().size)
        }
    }

    @Test
    fun everyLanguageOfTheApp_hasItsStrings_andIsOfferedToAndroid() {
        // values-pt-rBR is the folder of the tag pt-BR
        val folders = AppLanguage.entries.filter { it != AppLanguage.ENGLISH }.map { it.tag.replace("-", "-r") }.toSet()
        assertEquals(folders, translations.keys)

        val config = File(resDir, "xml/locales_config.xml").readText()
        val offered = Regex("android:name=\"([^\"]+)\"").findAll(config).map { it.groupValues[1] }.toList()
        assertEquals(AppLanguage.entries.map { it.tag }, offered)
    }

    private companion object {
        val PLACEHOLDER = Regex("%\\d+\\$[a-zA-Z]")

        // The plural categories of each language (CLDR). A new language is added here with its own
        val REQUIRED_QUANTITIES = mapOf(
            "en" to setOf("one", "other"),
            // "one" covers 0 and 1 in Hindi
            "hi" to setOf("one", "other"),
            "de" to setOf("one", "other"),
            // "many" is for whole millions ("1 000 000 de transactions")
            "es" to setOf("one", "many", "other"),
            "fr" to setOf("one", "many", "other"),
            "pt-rBR" to setOf("one", "many", "other")
        )
    }
}
