package org.reactome.server.tools

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.biopax.paxtools.model.Model
import org.reactome.server.graph.domain.model.Pathway
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.io.File

class WriteBioPAX3Test {
    
    @Test
    fun `test default constructor initializes correctly`() {
        // Create the WriteBioPAX3 instance with default constructor
        val writer = WriteBioPAX3()
        
        // Verify that the xmlBase is set correctly
        assertEquals("http://www.reactome.org/biopax/#", WriteBioPAX3.xmlBase)
        
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
        assertEquals("http://www.reactome.org/biopax/0/123#", WriteBioPAX3.xmlBase)
        
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
        assertEquals(42, WriteBioPAX3.dbVersion)
        
        // Verify that the xmlBase is set correctly
        assertEquals("http://www.reactome.org/biopax/42/123#", WriteBioPAX3.xmlBase)
        
        // Verify that the model is null initially
        assertNull(writer.getModel())
    }
    
    @Test
    fun `test setDBVersion updates dbVersion`() {
        // Create the WriteBioPAX3 instance
        val writer = WriteBioPAX3()
        
        // Set the dbVersion
        writer.setDBVersion(42)
        
        // Verify that the dbVersion is set correctly
        assertEquals(42, WriteBioPAX3.dbVersion)
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
    fun `test toFile writes model to file`(@TempDir tempDir: File) {
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
} 