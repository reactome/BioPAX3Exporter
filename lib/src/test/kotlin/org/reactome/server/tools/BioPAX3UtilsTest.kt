package org.reactome.server.tools

import org.biopax.paxtools.model.BioPAXElement
import org.biopax.paxtools.model.BioPAXLevel
import org.biopax.paxtools.model.level3.Named
import org.biopax.paxtools.model.level3.Protein
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BioPAX3UtilsTest {
    // A real element, as the model makes them: Paxtools' interfaces are not
    // for implementing by hand.
    private fun protein(id: String): Protein = BioPAXLevel.L3.defaultFactory.create(Protein::class.java, id)

    @Test
    fun `test getObjectFromSet returns correct object`() {
        // Create a mock BioPAXElement
        val mockElement: BioPAXElement = protein("TestId")

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
        val mockNamed: Named = protein("TestId").apply { addName("TestName") }

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
        val mockElement: BioPAXElement = protein("TestId")

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
        val mockNamed: Named = protein("TestId").apply { addName("TestName") }

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

    // The names of an entity or an event as the release writes them (T011, T012).
    @Test
    fun `addNames takes the first of the shortest as the display name, and the rest as names`() {
        val bp = protein("x#p")
        // Two of the shortest length: the first is taken, as the release's strict < takes it
        BioPAX3Utils.addNames(listOf("ATP hydrolysis", "ATPase", "ADPase"), "the graph's display name", bp)
        assertEquals("ATPase", bp.displayName)
        // The display name is not written again as a name
        assertEquals(setOf("ATP hydrolysis", "ADPase"), bp.name - bp.displayName)
    }

    @Test
    fun `addNames uses the display name when there is no name`() {
        val bp = protein("x#p")
        BioPAX3Utils.addNames(null, "RBX1 [cytosol]", bp)
        assertEquals("RBX1 [cytosol]", bp.displayName)
        assertEquals(setOf("RBX1 [cytosol]"), bp.name)
    }
}
