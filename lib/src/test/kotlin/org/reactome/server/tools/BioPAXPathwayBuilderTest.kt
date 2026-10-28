package org.reactome.server.tools

import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.Catalysis
import org.biopax.paxtools.model.level3.Degradation
import org.biopax.paxtools.model.level3.PathwayStep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.reactome.server.graph.domain.model.BlackBoxEvent
import org.reactome.server.graph.domain.model.CatalystActivity
import org.reactome.server.graph.domain.model.Pathway
import org.reactome.server.graph.domain.model.SimpleEntity
import org.biopax.paxtools.model.level3.Pathway as BPPathway

class BioPAXPathwayBuilderTest {
    @Test
    fun `test addReactomePathway with null pathway returns null`() {
        // Create mocks
        val mockModel = mock(Model::class.java)

        // Create the builder with null pathway
        val builder = BioPAXPathwayBuilder(null, ConversionContext("", 97, mockModel))

        // Call the method
        builder.addReactomePathway()

        // Verify that no Pathway was created
        verify(mockModel, never()).addNew(eq(BPPathway::class.java), any())
    }

    @Test
    fun `a step lists the catalysis of a Degradation, as of any interaction`() {
        val apoB = mock(SimpleEntity::class.java)
        whenever(apoB.dbId).thenReturn(4)
        whenever(apoB.displayName).thenReturn("apoB-48")
        val catalyst = mock(CatalystActivity::class.java)
        val degradation = mock(BlackBoxEvent::class.java)
        whenever(degradation.dbId).thenReturn(174731)
        whenever(degradation.displayName).thenReturn("Degradation of apoB-48")
        whenever(degradation.input).thenReturn(listOf(apoB))
        whenever(degradation.output).thenReturn(emptyList())
        whenever(degradation.catalystActivity).thenReturn(listOf(catalyst))
        val pathway = mock(Pathway::class.java)
        whenever(pathway.dbId).thenReturn(1)
        whenever(pathway.displayName).thenReturn("Lipoprotein metabolism")
        whenever(pathway.hasEvent).thenReturn(listOf(degradation))
        val context = ConversionContext("http://www.reactome.org/biopax/98/1#", 98)

        BioPAXPathwayBuilder(pathway, context).addReactomePathway()

        val step = context.model.getObjects(PathwayStep::class.java).single()
        assertEquals(
            setOf(Degradation::class.java, Catalysis::class.java),
            step.stepProcess.map { it.modelInterface }.toSet(),
        )
    }
}
