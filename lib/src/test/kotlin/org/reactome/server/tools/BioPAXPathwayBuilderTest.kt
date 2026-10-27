package org.reactome.server.tools

import org.biopax.paxtools.model.Model
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.biopax.paxtools.model.level3.Pathway as BPPathway

class BioPAXPathwayBuilderTest {
    @Test
    fun `test addReactomePathway with null pathway returns null`() {
        // Create mocks
        val mockModel = mock(Model::class.java)

        // Create the builder with null pathway
        val builder = BioPAXPathwayBuilder(null, ConversionContext("", mockModel))

        // Call the method
        builder.addReactomePathway()

        // Verify that no Pathway was created
        verify(mockModel, never()).addNew(eq(BPPathway::class.java), any())
    }
}
