package ai.rojan.designlab.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Minimal, dependency-free architecture guard (Phase 1 audit). Plain
 * [File] + regex source scan over import lines — no ArchUnit/Konsist, per
 * the "no unnecessary dependencies" constraint. Gradle unit tests run
 * with the module directory (`app/`) as the working directory, so
 * `src/main/java/...` resolves without any extra path plumbing.
 */
class ArchitectureRulesTest {

    private val mainSourceRoot = File("src/main/java/ai/rojan/designlab")

    private fun ktFiles(subPath: String): List<File> {
        val root = File(mainSourceRoot, subPath)
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun matchingImportLines(file: File, regex: Regex): List<String> =
        file.readLines().filter { regex.containsMatchIn(it) }

    @Test
    fun `production repositories do not depend on demo data sources`() {
        val demoImport = Regex("""^import ai\.rojan\.designlab\.data\.demo""")
        val violations = ktFiles("data/repository").flatMap { file ->
            matchingImportLines(file, demoImport).map { "${file.path}: $it" }
        }
        assertTrue(
            "data/repository/*.kt must not import data.demo:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun `domain layer has zero Android framework imports`() {
        val androidImport = Regex("""^import (android|androidx)\.""")
        val violations = ktFiles("domain").flatMap { file ->
            matchingImportLines(file, androidImport).map { "${file.path}: $it" }
        }
        assertTrue(
            "domain/ must not import android.*/androidx.*:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    @Test
    fun `domain layer does not depend on outer layers except the tracked demo allowlist`() {
        val outerLayerImport = Regex("""^import ai\.rojan\.designlab\.(data|presentation|screens|manager|ui|di)\.""")
        val demoImport = Regex("""^import ai\.rojan\.designlab\.data\.demo""")

        val violations = ktFiles("domain").flatMap { file ->
            val outerImports = matchingImportLines(file, outerLayerImport)
            if (outerImports.isEmpty()) return@flatMap emptyList()

            val relativePath = file.relativeTo(mainSourceRoot).path.replace('\\', '/').removePrefix("domain/")
            val (demoImports, nonDemoImports) = outerImports.partition { demoImport.containsMatchIn(it) }

            val bannedLayerViolations = nonDemoImports.map {
                "${file.path}: $it (domain may not import data/presentation/screens/manager/ui/di at all)"
            }
            val untrackedDemoViolations = if (demoImports.isNotEmpty() && relativePath !in KNOWN_DOMAIN_DEMO_VIOLATIONS) {
                demoImports.map {
                    "${file.path}: $it (data.demo import not covered by KNOWN_DOMAIN_DEMO_VIOLATIONS allowlist)"
                }
            } else {
                emptyList()
            }
            bannedLayerViolations + untrackedDemoViolations
        }

        assertTrue(
            "domain/ must not depend on outer layers, except the pre-existing, explicitly tracked " +
                "data.demo violations below:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    private companion object {

        /**
         * Pre-existing domain -> data.demo violations, confirmed by direct
         * inspection during the Phase 1 audit. This is a ratchet, not a
         * pass: it must never grow. Prune an entry in the same commit that
         * fixes its file — a stale, un-pruned entry is a code-review signal,
         * not something this test re-verifies.
         *
         * Emptied (Task 7): every file that was on this list —
         * `BookingEngine.kt` (its demo-dependent methods removed, kept for
         * its still-live step/intent resolution), `BookingAvailabilityRules.kt`,
         * `CatalogEngine.kt`, `CustomerEcosystemEngine.kt`,
         * `CustomerEcosystemState.kt`, `EcosystemEvent(Reducer).kt`,
         * `insights/ProfileInsights(Engine).kt`,
         * `rules/CouponEligibilityEngine.kt`,
         * `rules/PreferredEntityRuleProvider.kt`,
         * `identity/mapping/SalonIdentityMapper.kt`, `waitlist/WaitlistEngine.kt`
         * — either had its `data.demo` dependency removed or was deleted
         * outright once verified to have zero references outside this
         * cluster (along with the rest of `data/demo`).
         */
        val KNOWN_DOMAIN_DEMO_VIOLATIONS = emptySet<String>()
    }
}
