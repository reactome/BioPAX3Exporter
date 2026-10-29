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
import org.reactome.server.graph.domain.model.Pathway
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

        assertTrue(written(pathway).contains(">One.&#13;Two.</bp:comment>"))
    }

    @Test
    fun `a file that cannot be written is an error, not a message`(
        @TempDir dir: File,
    ) {
        val pathway = mock(Pathway::class.java).also { whenever(it.dbId).thenReturn(123L) }
        val writer = WriteBioPAX3(pathway, 98).apply { createModel() }

        assertThrows(java.io.IOException::class.java) { writer.toFile(File(dir, "missing/out.owl")) }
    }
}
