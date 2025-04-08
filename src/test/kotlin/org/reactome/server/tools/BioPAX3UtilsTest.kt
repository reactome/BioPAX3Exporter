package org.reactome.server.tools

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class BioPAX3UtilsTest {
    
    @Test
    fun `test getTypeCount returns correct ID format`() {
        // Test first ID for a new type
        val firstId = BioPAX3Utils.getTypeCount("TestType")
        assertEquals("TestType1", firstId)
        
        // Test second ID for the same type
        val secondId = BioPAX3Utils.getTypeCount("TestType")
        assertEquals("TestType2", secondId)
        
        // Test ID for a different type
        val differentTypeId = BioPAX3Utils.getTypeCount("AnotherType")
        assertEquals("AnotherType1", differentTypeId)
    }
    
    @Test
    fun `test getID returns correct format`() {
        val id = BioPAX3Utils.getID("TestName")
        assertTrue(id.startsWith("http://www.reactome.org/biopax3/"))
        assertTrue(id.endsWith("TestName"))
    }
    
    @Test
    fun `test getTypeCount maintains separate counters for different types`() {
        // Reset counters by creating new instances
        val pathwayId1 = BioPAX3Utils.getTypeCount("Pathway")
        val publicationId1 = BioPAX3Utils.getTypeCount("Publication")
        val pathwayId2 = BioPAX3Utils.getTypeCount("Pathway")
        val publicationId2 = BioPAX3Utils.getTypeCount("Publication")
        
        assertEquals("Pathway1", pathwayId1)
        assertEquals("Publication1", publicationId1)
        assertEquals("Pathway2", pathwayId2)
        assertEquals("Publication2", publicationId2)
    }
} 