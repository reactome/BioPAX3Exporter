package org.reactome.server.tools

import org.biopax.paxtools.model.BioPAXLevel
import org.biopax.paxtools.model.level3.Protein
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BioPAX3UtilsTest {
    // A real element, as the model makes them: Paxtools' interfaces are not
    // for implementing by hand.
    private fun protein(id: String): Protein = BioPAXLevel.L3.defaultFactory.create(Protein::class.java, id)

    // The names of an entity or an event as the release writes them (T011, T012).
    @Test
    fun `addNames takes the first of the shortest as the display name, and the rest as names`() {
        val bp = protein("x#p")
        // Two of the shortest length: the first is taken, as the release's strict < takes it
        BioPAX3Utils.addNames(listOf("ATP hydrolysis", "ATPase", "ADPase"), "the graph's display name", bp)
        assertEquals("ATPase", bp.displayName)
        // The rest are names; Paxtools keeps the display name among them, and its writer leaves it out
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
