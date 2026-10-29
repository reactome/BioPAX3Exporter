package org.reactome.server.tools

import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.Entity
import org.biopax.paxtools.model.level3.Pathway
import org.biopax.paxtools.model.level3.Provenance
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.reactome.server.graph.domain.model.Event

class BioPAX3BasicElementsBuilderTest {
    @Test
    fun `test addReactomeDataSource uses existing Provenance if exists`() {
        // One Provenance per file, as the published files have (research R1):
        // a second element asks for the one already in the model.
        val mockModel = mock(Model::class.java)
        val mockEntity = mock(Entity::class.java)
        val mockProvenance = mock(Provenance::class.java)
        whenever(mockProvenance.uri).thenReturn("http://www.reactome.org/biopax/97/1#Provenance1")
        whenever(mockModel.getObjects(Provenance::class.java)).thenReturn(setOf(mockProvenance))

        val builder =
            BioPAX3BasicElementsBuilder(
                thisReactomeEvent = null,
                context = ConversionContext("http://www.reactome.org/biopax/97/1#", 97, mockModel),
                thisBPElement = mockEntity,
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
        val builder =
            BioPAX3BasicElementsBuilder(
                thisReactomeEvent = null,
                context = ConversionContext("", 97, mock(Model::class.java)),
                thisBPElement = mockPathway,
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
        val builder =
            BioPAX3BasicElementsBuilder(
                thisReactomeEvent = mockEvent,
                context = ConversionContext("", 97, mock(Model::class.java)),
                thisBPElement = mockEntity,
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
        val builder =
            BioPAX3BasicElementsBuilder(
                thisReactomeEvent = null,
                context = ConversionContext("", 97, mockModel),
                thisBPElement = mockEntity,
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
}
