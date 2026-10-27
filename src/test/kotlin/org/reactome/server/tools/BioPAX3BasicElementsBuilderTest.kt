package org.reactome.server.tools

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.*
import org.reactome.server.graph.domain.model.Event
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class BioPAX3BasicElementsBuilderTest {
    // The exporter keeps its base, version and id counters in shared state
    // (spec 001 T008 removes it); each test starts from none.
    @BeforeEach
    fun resetSharedState() {
        WriteBioPAX3.xmlBase = ""
        WriteBioPAX3.dbVersion = 0
        BioPAX3Utils.clearCounterArray()
    }

    @Test
    fun `test addReactomeDataSource uses existing Provenance if exists`() {
        // One Provenance per file, as the published files have (research R1):
        // a second element asks for the one already in the model.
        val mockModel = mock(Model::class.java)
        val mockEntity = mock(Entity::class.java)
        val mockProvenance = mock(Provenance::class.java)
        whenever(mockProvenance.rdfId).thenReturn(BioPAX3Utils.getID("Provenance1"))
        whenever(mockModel.getObjects(Provenance::class.java)).thenReturn(setOf(mockProvenance))

        val builder = BioPAX3BasicElementsBuilder(
            thisReactomeEvent = null,
            thisBPModel = mockModel,
            thisBPElement = mockEntity
        )
        builder.addReactomeDataSource()

        verify(mockModel, never()).addNew(eq(Provenance::class.java), any())
        verify(mockEntity).addDataSource(mockProvenance)
    }

    
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
        
        // An Entity has no organism; nothing is set on it.
        verifyNoInteractions(mockEntity)
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
        whenever(mockModel.addNew(eq(Provenance::class.java), any())).thenReturn(mockProvenance)
        
        // Create the builder
        val builder = BioPAX3BasicElementsBuilder(
            thisReactomeEvent = null,
            thisBPModel = mockModel,
            thisBPElement = mockEntity
        )
        
        // Call the method
        builder.addReactomeDataSource()
        
        // Verify that a new Provenance was created
        verify(mockModel).addNew(eq(Provenance::class.java), any())
        
        // Verify that the Provenance was configured
        verify(mockProvenance).addName("Reactome")
        verify(mockProvenance).addComment("http://www.reactome.org")
        
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
        whenever(mockModel.addNew(eq(Evidence::class.java), any())).thenReturn(mockEvidence)
        
        // Create the builder
        val builder = BioPAX3BasicElementsBuilder(
            thisReactomeEvent = null,
            thisBPModel = mockModel,
            thisBPElement = mockEntity
        )
        
        // Call the method
        builder.addEvidence()
        
        // Verify that a new Evidence was created
        verify(mockModel).addNew(eq(Evidence::class.java), any())
        
        // Verify that the Evidence was added to the entity
        verify(mockEntity).addEvidence(mockEvidence)
    }
    
} 