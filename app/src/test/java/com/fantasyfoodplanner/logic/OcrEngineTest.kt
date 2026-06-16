package com.fantasyfoodplanner.logic

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit-Tests für OcrEngine V3 – Professioneller Scanner.
 * Testet: Standard-Etiketten, ML Kit blockweises Format, OCR-Fehler,
 * Fuzzy Matching, FrameAccumulator, selektive has-Flags.
 */
class OcrEngineTest {

    // ═══════════════════════════════════════════
    // STANDARD ETIKETTEN
    // ═══════════════════════════════════════════

    @Test
    fun `Müsli - Standard einzeilig kJ kcal`() {
        val text = """
            Nährwertangaben pro 100 g
            Brennwert 1.738 kJ / 413 kcal
            Fett 12,0 g
            davon gesättigte Fettsäuren 4,8 g
            Kohlenhydrate 62,0 g
            davon Zucker 22,0 g
            Eiweiß 10,0 g
            Salz 0,10 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(413.0, r.kcal100, 2.0)
        assertEquals(10.0, r.protein100, 0.5)
        assertEquals(62.0, r.carbs100, 0.5)
        assertEquals(12.0, r.fat100, 0.5)
        assertTrue(r.hasKcal); assertTrue(r.hasProtein); assertTrue(r.hasCarbs); assertTrue(r.hasFat)
        assertTrue(r.isPlausible)
    }

    @Test
    fun `Joghurt - kompakt kJ slash kcal`() {
        val text = """
            Durchschnittliche Nährwerte pro 100 g
            Energie 266 kJ/64 kcal
            Fett 3,5 g
            davon gesättigte Fettsäuren 2,3 g
            Kohlenhydrate 4,6 g
            davon Zucker 4,6 g
            Eiweiß 3,4 g
            Salz 0,13 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(64.0, r.kcal100, 2.0)
        assertEquals(3.4, r.protein100, 0.5)
        assertEquals(4.6, r.carbs100, 0.5)
        assertEquals(3.5, r.fat100, 0.5)
    }

    @Test
    fun `Milch - Klammer-Format kJ`() {
        val text = """
            Brennwert 275 kJ (66 kcal)
            Fett 3,8 g
            davon gesättigte Fettsäuren 2,5 g
            Kohlenhydrate 4,8 g
            Eiweiß 3,3 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(66.0, r.kcal100, 2.0)
        assertEquals(3.8, r.fat100, 0.5)
        assertEquals(4.8, r.carbs100, 0.5)
        assertEquals(3.3, r.protein100, 0.5)
    }

    // ═══════════════════════════════════════════
    // ML KIT BLOCKWEISES FORMAT (DAS KERNPROBLEM)
    // ═══════════════════════════════════════════

    @Test
    fun `ML Kit blockweise - alle Werte getrennt`() {
        val text = """
            Brennwert
            1.738 kJ / 413 kcal
            Fett
            12,0 g
            davon gesättigte Fettsäuren
            4,8 g
            Kohlenhydrate
            62,0 g
            davon Zucker
            22,0 g
            Eiweiß
            10,0 g
            Salz
            0,10 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(413.0, r.kcal100, 5.0)
        assertEquals(12.0, r.fat100, 0.5)
        assertEquals(62.0, r.carbs100, 0.5)
        assertEquals(10.0, r.protein100, 0.5)
        assertTrue(r.isPlausible)
    }

    @Test
    fun `ML Kit blockweise - teilweise getrennt`() {
        val text = """
            Brennwert 413 kcal
            Fett
            15,0 g
            Kohlenhydrate
            40,0 g
            Eiweiß 12,0 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(413.0, r.kcal100, 1.0)
        assertEquals(15.0, r.fat100, 0.5)
        assertEquals(40.0, r.carbs100, 0.5)
        assertEquals(12.0, r.protein100, 0.5)
    }

    @Test
    fun `ML Kit blockweise - Protein und Carbs getrennt`() {
        val text = """
            Energie 250 kcal
            Fett 10,0 g
            Kohlenhydrate
            30,0 g
            Protein
            8,0 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(250.0, r.kcal100, 1.0)
        assertEquals(10.0, r.fat100, 0.5)
        assertEquals(30.0, r.carbs100, 0.5)
        assertEquals(8.0, r.protein100, 0.5)
    }

    @Test
    fun `ML Kit kJ und kcal auf 2 Zeilen`() {
        val text = """
            Brennwert  2.186kJ
            522kcal
            Fett 30,0g
            Kohlenhydrate 52,0g
            Eiweiss 6,5g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(522.0, r.kcal100, 5.0)
        assertEquals(30.0, r.fat100, 0.5)
        assertEquals(52.0, r.carbs100, 0.5)
        assertEquals(6.5, r.protein100, 0.5)
    }

    // ═══════════════════════════════════════════
    // ZWEI-SPALTEN FORMAT
    // ═══════════════════════════════════════════

    @Test
    fun `Chips - zweizeilig kJ kcal mit Spalten`() {
        val text = """
            Nährwerte je 100g   je Portion (30g)
            Brennwert  2.186kJ   656kJ
                       522kcal   157kcal
            Fett       30,0g     9,0g
            dav. ges. Fettsäuren 2,9g 0,9g
            Kohlenhydrate  52,0g  15,6g
            dav. Zucker     2,5g   0,8g
            Eiweiss         6,5g   2,0g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(522.0, r.kcal100, 5.0)
        assertEquals(52.0, r.carbs100, 0.5)
        assertEquals(30.0, r.fat100, 0.5)
        assertEquals(6.5, r.protein100, 0.5)
    }

    @Test
    fun `Nutella - zweizeilig mit Portion (15g)`() {
        val text = """
            Nährwertangaben    pro 100 g   pro Portion (15 g)
            Brennwert          2.252 kJ    338 kJ
                               539 kcal    81 kcal
            Fett               30,9 g      4,6 g
            davon gesättigte Fettsäuren 10,6 g 1,6 g
            Kohlenhydrate      57,5 g      8,6 g
            davon Zucker       56,3 g      8,4 g
            Eiweiß              6,3 g      0,9 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(539.0, r.kcal100, 5.0)
        assertEquals(57.5, r.carbs100, 0.5)
        assertEquals(30.9, r.fat100, 0.5)
        assertEquals(6.3, r.protein100, 0.5)
    }

    // ═══════════════════════════════════════════
    // OCR-FEHLER + FUZZY MATCHING
    // ═══════════════════════════════════════════

    @Test
    fun `OCR-Fehler - EiweiB statt Eiweiß`() {
        val text = """
            Brennwert 358 kcal
            Fett 15,0 g
            Kohlenhydrate 40,0 g
            EiweiB 12,0 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(12.0, r.protein100, 0.5)
        assertTrue(r.hasProtein)
    }

    @Test
    fun `OCR-Fehler - Eiwei6 statt Eiweiß`() {
        val text = """
            Brennwert 250 kcal
            Fett 10,0 g
            Kohlenhydrate 30,0 g
            Eiwei6 8,0 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(8.0, r.protein100, 0.5)
    }

    @Test
    fun `OCR-Fehler - Kohienhydrate (fuzzy)`() {
        val text = """
            Brennwert 420 kcal
            Fett 20,0 g
            Kohienhydrate 45,0 g
            Eiweiß 15,0 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(45.0, r.carbs100, 0.5)
    }

    @Test
    fun `Abgekürzte Keywords - Kohlenh`() {
        val text = """
            Brennwert 420 kcal
            Fett 20,0 g
            Kohlenh. 45,0 g
            Eiweiss 15,0 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(45.0, r.carbs100, 0.5)
        assertEquals(15.0, r.protein100, 0.5)
    }

    @Test
    fun `Punktlinien zwischen Label und Wert`() {
        val text = """
            Brennwert . . . . . . 350 kcal
            Fett . . . . . . . . . 18,0 g
            Kohlenhydrate . . . . 35,0 g
            Eiweiß . . . . . . .  12,0 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(350.0, r.kcal100, 1.0)
        assertEquals(18.0, r.fat100, 0.5)
        assertEquals(35.0, r.carbs100, 0.5)
        assertEquals(12.0, r.protein100, 0.5)
    }

    // ═══════════════════════════════════════════
    // HAS-FLAGS (Selektives Erkennen)
    // ═══════════════════════════════════════════

    @Test
    fun `Nur kcal und Fett erkannt - hasCarbs und hasProtein sind false`() {
        val text = """
            Brennwert 250 kcal
            Fett 10,0 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertTrue(r.hasKcal)
        assertTrue(r.hasFat)
        assertFalse(r.hasCarbs)
        assertFalse(r.hasProtein)
    }

    // ═══════════════════════════════════════════
    // FRAME ACCUMULATOR
    // ═══════════════════════════════════════════

    @Test
    fun `FrameAccumulator - Median über 3 Frames`() {
        val acc = OcrEngine.FrameAccumulator()

        acc.addFrame(OcrEngine.ScanResult(kcal100 = 250.0, protein100 = 10.0, carbs100 = 30.0, fat100 = 8.0,
            hasKcal = true, hasProtein = true, hasCarbs = true, hasFat = true))
        acc.addFrame(OcrEngine.ScanResult(kcal100 = 260.0, protein100 = 10.5, carbs100 = 31.0, fat100 = 8.5,
            hasKcal = true, hasProtein = true, hasCarbs = true, hasFat = true))
        acc.addFrame(OcrEngine.ScanResult(kcal100 = 255.0, protein100 = 10.0, carbs100 = 30.0, fat100 = 8.0,
            hasKcal = true, hasProtein = true, hasCarbs = true, hasFat = true))

        val stable = acc.getStableResult()!!
        assertEquals(255.0, stable.kcal100, 5.0)
        assertTrue(acc.isStable())
    }

    @Test
    fun `FrameAccumulator - instabil bei großer Schwankung`() {
        val acc = OcrEngine.FrameAccumulator()

        acc.addFrame(OcrEngine.ScanResult(kcal100 = 250.0, hasKcal = true))
        acc.addFrame(OcrEngine.ScanResult(kcal100 = 400.0, hasKcal = true))
        acc.addFrame(OcrEngine.ScanResult(kcal100 = 100.0, hasKcal = true))

        assertFalse(acc.isStable())
    }

    // ═══════════════════════════════════════════
    // EDGE CASES
    // ═══════════════════════════════════════════

    @Test
    fun `Nur kJ ohne kcal - muss umrechnen`() {
        val text = """
            Energie 1046 kJ
            Fett 8,0 g
            Kohlenhydrate 30,0 g
            Protein 5,0 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(250.0, r.kcal100, 5.0)
    }

    @Test
    fun `Leerer Text gibt null`() {
        assertNull(OcrEngine.parseLiveText(""))
        assertNull(OcrEngine.parseLiveText("   "))
    }

    @Test
    fun `Nur 1 Wert gibt null`() {
        assertNull(OcrEngine.parseLiveText("Brennwert 350 kcal"))
    }

    @Test
    fun `Sub-Zeilen werden korrekt übersprungen`() {
        val text = """
            Brennwert 350 kcal
            Fett 15,0 g
            davon gesättigte Fettsäuren 5,0 g
            Kohlenhydrate 40,0 g
            davon Zucker 20,0 g
            Eiweiß 10,0 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(15.0, r.fat100, 0.5)
        assertEquals(40.0, r.carbs100, 0.5)
    }

    @Test
    fun `Regex-Fallback findet Werte im Fließtext`() {
        // Wenn zeilen-basiert nichts findet, soll Regex greifen
        val text = "Energie 350 kcal Fett 15g Kohlenhydrate 40g Protein 12g"

        val r = OcrEngine.parseLiveText(text)
        assertNotNull(r)
        assertEquals(350.0, r!!.kcal100, 5.0)
    }

    @Test
    fun `Hoher Fettgehalt Öl - kcal nahe 900`() {
        val text = """
            Brennwert 3700 kJ / 884 kcal
            Fett 99,9 g
            davon gesättigte Fettsäuren 14,0 g
            Kohlenhydrate 0 g
            Eiweiß 0 g
        """.trimIndent()

        val r = OcrEngine.parseLiveText(text)!!
        assertEquals(884.0, r.kcal100, 5.0)
        assertEquals(99.9, r.fat100, 0.5)
    }
}
