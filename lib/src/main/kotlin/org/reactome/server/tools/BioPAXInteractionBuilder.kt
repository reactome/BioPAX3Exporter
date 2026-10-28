@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.reactome.server.tools

import org.biopax.paxtools.model.level3.*
import org.reactome.server.graph.domain.model.ReactionLikeEvent
import org.reactome.server.tools.BioPAX3ReferenceUtils

/**
 * Builds a BioPAX BiochemicalReaction for a given Reactome ReactionLikeEvent,
 * mapping inputs → left and outputs → right.  Stoichiometry is left at 1 for
 * now (extend once coefficients are exposed in the domain model).
 */
class BioPAXInteractionBuilder(
    private val thisRLEvent: ReactionLikeEvent?,
    private val context: ConversionContext?,
) {
    private val thisModel = context?.model

    // Each entity once per file, as the release writes it (T011).
    private val entities = context?.let { BioPAXPhysicalEntityBuilder(it) }

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
        val bpReaction =
            thisModel.addNew(
                BiochemicalReaction::class.java,
                context!!.nextId("BiochemicalReaction"),
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
        BioPAX3BasicElementsBuilder(event, context, bpReaction).addEvidence()
        return bpReaction
    }

    /**
     * Maps Reactome inputs/outputs onto BioPAX left/right sets.
     */
    private fun addParticipants(
        event: ReactionLikeEvent,
        rxn: BiochemicalReaction,
    ) {
        // LEFT = input
        event.input?.forEach { pe ->
            rxn.addLeft(entities!!.physicalEntity(pe))
        }

        // RIGHT = output
        event.output?.forEach { pe ->
            rxn.addRight(entities!!.physicalEntity(pe))
        }
    }

    private fun addBPCatalyst(): Catalysis? =
        thisModel?.addNew(
            Catalysis::class.java,
            context!!.nextId("Catalysis"),
        )
}
