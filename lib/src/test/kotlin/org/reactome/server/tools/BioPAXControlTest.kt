package org.reactome.server.tools

import org.biopax.paxtools.io.SimpleIOHandler
import org.biopax.paxtools.model.level3.BiochemicalReaction
import org.biopax.paxtools.model.level3.Catalysis
import org.biopax.paxtools.model.level3.Control
import org.biopax.paxtools.model.level3.ControlType
import org.biopax.paxtools.model.level3.RelationshipXref
import org.biopax.paxtools.model.level3.TemplateReaction
import org.biopax.paxtools.model.level3.TemplateReactionRegulation
import org.biopax.paxtools.model.level3.UnificationXref
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.reactome.server.graph.domain.model.BlackBoxEvent
import org.reactome.server.graph.domain.model.CatalystActivity
import org.reactome.server.graph.domain.model.EntityWithAccessionedSequence
import org.reactome.server.graph.domain.model.GO_MolecularFunction
import org.reactome.server.graph.domain.model.NegativeRegulation
import org.reactome.server.graph.domain.model.PositiveRegulation
import org.reactome.server.graph.domain.model.Reaction
import org.reactome.server.graph.domain.model.ReactionLikeEvent
import org.reactome.server.graph.domain.model.Regulation
import org.reactome.server.graph.domain.model.Requirement
import org.reactome.server.graph.domain.model.SimpleEntity
import java.io.ByteArrayOutputStream
import org.reactome.server.graph.domain.model.PhysicalEntity as RPhysicalEntity

// What a control controls. Control.processes is deprecated (Paxtools 5.1's javadoc,
// 6.0's annotation) with no replacement yet; each control here has one process.
@Suppress("DEPRECATION")
internal val Control.processes: Set<org.biopax.paxtools.model.level3.Process> get() = controlled

/**
 * Catalyses and regulations as the release's converter writes them (spec 001 T013), on a
 * real Paxtools model. Graph-core's objects are mocks, each made before the next is stubbed.
 */
class BioPAXControlTest {
    private val context = ConversionContext("http://www.reactome.org/biopax/98/1#", 98)

    private inline fun <reified T : RPhysicalEntity> entity(
        dbId: Long,
        name: String,
    ): RPhysicalEntity =
        mock(T::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn(name)
        }

    private fun activity(
        dbId: Long,
        catalyst: RPhysicalEntity,
        units: Set<RPhysicalEntity> = emptySet(),
    ): CatalystActivity {
        val function = mock(GO_MolecularFunction::class.java)
        whenever(function.dbId).thenReturn(dbId + 1)
        whenever(function.accession).thenReturn("0004519")
        return mock(CatalystActivity::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.physicalEntity).thenReturn(catalyst)
            whenever(it.activity).thenReturn(function)
            whenever(it.activeUnit).thenReturn(units)
        }
    }

    private inline fun <reified T : Regulation> regulation(
        dbId: Long,
        regulator: RPhysicalEntity,
        units: List<RPhysicalEntity> = emptyList(),
    ): T =
        mock(T::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.stId).thenReturn("R-HSA-$dbId")
            whenever(it.stIdVersion).thenReturn("R-HSA-$dbId.1")
            whenever(it.regulator).thenReturn(regulator)
            whenever(it.activeUnit).thenReturn(units)
        }

    private inline fun <reified T : ReactionLikeEvent> reaction(
        dbId: Long,
        inputs: List<RPhysicalEntity>,
        outputs: List<RPhysicalEntity>,
        catalysts: List<CatalystActivity> = emptyList(),
        regulations: List<Regulation> = emptyList(),
    ): T =
        mock(T::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn("reaction $dbId")
            whenever(it.input).thenReturn(inputs)
            whenever(it.output).thenReturn(outputs)
            whenever(it.catalystActivity).thenReturn(catalysts)
            whenever(it.regulatedBy).thenReturn(regulations)
        }

    private fun org.biopax.paxtools.model.level3.Xref.dbAndId() = "$db $id"

    @Test
    fun `a catalysis - its catalyst, function, own id and active unit`() {
        val enzyme = entity<EntityWithAccessionedSequence>(10, "MLH1")
        val unit = entity<EntityWithAccessionedSequence>(11, "PMS2")
        val atp = entity<SimpleEntity>(12, "ATP")
        val event = reaction<Reaction>(500, listOf(atp), listOf(atp), catalysts = listOf(activity(600, enzyme, setOf(unit))))

        val reaction = BioPAXInteractionBuilder(event, context).addReactomeRLEvent() as BiochemicalReaction
        val catalysis = context.model.getObjects(Catalysis::class.java).single()

        assertEquals(setOf("MLH1"), catalysis.controller.map { (it as org.biopax.paxtools.model.level3.Named).displayName }.toSet())
        assertEquals(setOf(reaction), catalysis.processes)
        assertEquals(ControlType.ACTIVATION, catalysis.controlType)
        val xrefs = catalysis.xref.filterIsInstance<RelationshipXref>()
        val function = xrefs.single { it.db == "GENE ONTOLOGY" }
        assertEquals("GO:0004519", function.id)
        assertEquals(setOf("gene ontology term for cellular function"), function.relationshipType.term)
        val own = xrefs.single { it.db == "Reactome Database ID Release 98" }
        assertEquals("600", own.id)
        assertEquals(
            setOf(
                "Database identifier. Use this URL to connect to the web page of this instance in Reactome: " +
                    "http://www.reactome.org/cgi-bin/eventbrowser?DB=gk_current&ID=600",
            ),
            own.comment,
        )
        assertEquals(setOf("Same Catalyst Activity"), own.relationshipType.term)
        // The release gives "Same Catalyst Activity" no PSI-MI xref, and a catalyst activity no stable id
        assertTrue(own.relationshipType.xref.isEmpty())
        assertTrue(catalysis.xref.filterIsInstance<UnificationXref>().isEmpty())
        assertEquals(
            "Reactome",
            catalysis.dataSource
                .single()
                .name
                .single(),
        )
        // The active unit is written, and named by the id it is written under
        val written =
            context.model.objects
                .filterIsInstance<org.biopax.paxtools.model.level3.Named>()
                .single { it.displayName == "PMS2" }
        assertEquals(setOf("activeUnit: #" + written.uri.substringAfterLast('#')), catalysis.comment)
    }

    @Test
    fun `active units are taken in dbId order, so the ids are the same however the set iterates`() {
        fun written(order: List<Long>): String {
            val units = order.map { entity<EntityWithAccessionedSequence>(it, "unit $it") }.toCollection(LinkedHashSet())
            val enzyme = entity<EntityWithAccessionedSequence>(10, "enzyme")
            val event = reaction<Reaction>(500, emptyList(), emptyList(), catalysts = listOf(activity(600, enzyme, units)))
            val context = ConversionContext("http://www.reactome.org/biopax/98/1#", 98)
            BioPAXInteractionBuilder(event, context).addReactomeRLEvent()
            return ByteArrayOutputStream().also { SimpleIOHandler().convertToOWL(context.model, it) }.toString(Charsets.UTF_8)
        }
        assertEquals(written(listOf(21, 22, 23)), written(listOf(23, 21, 22)))
        // And in dbId order: the lowest is numbered first, whatever order the set gives
        val units = listOf(23L, 21L, 22L).map { entity<EntityWithAccessionedSequence>(it, "unit $it") }.toCollection(LinkedHashSet())
        val event =
            reaction<Reaction>(505, emptyList(), emptyList(), catalysts = listOf(activity(602, entity<SimpleEntity>(10, "enzyme"), units)))
        BioPAXInteractionBuilder(event, context).addReactomeRLEvent()
        val numbers =
            (21..23).map { n ->
                context.model.objects
                    .filterIsInstance<org.biopax.paxtools.model.level3.Named>()
                    .single { it.displayName == "unit $n" }
                    .uri
                    .takeLastWhile { it.isDigit() }
                    .toInt()
            }
        assertEquals(numbers.sorted(), numbers)
    }

    @Test
    fun `a regulation - its type from its class, its ids, its regulator and active unit`() {
        val inhibitor = entity<SimpleEntity>(20, "chlorpromazine")
        val activator = entity<SimpleEntity>(21, "cAMP")
        val required = entity<SimpleEntity>(22, "Mg2+")
        val unit = entity<EntityWithAccessionedSequence>(23, "PKA")
        val down = regulation<NegativeRegulation>(700, inhibitor)
        val up = regulation<PositiveRegulation>(701, activator, listOf(unit))
        val needs = regulation<Requirement>(702, required)
        val event = reaction<Reaction>(500, emptyList(), emptyList(), regulations = listOf(down, up, needs))

        val reaction = BioPAXInteractionBuilder(event, context).addReactomeRLEvent()
        val controls = context.model.getObjects(Control::class.java).filter { it !is Catalysis }

        fun of(dbId: String) = controls.single { c -> c.xref.any { it.db == "Reactome Database ID Release 98" && it.id == dbId } }
        assertEquals(ControlType.INHIBITION, of("700").controlType)
        assertEquals(ControlType.ACTIVATION, of("701").controlType)
        // A Requirement is a positive regulation
        assertEquals(ControlType.ACTIVATION, of("702").controlType)
        assertEquals(setOf("Reactome Database ID Release 98 700", "Reactome R-HSA-700"), of("700").xref.map { it.dbAndId() }.toSet())
        controls.forEach {
            assertEquals(Control::class.java, it.modelInterface)
            assertEquals(setOf(reaction), it.processes)
            assertEquals(
                "Reactome",
                it.dataSource
                    .single()
                    .name
                    .single(),
            )
        }
        assertEquals(
            setOf("chlorpromazine"),
            of("700").controller.map { (it as org.biopax.paxtools.model.level3.Named).displayName }.toSet(),
        )
        val pka =
            context.model.objects
                .filterIsInstance<org.biopax.paxtools.model.level3.Named>()
                .single { it.displayName == "PKA" }
        assertEquals(setOf("activeUnit: #" + pka.uri.substringAfterLast('#')), of("701").comment)
    }

    @Test
    fun `a TemplateReaction's regulation is a TemplateReactionRegulation, and it has no catalysis`() {
        val factor = entity<EntityWithAccessionedSequence>(30, "STAT3")
        val gene = entity<EntityWithAccessionedSequence>(31, "SOCS3")
        val enzyme = entity<EntityWithAccessionedSequence>(32, "an enzyme")
        val event =
            reaction<BlackBoxEvent>(
                500,
                emptyList(),
                listOf(gene),
                catalysts = listOf(activity(600, enzyme)),
                regulations = listOf(regulation<PositiveRegulation>(703, factor)),
            )

        val template = BioPAXInteractionBuilder(event, context).addReactomeRLEvent() as TemplateReaction
        val regulation = context.model.getObjects(TemplateReactionRegulation::class.java).single()

        assertEquals(setOf(template), regulation.processes)
        assertEquals(ControlType.ACTIVATION, regulation.controlType)
        assertTrue(context.model.getObjects(Catalysis::class.java).isEmpty())
    }

    @Test
    fun `a regulation of two reactions is a Control for each, and a catalyst activity a Catalysis for each`() {
        val inhibitor = entity<SimpleEntity>(40, "inhibitor")
        val enzyme = entity<EntityWithAccessionedSequence>(41, "enzyme")
        val atp = entity<SimpleEntity>(42, "ATP")
        val shared = regulation<NegativeRegulation>(704, inhibitor)
        val activity = activity(601, enzyme)
        val one = reaction<Reaction>(501, listOf(atp), emptyList(), listOf(activity), listOf(shared))
        val other = reaction<Reaction>(502, listOf(atp), emptyList(), listOf(activity), listOf(shared))

        val first = BioPAXInteractionBuilder(one, context).addReactomeRLEvent()
        val second = BioPAXInteractionBuilder(other, context).addReactomeRLEvent()

        val controls = context.model.getObjects(Control::class.java).filter { it !is Catalysis }
        assertEquals(2, controls.size)
        assertEquals(setOf(setOf(first), setOf(second)), controls.map { it.processes.toSet() }.toSet())
        val catalyses = context.model.getObjects(Catalysis::class.java)
        assertEquals(2, catalyses.size)
        assertEquals(setOf(setOf(first), setOf(second)), catalyses.map { it.processes.toSet() }.toSet())
        // One molecular-function xref for the term, shared
        assertEquals(
            1,
            catalyses
                .flatMap { it.xref }
                .filter { it.db == "GENE ONTOLOGY" }
                .toSet()
                .size,
        )
        // The two share the activity's xrefs, written once
        assertEquals(
            1,
            catalyses
                .flatMap { it.xref }
                .filter { it.db == "Reactome Database ID Release 98" }
                .toSet()
                .size,
        )
    }

    @Test
    fun `a regulation listed twice on a reaction is one Control`() {
        val required = entity<SimpleEntity>(50, "Mg2+")
        // graph-core lists it twice where its link's stoichiometry is 2
        val twice = regulation<Requirement>(705, required)
        val event = reaction<Reaction>(503, emptyList(), emptyList(), regulations = listOf(twice, twice))

        BioPAXInteractionBuilder(event, context).addReactomeRLEvent()

        assertEquals(
            1,
            context.model
                .getObjects(Control::class.java)
                .filter { it !is Catalysis }
                .size,
        )
    }

    @Test
    fun `a regulation's active units are numbered in its own order`() {
        val later = entity<EntityWithAccessionedSequence>(61, "first in the list")
        val earlier = entity<EntityWithAccessionedSequence>(60, "second in the list")
        val event =
            reaction<Reaction>(
                504,
                emptyList(),
                emptyList(),
                regulations = listOf(regulation<PositiveRegulation>(706, entity<SimpleEntity>(62, "cAMP"), listOf(later, earlier))),
            )

        BioPAXInteractionBuilder(event, context).addReactomeRLEvent()

        fun number(name: String) =
            context.model.objects
                .filterIsInstance<org.biopax.paxtools.model.level3.Named>()
                .single { it.displayName == name }
                .uri
                .takeLastWhile { it.isDigit() }
                .toInt()
        assertTrue(number("first in the list") < number("second in the list"))
    }
}
