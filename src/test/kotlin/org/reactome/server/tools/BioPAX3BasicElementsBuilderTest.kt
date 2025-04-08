package org.reactome.server.tools

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.*
import org.reactome.server.graph.domain.model.Event
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class BioPAX3BasicElementsBuilderTest {
    
    @Test
    fun `test addBioSourceInformation does nothing when event is null`() {
        // Create a mock Pathway
        val mockPathway = mock(Pathway::class.java)
        
        // Create the builder with null event
        val builder = BioPAX3BasicElementsBuilder(
            thisReactomeEvent = null,
            thisBPModel = mock(Model::class.java),
            thisBPElement = mockPathway
        )
        
        // Call the method
        builder.addBioSourceInformation()
        
        // Verify that organism was not set
        verify(mockPathway, never()).organism = any()
    }
    
    @Test
    fun `test addBioSourceInformation does nothing when element is not a Pathway`() {
        // Create a mock Event and Entity (not a Pathway)
        val mockEvent = mock(Event::class.java)
        val mockEntity = mock(Entity::class.java)
        
        // Create the builder
        val builder = BioPAX3BasicElementsBuilder(
            thisReactomeEvent = mockEvent,
            thisBPModel = mock(Model::class.java),
            thisBPElement = mockEntity
        )
        
        // Call the method
        builder.addBioSourceInformation()
        
        // Verify that organism was not set
        verify(mockEntity, never()).organism = any()
    }
    
    @Test
    fun `test addReactomeDataSource creates new Provenance if not exists`() {
        // Create mocks
        val mockModel = mock(Model::class.java)
        val mockEntity = mock(Entity::class.java)
        val mockProvenance = mock(Provenance::class.java)
        
        // Set up the model to return empty set of Provenance objects
        whenever(mockModel.getObjects(Provenance::class.java)).thenReturn(emptySet())
        
        // Set up the model to create a new Provenance
        whenever(mockModel.addNew(Provenance::class.java, any())).thenReturn(mockProvenance)
        
        // Create the builder
        val builder = BioPAX3BasicElementsBuilder(
            thisReactomeEvent = null,
            thisBPModel = mockModel,
            thisBPElement = mockEntity
        )
        
        // Call the method
        builder.addReactomeDataSource()
        
        // Verify that a new Provenance was created
        verify(mockModel).addNew(Provenance::class.java, any())
        
        // Verify that the Provenance was configured
        verify(mockProvenance).addName("Reactome")
        verify(mockProvenance).addComment("http://www.reactome.org")
        
        // Verify that the Provenance was added to the entity
        verify(mockEntity).addDataSource(mockProvenance)
    }
    
    @Test
    fun `test addReactomeDataSource uses existing Provenance if exists`() {
        // Create mocks
        val mockModel = mock(Model::class.java)
        val mockEntity = mock(Entity::class.java)
        val mockProvenance = mock(Provenance::class.java)
        
        // Set up the model to return a set with the mock Provenance
        whenever(mockModel.getObjects(Provenance::class.java)).thenReturn(setOf(mockProvenance))
        
        // Set up BioPAX3Utils to return the mock Provenance
        // This is a bit tricky since BioPAX3Utils is an object, but we can test the behavior indirectly
        
        // Create the builder
        val builder = BioPAX3BasicElementsBuilder(
            thisReactomeEvent = null,
            thisBPModel = mockModel,
            thisBPElement = mockEntity
        )
        
        // Call the method
        builder.addReactomeDataSource()
        
        // Verify that a new Provenance was not created
        verify(mockModel, never()).addNew(Provenance::class.java, any())
        
        // Verify that the Provenance was added to the entity
        verify(mockEntity).addDataSource(mockProvenance)
    }
    
    @Test
    fun `test addEvidence creates new Evidence if not exists`() {
        // Create mocks
        val mockModel = mock(Model::class.java)
        val mockEntity = mock(Entity::class.java)
        val mockEvidence = mock(Evidence::class.java)
        
        // Set up the model to return empty set of Evidence objects
        whenever(mockModel.getObjects(Evidence::class.java)).thenReturn(emptySet())
        
        // Set up the model to create a new Evidence
        whenever(mockModel.addNew(Evidence::class.java, any())).thenReturn(mockEvidence)
        
        // Create the builder
        val builder = BioPAX3BasicElementsBuilder(
            thisReactomeEvent = null,
            thisBPModel = mockModel,
            thisBPElement = mockEntity
        )
        
        // Call the method
        builder.addEvidence()
        
        // Verify that a new Evidence was created
        verify(mockModel).addNew(Evidence::class.java, any())
        
        // Verify that the Evidence was added to the entity
        verify(mockEntity).addEvidence(mockEvidence)
    }
    
    @Test
    fun `test addEvidence uses existing Evidence if exists`() {
        // Create mocks
        val mockModel = mock(Model::class.java)
        val mockEntity = mock(Entity::class.java)
        val mockEvidence = mock(Evidence::class.java)
        
        // Set up the model to return a set with the mock Evidence
        whenever(mockModel.getObjects(Evidence::class.java)).thenReturn(setOf(mockEvidence))
        
        // Create the builder
        val builder = BioPAX3BasicElementsBuilder(
            thisReactomeEvent = null,
            thisBPModel = mockModel,
            thisBPElement = mockEntity
        )
        
        // Call the method
        builder.addEvidence()
        
        // Verify that a new Evidence was not created
        verify(mockModel, never()).addNew(Evidence::class.java, any())
        
        // Verify that the Evidence was added to the entity
        verify(mockEntity).addEvidence(mockEvidence)
    }
} 