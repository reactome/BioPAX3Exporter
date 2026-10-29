package org.reactome.server.tools

import org.biopax.paxtools.model.level3.BiochemicalReaction
import org.biopax.paxtools.model.level3.Degradation
import org.biopax.paxtools.model.level3.Protein
import org.biopax.paxtools.model.level3.RelationshipXref
import org.biopax.paxtools.model.level3.TemplateReaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.reactome.server.graph.domain.model.BlackBoxEvent
import org.reactome.server.graph.domain.model.Disease
import org.reactome.server.graph.domain.model.EntityWithAccessionedSequence
import org.reactome.server.graph.domain.model.GO_BiologicalProcess
import org.reactome.server.graph.domain.model.Pathway
import org.reactome.server.graph.domain.model.Reaction

/**
 * An event's own cross-references as the release's converter writes them (spec 001 T014),
 * on a real Paxtools model: its GO biological process and its diseases.
 */
class BioPAXEventXrefTest {
    private val context = ConversionContext("http://www.reactome.org/biopax/98/1#", 98)

    private fun process(): GO_BiologicalProcess =
        mock(GO_BiologicalProcess::class.java).also {
            whenever(it.dbId).thenReturn(800)
            whenever(it.accession).thenReturn("0006298")
        }

    private fun disease(
        dbId: Long,
        doid: String,
    ): Disease =
        mock(Disease::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.databaseName).thenReturn("DOID")
            whenever(it.identifier).thenReturn(doid)
        }

    private fun reaction(
        dbId: Long,
        process: GO_BiologicalProcess?,
        diseases: List<Disease>,
    ): Reaction =
        mock(Reaction::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn("reaction $dbId")
            whenever(it.goBiologicalProcess).thenReturn(process)
            whenever(it.disease).thenReturn(diseases)
        }

    private fun RelationshipXref.described() = "$db $id ${relationshipType.term.single()}"

    @Test
    fun `a reaction's GO biological process`() {
        val bp = BioPAXInteractionBuilder(reaction(500, process(), emptyList()), context).addReactomeRLEvent() as BiochemicalReaction

        val xref = bp.xref.filterIsInstance<RelationshipXref>().single()
        assertEquals("GENE ONTOLOGY GO:0006298 gene ontology term for cellular process", xref.described())
        assertEquals(
            setOf("MI MI:0359"),
            xref.relationshipType.xref
                .map { "${it.db} ${it.id}" }
                .toSet(),
        )
    }

    @Test
    fun `every disease of a reaction, though one was written before`() {
        val cancer = disease(901, "162")
        val other = disease(902, "5603")
        BioPAXInteractionBuilder(reaction(501, null, listOf(other)), context).addReactomeRLEvent()
        // The release's converter stopped at the first it had already written: 5603 here
        val bp = BioPAXInteractionBuilder(reaction(502, null, listOf(other, cancer)), context).addReactomeRLEvent() as BiochemicalReaction

        assertEquals(
            setOf("DOID 5603 additional information", "DOID 162 additional information"),
            bp.xref
                .filterIsInstance<RelationshipXref>()
                .map { it.described() }
                .toSet(),
        )
    }

    @Test
    fun `a pathway's GO biological process and diseases, shared with its reactions'`() {
        val go = process()
        val cancer = disease(901, "162")
        val child = reaction(503, go, listOf(cancer))
        val pathway =
            mock(Pathway::class.java).also {
                whenever(it.dbId).thenReturn(1)
                whenever(it.displayName).thenReturn("Signaling by NOTCH1 in cancer")
                whenever(it.goBiologicalProcess).thenReturn(go)
                whenever(it.disease).thenReturn(listOf(cancer))
                whenever(it.hasEvent).thenReturn(listOf(child))
            }

        BioPAXPathwayBuilder(pathway, context).addReactomePathway()

        val bpPathway =
            context.model
                .getObjects(org.biopax.paxtools.model.level3.Pathway::class.java)
                .single()
        assertEquals(
            setOf("GENE ONTOLOGY GO:0006298 gene ontology term for cellular process", "DOID 162 additional information"),
            bpPathway.xref
                .filterIsInstance<RelationshipXref>()
                .map { it.described() }
                .toSet(),
        )
        val bpReaction = context.model.getObjects(BiochemicalReaction::class.java).single()
        // Each term's xref written once, shared
        bpPathway.xref.filterIsInstance<RelationshipXref>().forEach { xref ->
            assertSame(xref, bpReaction.xref.single { it.db == xref.db })
        }
    }

    @Test
    fun `an event's disease xref is the one its entities name`() {
        val cancer = disease(901, "162")
        val mutant =
            mock(EntityWithAccessionedSequence::class.java).also {
                whenever(it.dbId).thenReturn(910)
                whenever(it.displayName).thenReturn("NOTCH1 mutant")
                whenever(it.disease).thenReturn(listOf(cancer))
            }
        val event = reaction(504, null, listOf(cancer))
        whenever(event.input).thenReturn(listOf(mutant))

        val bp = BioPAXInteractionBuilder(event, context).addReactomeRLEvent() as BiochemicalReaction

        val entity =
            context.model
                .getObjects(Protein::class.java)
                .ifEmpty {
                    context.model.getObjects(org.biopax.paxtools.model.level3.PhysicalEntity::class.java)
                }.single {
                    it.displayName ==
                        "NOTCH1 mutant"
                }
        assertSame(
            entity.xref.filterIsInstance<RelationshipXref>().single(),
            bp.xref.filterIsInstance<RelationshipXref>().single(),
        )
    }

    @Test
    fun `a TemplateReaction and a Degradation have their GO term and diseases too`() {
        val go = process()
        val cancer = disease(901, "162")
        val gene = mock(EntityWithAccessionedSequence::class.java).also { whenever(it.dbId).thenReturn(920) }

        fun blackBox(
            dbId: Long,
            name: String,
            inputs: List<EntityWithAccessionedSequence>,
            outputs: List<EntityWithAccessionedSequence>,
        ): BlackBoxEvent =
            mock(BlackBoxEvent::class.java).also {
                whenever(it.dbId).thenReturn(dbId)
                whenever(it.displayName).thenReturn(name)
                whenever(it.input).thenReturn(inputs)
                whenever(it.output).thenReturn(outputs)
                whenever(it.goBiologicalProcess).thenReturn(go)
                whenever(it.disease).thenReturn(listOf(cancer))
            }
        val template =
            BioPAXInteractionBuilder(
                blackBox(505, "SOCS3 gene expression", emptyList(), listOf(gene)),
                context,
            ).addReactomeRLEvent()
        val degradation =
            BioPAXInteractionBuilder(
                blackBox(506, "Degradation of SOCS3", listOf(gene), emptyList()),
                context,
            ).addReactomeRLEvent()

        assertEquals(TemplateReaction::class.java, template!!.modelInterface)
        assertEquals(Degradation::class.java, degradation!!.modelInterface)
        for (bp in listOf(template, degradation)) {
            assertEquals(
                setOf("GENE ONTOLOGY GO:0006298 gene ontology term for cellular process", "DOID 162 additional information"),
                bp.xref
                    .filterIsInstance<RelationshipXref>()
                    .map { it.described() }
                    .toSet(),
            )
        }
    }
}
