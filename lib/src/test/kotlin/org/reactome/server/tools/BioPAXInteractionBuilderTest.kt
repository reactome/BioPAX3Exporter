// Control.controlled is deprecated in Paxtools 6, with no replacement yet.
@file:Suppress("DEPRECATION")

package org.reactome.server.tools

import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.BiochemicalReaction
import org.biopax.paxtools.model.level3.Catalysis
import org.biopax.paxtools.model.level3.ConversionDirectionType
import org.biopax.paxtools.model.level3.Degradation
import org.biopax.paxtools.model.level3.TemplateReaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
import org.reactome.server.graph.domain.model.Complex
import org.reactome.server.graph.domain.model.EntityWithAccessionedSequence
import org.reactome.server.graph.domain.model.GO_MolecularFunction
import org.reactome.server.graph.domain.model.Reaction
import org.reactome.server.graph.domain.model.ReactionLikeEvent
import org.reactome.server.graph.domain.model.SimpleEntity
import org.reactome.server.graph.domain.model.PhysicalEntity as RPhysicalEntity

class BioPAXInteractionBuilderTest {
    @Test
    fun `test addReactomeRLEvent with null event returns null`() {
        // Create a mock Model
        val mockModel = mock(Model::class.java)

        // Create the builder with null event
        val builder = BioPAXInteractionBuilder(null, ConversionContext("", 97, mockModel))

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

    // Reactions as the release's converter writes them (spec 001 T012), on a real model.
    private val context = ConversionContext("http://www.reactome.org/biopax/98/1#", 98)

    private inline fun <reified T : RPhysicalEntity> entity(
        dbId: Long,
        name: String,
    ): RPhysicalEntity =
        mock(T::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn(name)
        }

    private fun catalyst(ecNumber: String?): CatalystActivity {
        val function = mock(GO_MolecularFunction::class.java)
        whenever(function.ecNumber).thenReturn(ecNumber)
        return mock(CatalystActivity::class.java).also { whenever(it.activity).thenReturn(function) }
    }

    private inline fun <reified T : ReactionLikeEvent> reaction(
        name: String,
        inputs: List<RPhysicalEntity>,
        outputs: List<RPhysicalEntity>,
        catalysts: List<CatalystActivity> = emptyList(),
    ): T =
        mock(T::class.java).also {
            whenever(it.dbId).thenReturn(500)
            whenever(it.stId).thenReturn("R-HSA-500")
            whenever(it.stIdVersion).thenReturn("R-HSA-500.2")
            // The shortest name is the display name, not the graph's display name
            whenever(it.displayName).thenReturn("$name, as displayed")
            whenever(it.name).thenReturn(listOf("a long name of the reaction", name))
            whenever(it.input).thenReturn(inputs)
            whenever(it.output).thenReturn(outputs)
            whenever(it.catalystActivity).thenReturn(catalysts)
        }

    @Test
    fun `a reaction - left to right, each participant once with its count, and EC numbers`() {
        val atp = entity<SimpleEntity>(1, "ATP")
        val water = entity<SimpleEntity>(2, "H2O")
        val adp = entity<SimpleEntity>(3, "ADP")
        val catalysts = listOf(catalyst("3.6.1.3"), catalyst(null), catalyst("3.6.4.12"))
        // The graph lists a participant once for each time it takes part.
        val event = reaction<Reaction>("ATP hydrolysis", listOf(atp, water, atp), listOf(adp, adp), catalysts)

        val bp = BioPAXInteractionBuilder(event, context).addReactomeRLEvent() as BiochemicalReaction

        assertEquals("ATP hydrolysis", bp.displayName)
        assertEquals(ConversionDirectionType.LEFT_TO_RIGHT, bp.conversionDirection)
        assertEquals(setOf("ATP", "H2O"), bp.left.map { it.displayName }.toSet())
        assertEquals(setOf("ADP"), bp.right.map { it.displayName }.toSet())
        assertEquals(
            mapOf("ATP" to 2f, "ADP" to 2f),
            bp.participantStoichiometry.associate { it.physicalEntity.displayName to it.stoichiometricCoefficient },
        )
        assertEquals(setOf("3.6.1.3", "3.6.4.12"), bp.ecNumber)
        assertEquals(
            setOf("Reactome Database ID Release 98 500", "Reactome R-HSA-500"),
            bp.xref.map { "${it.db} ${it.id}" }.toSet(),
        )
        assertEquals(
            "Reactome",
            bp.dataSource
                .single()
                .name
                .single(),
        )
        assertEquals(3, context.model.getObjects(Catalysis::class.java).size)
    }

    @Test
    fun `a black box with one input, no output and degradation in its name is a Degradation`() {
        val apoB = entity<Complex>(4, "apoB-48")
        // Capitalised, as the release's names are ("Degradation of newly synthesized ApoB-48")
        val event = reaction<BlackBoxEvent>("Degradation of apoB-48", listOf(apoB), emptyList(), listOf(catalyst("3.4.21.1")))

        val bp = BioPAXInteractionBuilder(event, context).addReactomeRLEvent()

        assertTrue(bp is Degradation)
        bp as Degradation
        assertEquals(setOf("apoB-48"), bp.left.map { it.displayName }.toSet())
        assertTrue(bp.right.isEmpty())
        // Neither a direction nor EC numbers, which are a BiochemicalReaction's
        assertNull(bp.conversionDirection)
        // Its catalysis, as the release writes one for a Degradation's catalyst
        assertEquals(
            setOf(bp),
            context.model
                .getObjects(Catalysis::class.java)
                .single()
                .controlled,
        )
    }

    @Test
    fun `a black box with no input and one output is a TemplateReaction, its product only`() {
        val gene = entity<EntityWithAccessionedSequence>(5, "SOCS3")
        val event = reaction<BlackBoxEvent>("SOCS3 gene expression", emptyList(), listOf(gene), listOf(catalyst("1.1.1.1")))

        val bp = BioPAXInteractionBuilder(event, context).addReactomeRLEvent()

        assertTrue(bp is TemplateReaction)
        bp as TemplateReaction
        assertEquals(setOf("SOCS3"), bp.product.map { it.displayName }.toSet())
        // Not written as a participant beside it, and no catalysis
        assertTrue(bp.participant.isEmpty())
        assertTrue(context.model.getObjects(Catalysis::class.java).isEmpty())
        // Its names and ids, as every event has them
        assertEquals("SOCS3 gene expression", bp.displayName)
        assertEquals(setOf("a long name of the reaction"), bp.name - bp.displayName)
        assertEquals(setOf("Reactome Database ID Release 98 500", "Reactome R-HSA-500"), bp.xref.map { "${it.db} ${it.id}" }.toSet())
    }

    @Test
    fun `any other black box, or a reaction of either shape, is a BiochemicalReaction`() {
        val one = entity<SimpleEntity>(6, "A")
        val other = entity<SimpleEntity>(7, "B")
        val notNamed = reaction<BlackBoxEvent>("A is removed", listOf(one), emptyList())
        val twoOut = reaction<BlackBoxEvent>("A and B appear", emptyList(), listOf(one, other))
        val notBlackBox = reaction<Reaction>("B degradation", listOf(other), emptyList())
        val twoIn = reaction<BlackBoxEvent>("A and B degradation", listOf(one, other), emptyList())
        // A Degradation has no output, and a TemplateReaction no input
        val inAndOut = reaction<BlackBoxEvent>("Degradation of A to B", listOf(one), listOf(other))
        val fromInput = reaction<BlackBoxEvent>("B from A", listOf(one), listOf(other))
        for (event in listOf(notNamed, twoOut, notBlackBox, twoIn, inAndOut, fromInput)) {
            val bp = BioPAXInteractionBuilder(event, ConversionContext("x#", 98)).addReactomeRLEvent()
            assertEquals(BiochemicalReaction::class.java, bp!!.modelInterface, event.displayName)
        }
    }
}
