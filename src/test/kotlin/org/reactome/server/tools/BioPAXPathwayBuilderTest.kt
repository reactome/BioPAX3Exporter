package org.reactome.server.tools

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.*
// Both packages have a Pathway: the graph's is Pathway, BioPAX's BPPathway.
import org.biopax.paxtools.model.level3.Pathway as BPPathway
import org.reactome.server.graph.domain.model.Pathway
import org.reactome.server.graph.domain.model.*
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify

class BioPAXPathwayBuilderTest {
    
    @Test
    fun `test addReactomePathway with null pathway returns null`() {
        // Create mocks
        val mockModel = mock(Model::class.java)
        
        // Create the builder with null pathway
        val builder = BioPAXPathwayBuilder(null, mockModel)
        
        // Call the method
        builder.addReactomePathway()
        
        // Verify that no Pathway was created
        verify(mockModel, never()).addNew(eq(BPPathway::class.java), any())
    }
    
} 