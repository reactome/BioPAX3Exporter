package org.reactome.server.tools

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.*
import org.reactome.server.graph.domain.model.*
import org.mockito.Mockito.*
import org.mockito.kotlin.any
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
        verify(mockModel, never()).addNew(Pathway::class.java, any())
    }
    
    @Test
    fun `test addReactomePathway with null model returns null`() {
        // Create a mock Pathway
        val mockPathway = mock(Pathway::class.java)
        
        // Create the builder with null model
        val builder = BioPAXPathwayBuilder(mockPathway, null)
        
        // Call the method
        builder.addReactomePathway()
        
        // No verification needed as the method will return early
    }
    
    @Test
    fun `test addReactomePathway creates Pathway with correct properties`() {
        // Create mocks
        val mockModel = mock(Model::class.java)
        val mockPathway = mock(Pathway::class.java)
        val mockBPPathway = mock(org.biopax.paxtools.model.level3.Pathway::class.java)
        val mockSummation = mock(Summation::class.java)
        
        // Set up the mocks
        whenever(mockPathway.dbId).thenReturn(123L)
        whenever(mockPathway.displayName).thenReturn("Test Pathway")
        whenever(mockPathway.summation).thenReturn(listOf(mockSummation))
        whenever(mockSummation.text).thenReturn("Test description")
        whenever(mockPathway.hasEvent).thenReturn(emptyList())
        
        // Set up the model to return the mock BioPAX Pathway
        whenever(mockModel.addNew(Pathway::class.java, any())).thenReturn(mockBPPathway)
        
        // Create the builder
        val builder = BioPAXPathwayBuilder(mockPathway, mockModel)
        
        // Call the method
        builder.addReactomePathway()
        
        // Verify that the BioPAX Pathway was created with the correct properties
        verify(mockModel).addNew(Pathway::class.java, any())
        verify(mockBPPathway).displayName = "Test Pathway"
        verify(mockBPPathway).addComment(any())
    }
    
    @Test
    fun `test addReactomePathway handles child pathways`() {
        // Create mocks
        val mockModel = mock(Model::class.java)
        val mockPathway = mock(Pathway::class.java)
        val mockChildPathway = mock(Pathway::class.java)
        val mockBPPathway = mock(org.biopax.paxtools.model.level3.Pathway::class.java)
        val mockBPChildPathway = mock(org.biopax.paxtools.model.level3.Pathway::class.java)
        val mockPathwayStep = mock(PathwayStep::class.java)
        
        // Set up the mocks
        whenever(mockPathway.dbId).thenReturn(123L)
        whenever(mockPathway.displayName).thenReturn("Parent Pathway")
        whenever(mockPathway.summation).thenReturn(emptyList())
        whenever(mockPathway.hasEvent).thenReturn(listOf(mockChildPathway))
        
        whenever(mockChildPathway.dbId).thenReturn(456L)
        whenever(mockChildPathway.displayName).thenReturn("Child Pathway")
        whenever(mockChildPathway.summation).thenReturn(emptyList())
        whenever(mockChildPathway.hasEvent).thenReturn(emptyList())
        
        // Set up the model to return the mock BioPAX Pathways and PathwayStep
        whenever(mockModel.addNew(Pathway::class.java, any())).thenReturn(mockBPPathway, mockBPChildPathway)
        whenever(mockModel.addNew(PathwayStep::class.java, any())).thenReturn(mockPathwayStep)
        
        // Create the builder
        val builder = BioPAXPathwayBuilder(mockPathway, mockModel)
        
        // Call the method
        builder.addReactomePathway()
        
        // Verify that the child pathway was processed
        verify(mockModel).addNew(Pathway::class.java, any())
        verify(mockBPPathway).addPathwayOrder(mockPathwayStep)
        verify(mockBPPathway).addPathwayComponent(mockBPChildPathway)
        verify(mockPathwayStep).addStepProcess(mockBPChildPathway)
    }
    
    @Test
    fun `test addReactomePathway handles reaction-like events`() {
        // Create mocks
        val mockModel = mock(Model::class.java)
        val mockPathway = mock(Pathway::class.java)
        val mockRLEvent = mock(ReactionLikeEvent::class.java)
        val mockBPPathway = mock(org.biopax.paxtools.model.level3.Pathway::class.java)
        val mockBPReaction = mock(BiochemicalReaction::class.java)
        val mockPathwayStep = mock(PathwayStep::class.java)
        
        // Set up the mocks
        whenever(mockPathway.dbId).thenReturn(123L)
        whenever(mockPathway.displayName).thenReturn("Test Pathway")
        whenever(mockPathway.summation).thenReturn(emptyList())
        whenever(mockPathway.hasEvent).thenReturn(listOf(mockRLEvent))
        
        whenever(mockRLEvent.dbId).thenReturn(456L)
        whenever(mockRLEvent.displayName).thenReturn("Test Reaction")
        
        // Set up the model to return the mock BioPAX Pathway, Reaction, and PathwayStep
        whenever(mockModel.addNew(Pathway::class.java, any())).thenReturn(mockBPPathway)
        whenever(mockModel.addNew(BiochemicalReaction::class.java, any())).thenReturn(mockBPReaction)
        whenever(mockModel.addNew(PathwayStep::class.java, any())).thenReturn(mockPathwayStep)
        
        // Create the builder
        val builder = BioPAXPathwayBuilder(mockPathway, mockModel)
        
        // Call the method
        builder.addReactomePathway()
        
        // Verify that the reaction was processed
        verify(mockModel).addNew(BiochemicalReaction::class.java, any())
        verify(mockBPPathway).addPathwayOrder(mockPathwayStep)
        verify(mockBPPathway).addPathwayComponent(mockBPReaction)
        verify(mockPathwayStep).addStepProcess(mockBPReaction)
    }
} 