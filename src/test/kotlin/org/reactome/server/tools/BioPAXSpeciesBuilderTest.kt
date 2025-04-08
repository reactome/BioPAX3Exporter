package org.reactome.server.tools

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.*
import org.reactome.server.graph.domain.model.Species
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify

class BioPAXSpeciesBuilderTest {
    
    @Test
    fun `test addReactomeSpecies with null species does nothing`() {
        // Create a mock Model
        val mockModel = mock(Model::class.java)
        
        // Create the builder with null species
        val builder = BioPAXSpeciesBuilder(null, mockModel)
        
        // Call the method
        builder.addReactomeSpecies()
        
        // Verify that no BioSource was created
        verify(mockModel, never()).addNew(BioSource::class.java, any())
    }
    
    @Test
    fun `test addReactomeSpecies with null model does nothing`() {
        // Create a mock Species
        val mockSpecies = mock(Species::class.java)
        
        // Create the builder with null model
        val builder = BioPAXSpeciesBuilder(mockSpecies, null)
        
        // Call the method
        builder.addReactomeSpecies()
        
        // No verification needed as the method will return early
    }
    
    @Test
    fun `test addReactomeSpecies creates BioSource with correct properties`() {
        // Create mocks
        val mockModel = mock(Model::class.java)
        val mockSpecies = mock(Species::class.java)
        val mockBioSource = mock(BioSource::class.java)
        
        // Set up the mocks
        whenever(mockSpecies.getDisplayName()).thenReturn("Test Species")
        whenever(mockSpecies.getDbId()).thenReturn(123L)
        
        // Set up the model to return the mock BioSource
        whenever(mockModel.addNew(BioSource::class.java, any())).thenReturn(mockBioSource)
        
        // Create the builder
        val builder = BioPAXSpeciesBuilder(mockSpecies, mockModel)
        
        // Call the method
        builder.addReactomeSpecies()
        
        // Verify that the BioSource was created with the correct properties
        verify(mockModel).addNew(BioSource::class.java, any())
        verify(mockBioSource).name = any()
    }
} 