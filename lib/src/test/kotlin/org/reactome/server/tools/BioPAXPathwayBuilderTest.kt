package org.reactome.server.tools

import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.BiochemicalReaction
import org.biopax.paxtools.model.level3.Catalysis
import org.biopax.paxtools.model.level3.Control
import org.biopax.paxtools.model.level3.Degradation
import org.biopax.paxtools.model.level3.PathwayStep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.reactome.server.graph.domain.model.BlackBoxEvent
import org.reactome.server.graph.domain.model.CatalystActivity
import org.reactome.server.graph.domain.model.Event
import org.reactome.server.graph.domain.model.NegativeRegulation
import org.reactome.server.graph.domain.model.Pathway
import org.reactome.server.graph.domain.model.Reaction
import org.reactome.server.graph.domain.model.SimpleEntity
import org.reactome.server.graph.domain.model.Species
import org.biopax.paxtools.model.level3.Pathway as BPPathway

/**
 * Pathways and their steps as the release's converter writes them into one file (spec 001
 * T015), on a real Paxtools model. Graph-core's objects are mocks, each made before the
 * next is stubbed.
 */
class BioPAXPathwayBuilderTest {
    private val context = ConversionContext("http://www.reactome.org/biopax/98/1#", 98)

    private fun reaction(
        dbId: Long,
        preceding: List<Event> = emptyList(),
    ): Reaction {
        val input =
            mock(SimpleEntity::class.java).also {
                whenever(it.dbId).thenReturn(dbId * 10)
                whenever(it.displayName).thenReturn("entity ${dbId * 10}")
            }
        return mock(Reaction::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn("reaction $dbId")
            whenever(it.input).thenReturn(listOf(input))
            whenever(it.output).thenReturn(emptyList())
            whenever(it.precedingEvent).thenReturn(preceding)
        }
    }

    private fun pathway(
        dbId: Long,
        events: List<Event>,
    ): Pathway =
        mock(Pathway::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn("pathway $dbId")
            whenever(it.hasEvent).thenReturn(events)
        }

    private fun bpPathway(dbId: Long): BPPathway =
        context.model.getObjects(BPPathway::class.java).single { it.displayName == "pathway $dbId" }

    private fun bpReaction(dbId: Long): BiochemicalReaction =
        context.model.getObjects(BiochemicalReaction::class.java).single { it.displayName == "reaction $dbId" }

    private fun stepOf(dbId: Long): PathwayStep =
        context.model.getObjects(PathwayStep::class.java).single { step ->
            step.stepProcess.any { it.displayName == "reaction $dbId" || it.displayName == "pathway $dbId" }
        }

    @Test
    fun `a pathway two pathways hold is written once, a component of each`() {
        val shared = pathway(3, listOf(reaction(30)))
        val top = pathway(1, listOf(pathway(2, listOf(shared)), shared))

        BioPAXPathwayBuilder(top, context).addReactomePathway()

        assertEquals(3, context.model.getObjects(BPPathway::class.java).size)
        assertSame(bpPathway(3), bpPathway(1).pathwayComponent.single { it.displayName == "pathway 3" })
        assertSame(bpPathway(3), bpPathway(2).pathwayComponent.single())
        assertEquals(1, context.model.getObjects(BiochemicalReaction::class.java).size)
    }

    @Test
    fun `one step for each event, in the order of every pathway that holds it`() {
        val shared = reaction(20)
        val top = pathway(1, listOf(pathway(2, listOf(shared)), shared))

        BioPAXPathwayBuilder(top, context).addReactomePathway()

        // Steps for pathway 2 and reaction 20; none for the pathway given, which no pathway holds
        assertEquals(2, context.model.getObjects(PathwayStep::class.java).size)
        assertSame(stepOf(20), bpPathway(2).pathwayOrder.single())
        assertTrue(stepOf(20) in bpPathway(1).pathwayOrder)
        assertEquals(setOf(bpReaction(20)), stepOf(20).stepProcess.toSet())
    }

    @Test
    fun `a pathway is made before what it holds, and its step's process is the pathway`() {
        BioPAXPathwayBuilder(pathway(1, listOf(pathway(2, listOf(reaction(20))))), context).addReactomePathway()

        assertEquals("http://www.reactome.org/biopax/98/1#Pathway1", bpPathway(1).rdfId)
        assertEquals("http://www.reactome.org/biopax/98/1#Pathway2", bpPathway(2).rdfId)
        assertEquals(setOf(bpPathway(2)), stepOf(2).stepProcess.toSet())
    }

    @Test
    fun `each step's next steps are those of the events it precedes, in any pathway`() {
        val first = reaction(20)
        val elsewhere = reaction(40)
        val outside = reaction(50)
        // 30 follows 20 in the same pathway, 31 in another; 32 follows one no pathway here holds
        val top =
            pathway(
                1,
                listOf(
                    pathway(2, listOf(first, reaction(30, listOf(first)), reaction(32, listOf(outside)))),
                    pathway(3, listOf(elsewhere, reaction(31, listOf(first, elsewhere)))),
                ),
            )

        BioPAXPathwayBuilder(top, context).addReactomePathway()

        assertEquals(setOf(stepOf(30), stepOf(31)), stepOf(20).nextStep)
        assertEquals(setOf(stepOf(31)), stepOf(40).nextStep)
        assertEquals(emptySet<PathwayStep>(), stepOf(30).nextStep)
        assertEquals(emptySet<PathwayStep>(), stepOf(32).nextStep)
        assertTrue(context.model.getObjects(BiochemicalReaction::class.java).none { it.displayName == "reaction 50" })
    }

    @Test
    fun `a step lists its reaction's regulations`() {
        val regulator =
            mock(SimpleEntity::class.java).also {
                whenever(it.dbId).thenReturn(700)
                whenever(it.displayName).thenReturn("inhibitor")
            }
        val regulation =
            mock(NegativeRegulation::class.java).also {
                whenever(it.dbId).thenReturn(701)
                whenever(it.regulator).thenReturn(regulator)
                whenever(it.activeUnit).thenReturn(emptyList())
            }
        val regulated = reaction(20)
        whenever(regulated.regulatedBy).thenReturn(listOf(regulation))

        BioPAXPathwayBuilder(pathway(1, listOf(regulated)), context).addReactomePathway()

        val control = context.model.getObjects(Control::class.java).single()
        assertEquals(setOf(bpReaction(20), control), stepOf(20).stepProcess.toSet())
    }

    @Test
    fun `a species' file is its events, with no header pathway, its steps linked`() {
        val human =
            mock(Species::class.java).also {
                whenever(it.dbId).thenReturn(48887)
                whenever(it.displayName).thenReturn("Homo sapiens")
            }
        val shared = reaction(20)
        val next = reaction(21, listOf(shared))
        // A top-level reaction is written, in no pathway and with no step
        val events = listOf(pathway(1, listOf(shared, next)), pathway(2, listOf(shared)), reaction(90))

        val model =
            WriteBioPAX3(human, 98).run {
                createModelForSpecies(human, events)
                getModel()!!
            }

        assertEquals(setOf("pathway 1", "pathway 2"), model.getObjects(BPPathway::class.java).map { it.displayName }.toSet())
        assertEquals(
            setOf("reaction 20", "reaction 21", "reaction 90"),
            model.getObjects(BiochemicalReaction::class.java).map { it.displayName }.toSet(),
        )
        val steps = model.getObjects(PathwayStep::class.java).associateBy { it.stepProcess.single().displayName }
        assertEquals(setOf("reaction 20", "reaction 21"), steps.keys)
        assertEquals(setOf(steps["reaction 21"]), steps["reaction 20"]!!.nextStep)
    }

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
