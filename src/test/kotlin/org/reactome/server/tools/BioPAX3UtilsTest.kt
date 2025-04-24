package org.reactome.server.tools

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.biopax.paxtools.model.BioPAXElement
import org.biopax.paxtools.model.level3.Named
import java.util.*

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
    
    @Test
    fun `test clearCounterArray resets all counters`() {
        // Add some types to the counter
        BioPAX3Utils.getTypeCount("TypeA")
        BioPAX3Utils.getTypeCount("TypeB")
        
        // Clear the counter array
        BioPAX3Utils.clearCounterArray()
        
        // Get a new ID for TypeA, should start from 1 again
        val newId = BioPAX3Utils.getTypeCount("TypeA")
        assertEquals("TypeA1", newId)
    }
    
    @Test
    fun `test getObjectFromSet returns correct object`() {
        // Create a mock BioPAXElement
        val mockElement = object : BioPAXElement {
            override var rdfId: String = BioPAX3Utils.getID("TestId")
        }
        
        val set = setOf(mockElement)
        
        // Test finding the object
        val found = BioPAX3Utils.getObjectFromSet(set, "TestId")
        assertNotNull(found)
        assertEquals(mockElement, found)
        
        // Test with non-existent ID
        val notFound = BioPAX3Utils.getObjectFromSet(set, "NonExistentId")
        assertNull(notFound)
        
        // Test with null set
        val nullSetResult = BioPAX3Utils.getObjectFromSet<BioPAXElement>(null, "TestId")
        assertNull(nullSetResult)
        
        // Test with null ID
        val nullIdResult = BioPAX3Utils.getObjectFromSet(set, null)
        assertNull(nullIdResult)
    }
    
    @Test
    fun `test getObjectFromSetByName returns correct object`() {
        // Create a mock Named object
        val mockNamed = object : Named {
            override var name: Set<String> = TreeSet<String>().apply { add("TestName") }
            override var rdfId: String = BioPAX3Utils.getID("TestId")
        }
        
        val set = setOf(mockNamed)
        
        // Test finding the object
        val found = BioPAX3Utils.getObjectFromSetByName(set, "TestName")
        assertNotNull(found)
        assertEquals(mockNamed, found)
        
        // Test with non-existent name
        val notFound = BioPAX3Utils.getObjectFromSetByName(set, "NonExistentName")
        assertNull(notFound)
        
        // Test with null set
        val nullSetResult = BioPAX3Utils.getObjectFromSetByName<Named>(null, "TestName")
        assertNull(nullSetResult)
        
        // Test with null name
        val nullNameResult = BioPAX3Utils.getObjectFromSetByName(set, null)
        assertNull(nullNameResult)
    }
    
    @Test
    fun `test contains returns correct boolean`() {
        // Create a mock BioPAXElement
        val mockElement = object : BioPAXElement {
            override var rdfId: String = BioPAX3Utils.getID("TestId")
        }
        
        val set = setOf(mockElement)
        
        // Test with existing ID
        assertTrue(BioPAX3Utils.contains(set, "TestId"))
        
        // Test with non-existent ID
        assertFalse(BioPAX3Utils.contains(set, "NonExistentId"))
        
        // Test with null set
        assertFalse(BioPAX3Utils.contains<BioPAXElement>(null, "TestId"))
        
        // Test with null ID
        assertFalse(BioPAX3Utils.contains(set, null))
    }
    
    @Test
    fun `test containsName returns correct boolean`() {
        // Create a mock Named object
        val mockNamed = object : Named {
            override var name: Set<String> = TreeSet<String>().apply { add("TestName") }
            override var rdfId: String = BioPAX3Utils.getID("TestId")
        }
        
        val set = setOf(mockNamed)
        
        // Test with existing name
        assertTrue(BioPAX3Utils.containsName(set, "TestName"))
        
        // Test with non-existent name
        assertFalse(BioPAX3Utils.containsName(set, "NonExistentName"))
        
        // Test with null set
        assertFalse(BioPAX3Utils.containsName<Named>(null, "TestName"))
        
        // Test with null name
        assertFalse(BioPAX3Utils.containsName(set, null))
    }
} 