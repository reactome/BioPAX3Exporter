package org.reactome.server.tools

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.*
import org.reactome.server.graph.domain.model.ReactionLikeEvent
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify

class BioPAXInteractionBuilderTest {
    
    @Test
    fun `test addReactomeRLEvent with null event returns null`() {
        // Create a mock Model
        val mockModel = mock(Model::class.java)
        
        // Create the builder with null event
        val builder = BioPAXInteractionBuilder(null, mockModel)
        
        // Call the method
        val result = builder.addReactomeRLEvent()
        
        // Verify that no BiochemicalReaction was created
        verify(mockModel, never()).addNew(BiochemicalReaction::class.java, any())
        
        // Verify that the result is null
        assertNull(result)
    }
    
    @Test
    fun `test addReactomeRLEvent with null model returns null`() {
        // Create a mock ReactionLikeEvent
        val mockRLEvent = mock(ReactionLikeEvent::class.java)
        
        // Create the builder with null model
        val builder = BioPAXInteractionBuilder(mockRLEvent, null)
        
        // Call the method
        val result = builder.addReactomeRLEvent()
        
        // Verify that the result is null
        assertNull(result)
    }
    
    @Test
    fun `test addReactomeRLEvent creates BiochemicalReaction with correct properties`() {
        // Create mocks
        val mockModel = mock(Model::class.java)
        val mockRLEvent = mock(ReactionLikeEvent::class.java)
        val mockBPReaction = mock(BiochemicalReaction::class.java)
        
        // Set up the mocks
        whenever(mockRLEvent.dbId).thenReturn(123L)
        whenever(mockRLEvent.displayName).thenReturn("Test Reaction")
        
        // Set up the model to return the mock BiochemicalReaction
        whenever(mockModel.addNew(BiochemicalReaction::class.java, any())).thenReturn(mockBPReaction)
        
        // Create the builder
        val builder = BioPAXInteractionBuilder(mockRLEvent, mockModel)
        
        // Call the method
        val result = builder.addReactomeRLEvent()
        
        // Verify that the BiochemicalReaction was created with the correct properties
        verify(mockModel).addNew(BiochemicalReaction::class.java, any())
        verify(mockBPReaction).displayName = "Test Reaction"
        
        // Verify that the result is the mock BiochemicalReaction
        assertEquals(mockBPReaction, result)
    }
    
    @Test
    fun `test addReactomeRLEvent reuses existing BiochemicalReaction`() {
        // Create mocks
        val mockModel = mock(Model::class.java)
        val mockRLEvent = mock(ReactionLikeEvent::class.java)
        val mockBPReaction = mock(BiochemicalReaction::class.java)
        
        // Set up the mocks
        whenever(mockRLEvent.dbId).thenReturn(123L)
        whenever(mockRLEvent.displayName).thenReturn("Test Reaction")
        
        // Set up the model to return the mock BiochemicalReaction
        whenever(mockModel.addNew(BiochemicalReaction::class.java, any())).thenReturn(mockBPReaction)
        
        // Create the builder
        val builder = BioPAXInteractionBuilder(mockRLEvent, mockModel)
        
        // Call the method twice
        val result1 = builder.addReactomeRLEvent()
        val result2 = builder.addReactomeRLEvent()
        
        // Verify that the BiochemicalReaction was created only once
        verify(mockModel).addNew(BiochemicalReaction::class.java, any())
        
        // Verify that both results are the same
        assertEquals(result1, result2)
    }
} 