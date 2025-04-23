package org.reactome.server.tools

import java.util.HashMap
import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.*
import org.reactome.server.graph.domain.model.ReactionLikeEvent
import org.reactome.server.graph.domain.model.SimpleEntity
import org.reactome.server.graph.domain.model.EntityWithAccessionedSequence
import org.reactome.server.graph.domain.model.PhysicalEntity as RPhysicalEntity
import org.reactome.server.graph.domain.model.Complex as RComplex
import org.reactome.server.tools.BioPAX3ReferenceUtils

/**
 * Builds a BioPAX BiochemicalReaction for a given Reactome ReactionLikeEvent,
 * mapping inputs → left and outputs → right.  Stoichiometry is left at 1 for
 * now (extend once coefficients are exposed in the domain model).
 */
class BioPAXInteractionBuilder(
    private val thisRLEvent: ReactionLikeEvent?,
    private val thisModel: Model?
) {

    /** cache so that identical PhysicalEntities are reused within a reaction tree */
    private val peCache: MutableMap<Long, org.biopax.paxtools.model.level3.PhysicalEntity> = HashMap()

    // ─────────────────────────────────────────────────────────────────────────────
    //  Public entry point
    // ─────────────────────────────────────────────────────────────────────────────
    fun addReactomeRLEvent(): BiochemicalReaction? = addBPReaction(thisRLEvent)

    // ─────────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────────

    private fun addBPReaction(event: ReactionLikeEvent?): BiochemicalReaction? {
        if (event == null || thisModel == null) return null

        // Create the BiochemicalReaction shell
        val bpReaction = thisModel.addNew(
            BiochemicalReaction::class.java,
            BioPAX3Utils.getTypeCount("BiochemicalReaction")
        )
        bpReaction.displayName = event.displayName
        // Add functional notes from Reactome Summation as BioPAX comment
        bpReaction.addComment(BioPAX3ReferenceUtils.getComment(event.summation))

        // Participants (left/right)
        addParticipants(event, bpReaction)

        // Catalysts
        event.catalystActivity?.forEach { _ ->
            val bpCatalysis = addBPCatalyst()
            bpCatalysis?.addControlled(bpReaction)
        }

        // Evidence / datasource etc.
        BioPAX3BasicElementsBuilder(event, thisModel, bpReaction).addEvidence()
        return bpReaction
    }

    /**
     * Maps Reactome inputs/outputs onto BioPAX left/right sets.
     */
    private fun addParticipants(event: ReactionLikeEvent, rxn: BiochemicalReaction) {
        // LEFT = input
        event.input?.forEach { pe ->
            val bpPe = getOrCreatePhysicalEntity(pe)
            rxn.addLeft(bpPe)
        }

        // RIGHT = output
        event.output?.forEach { pe ->
            val bpPe = getOrCreatePhysicalEntity(pe)
            rxn.addRight(bpPe)
        }
    }

    /** create/reuse BioPAX PhysicalEntity corresponding to a Reactome PhysicalEntity */
    private fun getOrCreatePhysicalEntity(pe: RPhysicalEntity): org.biopax.paxtools.model.level3.PhysicalEntity {
        peCache[pe.dbId]?.let { return it }

        val bpPe: org.biopax.paxtools.model.level3.PhysicalEntity = when (pe) {
            is SimpleEntity -> thisModel!!.addNew(SmallMolecule::class.java,
                BioPAX3Utils.getTypeCount("SmallMolecule"))
            is EntityWithAccessionedSequence -> thisModel!!.addNew(Protein::class.java,
                BioPAX3Utils.getTypeCount("Protein"))
            is RComplex -> thisModel!!.addNew(org.biopax.paxtools.model.level3.Complex::class.java,
                BioPAX3Utils.getTypeCount("Complex"))
            else -> thisModel!!.addNew(org.biopax.paxtools.model.level3.PhysicalEntity::class.java,
                BioPAX3Utils.getTypeCount("PhysicalEntity"))
        }
        bpPe.displayName = pe.displayName
        peCache[pe.dbId] = bpPe
        return bpPe
    }

    private fun addBPCatalyst(): Catalysis? = thisModel?.addNew(
        Catalysis::class.java,
        BioPAX3Utils.getTypeCount("Catalysis")
    )
}
