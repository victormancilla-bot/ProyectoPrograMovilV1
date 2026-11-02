package bo.edu.ucb.encuentra.feature.claims

import bo.edu.ucb.encuentra.core.data.local.seed.SeedData
import bo.edu.ucb.encuentra.feature.claims.presentation.screen.comparisonLines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClaimComparisonTest {

    private fun report(id: String) = SeedData.lostReports.first { it.id.value == id }
    private fun item(id: String) = SeedData.foundItems.first { it.id.value == id }

    @Test
    fun calculatorReportMatchesTheFoundCalculator() {
        val lines = comparisonLines(report("lr-calculadora"), item("fi-calculadora")).associateBy { it.label }

        assertEquals(listOf(true, true, true, true, true), listOf("Tipo", "Marca", "Color", "Lugar", "Fecha").map { lines.getValue(it).matches })
        // El staff ve el detalle exacto y la descripción privada (el estudiante no, M05).
        assertEquals("Bloque C · Aula 201", lines.getValue("Lugar").found)
        assertEquals("Calculadora Casio fx-991 con un nombre escrito en la tapa.", lines.getValue("Descripción").found)
        assertNull(lines.getValue("Descripción").matches)
    }

    @Test
    fun differencesAreMarked() {
        val lines = comparisonLines(report("lr-calculadora"), item("fi-llavero")).associateBy { it.label }

        assertEquals(false, lines.getValue("Tipo").matches)
        assertEquals(false, lines.getValue("Color").matches)
        // El llavero no tiene marca: no se puede comparar.
        assertNull(lines.getValue("Marca").matches)
        // Encontrado en agosto, antes de la pérdida de octubre.
        assertEquals(false, lines.getValue("Fecha").matches)
    }
}
