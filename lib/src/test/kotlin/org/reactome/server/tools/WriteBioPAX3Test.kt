package org.reactome.server.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.reactome.server.graph.domain.model.BlackBoxEvent
import org.reactome.server.graph.domain.model.Event
import org.reactome.server.graph.domain.model.Pathway
import org.reactome.server.graph.domain.model.Reaction
import org.reactome.server.graph.domain.model.SimpleEntity
import org.reactome.server.graph.domain.model.Species
import org.reactome.server.graph.domain.model.Summation
import java.io.File

class WriteBioPAX3Test {
    // The bases are the release's (T018): http://www.reactome.org/biopax/{release}/{dbId}#,
    // without the release where none is given.

    @Test
    fun `test default constructor initializes correctly`() {
        // Create the WriteBioPAX3 instance with default constructor
        val writer = WriteBioPAX3()

        // Verify that the xmlBase is set correctly
        assertEquals("http://www.reactome.org/biopax#", writer.xmlBase)

        // Verify that the model is null initially
        assertNull(writer.getModel())
    }

    @Test
    fun `test pathway constructor initializes correctly`() {
        // Create a mock Pathway
        val mockPathway = mock(Pathway::class.java)
        whenever(mockPathway.dbId).thenReturn(123L)

        // Create the WriteBioPAX3 instance with pathway constructor
        val writer = WriteBioPAX3(mockPathway)

        // Verify that the xmlBase is set correctly
        assertEquals("http://www.reactome.org/biopax/123#", writer.xmlBase)

        // Verify that the model is null initially
        assertNull(writer.getModel())
    }

    @Test
    fun `test pathway and version constructor initializes correctly`() {
        // Create a mock Pathway
        val mockPathway = mock(Pathway::class.java)
        whenever(mockPathway.dbId).thenReturn(123L)

        // Create the WriteBioPAX3 instance with pathway and version constructor
        val writer = WriteBioPAX3(mockPathway, 42)

        // Verify that the dbVersion is set correctly
        assertEquals(42, writer.dbVersion)

        // Verify that the xmlBase is set correctly
        assertEquals("http://www.reactome.org/biopax/42/123#", writer.xmlBase)

        // Verify that the model is null initially
        assertNull(writer.getModel())
    }

    @Test
    fun `test createModel creates a model`() {
        // Create a mock Pathway
        val mockPathway = mock(Pathway::class.java)
        whenever(mockPathway.dbId).thenReturn(123L)

        // Create the WriteBioPAX3 instance
        val writer = WriteBioPAX3(mockPathway)

        // Create the model
        writer.createModel()

        // Verify that the model is created
        assertNotNull(writer.getModel())
    }

    @Test
    fun `test toFile writes model to file`(
        @TempDir tempDir: File,
    ) {
        // Create a mock Pathway
        val mockPathway = mock(Pathway::class.java)
        whenever(mockPathway.dbId).thenReturn(123L)

        // Create the WriteBioPAX3 instance
        val writer = WriteBioPAX3(mockPathway)

        // Create the model
        writer.createModel()

        // Create a temporary file
        val outputFile = File(tempDir, "test.owl")

        // Write the model to the file
        writer.toFile(outputFile)

        // Verify that the file exists and has content
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)
    }

    @Test
    fun `test toString returns string representation of model`() {
        // Create a mock Pathway
        val mockPathway = mock(Pathway::class.java)
        whenever(mockPathway.dbId).thenReturn(123L)

        // Create the WriteBioPAX3 instance
        val writer = WriteBioPAX3(mockPathway)

        // Create the model
        writer.createModel()

        // Get the string representation
        val result = writer.toString()

        // Verify that the result is not empty
        assertTrue(result.isNotEmpty())
        assertFalse(result.equals("failed to write"))
    }

    @Test
    fun `a character outside ASCII is written as itself, as the published files write it`(
        @TempDir dir: File,
    ) {
        // A name as release 98 has it, with U+2212 MINUS SIGN
        val pathway = mock(Pathway::class.java)
        whenever(pathway.dbId).thenReturn(123L)
        whenever(pathway.displayName).thenReturn("erythro-4-hydroxy-L-glutamate(1\u2212)")
        val writer = WriteBioPAX3(pathway)
        writer.createModel()
        val file = File(dir, "out.owl")

        writer.toFile(file)

        val written = file.readText(Charsets.UTF_8)
        assertTrue(written.contains("glutamate(1\u2212)"))
        assertFalse(written.contains("&#8722;"))
    }

    @Test
    fun `a species' file has the species' base, as the published files do`() {
        val species = mock(Species::class.java).also { whenever(it.dbId).thenReturn(170928) }

        assertEquals("http://www.reactome.org/biopax/98/170928#", WriteBioPAX3(species, 98).xmlBase)
    }

    private fun written(pathway: Pathway): String =
        WriteBioPAX3(pathway, 98).run {
            createModel()
            toString()
        }

    @Test
    fun `literals are written trimmed, a display name and a comment alike`() {
        val summations = listOf(mock(Summation::class.java).also { whenever(it.text).thenReturn("  About it.\n") })
        val pathway =
            mock(Pathway::class.java).also {
                whenever(it.dbId).thenReturn(123L)
                whenever(it.displayName).thenReturn("Platelet degranulation ")
                whenever(it.summation).thenReturn(summations)
            }

        val owl = written(pathway)

        assertTrue(owl.contains(">Platelet degranulation</bp:displayName>"))
        assertTrue(owl.contains(">About it.</bp:comment>"))
        assertFalse(owl.contains("degranulation </"))
    }

    @Test
    fun `a carriage return is kept, as a character reference`() {
        val summations = listOf(mock(Summation::class.java).also { whenever(it.text).thenReturn("One.\rTwo.") })
        val pathway =
            mock(Pathway::class.java).also {
                whenever(it.dbId).thenReturn(123L)
                whenever(it.displayName).thenReturn("pathway")
                whenever(it.summation).thenReturn(summations)
            }

        assertTrue(written(pathway).contains(">One.&#xD;Two.</bp:comment>"))
    }

    @Test
    fun `a file that cannot be written is an error, not a message`(
        @TempDir dir: File,
    ) {
        val pathway = mock(Pathway::class.java).also { whenever(it.dbId).thenReturn(123L) }
        val writer = WriteBioPAX3(pathway, 98).apply { createModel() }

        assertThrows(java.io.IOException::class.java) { writer.toFile(File(dir, "missing/out.owl")) }
    }

    @Test
    fun `only what Java's trim takes off is trimmed, a no-break space kept`() {
        val pathway =
            mock(Pathway::class.java).also {
                whenever(it.dbId).thenReturn(123L)
                whenever(it.displayName).thenReturn("Kept\u00a0\t")
            }

        assertTrue(written(pathway).contains(">Kept\u00a0</bp:displayName>"))
    }

    @Test
    fun `a file that cannot be put in place leaves nothing behind`(
        @TempDir dir: File,
    ) {
        val pathway = mock(Pathway::class.java).also { whenever(it.dbId).thenReturn(123L) }
        // A directory, not empty, where the file should go: the move cannot replace it
        val taken = File(dir, "out.owl").apply { mkdir() }
        File(taken, "inside").writeText("x")
        val writer = WriteBioPAX3(pathway, 98).apply { createModel() }

        assertThrows(java.io.IOException::class.java) { writer.toFile(taken) }
        assertEquals(listOf("out.owl"), dir.list()!!.toList())
    }

    private fun entity(dbId: Long): SimpleEntity =
        mock(SimpleEntity::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn("entity $dbId")
        }

    private fun classesOf(event: Event): Set<String> =
        WriteBioPAX3(event, 98).run {
            createModel()
            getModel()!!.objects.map { it.modelInterface.simpleName }.toSet()
        }

    @Test
    fun `one reaction, of each kind, is converted alone, with its own base (T019)`() {
        val reaction = mock(Reaction::class.java)
        whenever(reaction.dbId).thenReturn(5696356L)
        whenever(reaction.displayName).thenReturn("a reaction")
        val inputs = listOf(entity(1))
        whenever(reaction.input).thenReturn(inputs)
        val degradation = mock(BlackBoxEvent::class.java)
        whenever(degradation.dbId).thenReturn(174731L)
        whenever(degradation.displayName).thenReturn("Degradation of something")
        val degraded = listOf(entity(2))
        whenever(degradation.input).thenReturn(degraded)
        whenever(degradation.output).thenReturn(emptyList())
        val template = mock(BlackBoxEvent::class.java)
        whenever(template.dbId).thenReturn(6790038L)
        whenever(template.displayName).thenReturn("gene expression")
        whenever(template.input).thenReturn(emptyList())
        val made = listOf(entity(3))
        whenever(template.output).thenReturn(made)

        assertTrue("BiochemicalReaction" in classesOf(reaction))
        assertTrue("Degradation" in classesOf(degradation))
        assertTrue("TemplateReaction" in classesOf(template))
        // No pathway, and so no step, for a reaction alone
        assertFalse("Pathway" in classesOf(reaction) || "PathwayStep" in classesOf(reaction))
        assertEquals("http://www.reactome.org/biopax/98/5696356#", WriteBioPAX3(reaction, 98).xmlBase)
    }

    @Test
    fun `events listed together are one model, its base naming no release, its xrefs the release`() {
        val first = mock(Reaction::class.java)
        whenever(first.dbId).thenReturn(1L)
        whenever(first.displayName).thenReturn("first")
        val second = mock(Reaction::class.java)
        whenever(second.dbId).thenReturn(2L)
        whenever(second.displayName).thenReturn("second")

        val writer = WriteBioPAX3(98).apply { createModelForEvents(listOf(first, second)) }

        assertEquals("http://www.reactome.org/biopax#", writer.xmlBase)
        val owl = writer.toString()
        assertTrue(owl.contains(">first</bp:displayName>") && owl.contains(">second</bp:displayName>"))
        assertTrue(owl.contains(">Reactome Database ID Release 98</bp:db>"))
    }

    @Test
    fun `a writer for no event has nothing to convert alone`() {
        assertThrows(IllegalStateException::class.java) { WriteBioPAX3(98).createModel() }
    }

    @Test
    fun `a file written again replaces the one there`(
        @TempDir dir: File,
    ) {
        val out = File(dir, "out.owl").apply { writeText("an older file") }
        val pathway = mock(Pathway::class.java).also { whenever(it.dbId).thenReturn(123L) }

        WriteBioPAX3(pathway, 98).apply { createModel() }.toFile(out)

        assertTrue(out.readText().contains("xml:base=\"http://www.reactome.org/biopax/98/123#\""))
        assertEquals(listOf("out.owl"), dir.list()!!.toList())
    }

    @Test
    fun `standard output ends the model with a newline and stays open after it`() {
        val pathway = mock(Pathway::class.java).also { whenever(it.dbId).thenReturn(123L) }
        val captured = java.io.ByteArrayOutputStream()
        val before = System.out
        val out = java.io.PrintStream(captured, true, "UTF-8")
        System.setOut(out)
        try {
            WriteBioPAX3(pathway, 98).apply { createModel() }.toStdOut()
            println("after")
        } finally {
            System.setOut(before)
        }

        assertFalse(out.checkError())
        assertTrue(captured.toString(Charsets.UTF_8).replace("\r\n", "\n").endsWith("</rdf:RDF>\nafter\n"))
    }

    @Test
    fun `carriage returns within a value are escaped, at its ends trimmed`() {
        val summations =
            listOf(
                mock(org.reactome.server.graph.domain.model.Summation::class.java).also {
                    whenever(it.text).thenReturn("\rA\r\rB\r")
                },
            )
        val pathway =
            mock(Pathway::class.java).also {
                whenever(it.dbId).thenReturn(123L)
                whenever(it.displayName).thenReturn("pathway")
                whenever(it.summation).thenReturn(summations)
            }

        val owl = WriteBioPAX3(pathway, 98).apply { createModel() }.toString()

        // Trimmed at the ends, as Java's trim takes a carriage return; escaped within
        assertTrue(owl.contains(">A&#xD;&#xD;B</bp:comment>"))
        assertFalse(owl.contains("\r"))
    }

    @Test
    fun `the escaping stream escapes a carriage return written alone or within part of an array`() {
        val out = java.io.ByteArrayOutputStream()
        val escaping = WriteBioPAX3.CarriageReturnEscaping(out)
        val bytes = "xx\rA\r\rByy".toByteArray()

        escaping.write('\r'.code)
        escaping.write('C'.code)
        // From offset 2 for all but the last two bytes: "\rA\r\rB"
        escaping.write(bytes, 2, bytes.size - 4)
        escaping.flush()

        assertEquals("&#xD;C&#xD;A&#xD;&#xD;B", out.toString(Charsets.US_ASCII))
    }
}
