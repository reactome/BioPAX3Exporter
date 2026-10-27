package org.reactome.server.tools

import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.BiochemicalReaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.reactome.server.graph.domain.model.ReactionLikeEvent

class BioPAXInteractionBuilderTest {
    @Test
    fun `test addReactomeRLEvent with null event returns null`() {
        // Create a mock Model
        val mockModel = mock(Model::class.java)

        // Create the builder with null event
        val builder = BioPAXInteractionBuilder(null, ConversionContext(0, "", mockModel))

        // Call the method
        val result = builder.addReactomeRLEvent()

        // Verify that no BiochemicalReaction was created
        verify(mockModel, never()).addNew(eq(BiochemicalReaction::class.java), any())

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
        whenever(mockModel.addNew(eq(BiochemicalReaction::class.java), any())).thenReturn(mockBPReaction)

        // Create the builder
        val builder = BioPAXInteractionBuilder(mockRLEvent, ConversionContext(0, "", mockModel))

        // Call the method
        val result = builder.addReactomeRLEvent()

        // Verify that the BiochemicalReaction was created with the correct properties
        verify(mockModel).addNew(eq(BiochemicalReaction::class.java), any())
        verify(mockBPReaction).displayName = "Test Reaction"

        // Verify that the result is the mock BiochemicalReaction
        assertEquals(mockBPReaction, result)
    }
}
