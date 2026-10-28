package org.reactome.server.tools

import org.biopax.paxtools.model.level3.BiochemicalReaction
import org.biopax.paxtools.model.level3.Catalysis
import org.biopax.paxtools.model.level3.ConversionDirectionType
import org.biopax.paxtools.model.level3.Degradation
import org.biopax.paxtools.model.level3.Interaction
import org.biopax.paxtools.model.level3.Stoichiometry
import org.biopax.paxtools.model.level3.TemplateReaction
import org.reactome.server.graph.domain.model.BlackBoxEvent
import org.reactome.server.graph.domain.model.ReactionLikeEvent
import org.biopax.paxtools.model.level3.PhysicalEntity as BpPhysicalEntity
import org.reactome.server.graph.domain.model.PhysicalEntity as RPhysicalEntity

/**
 * A reaction as the release's converter writes it (spec 001 T012): its kind, its
 * names, Reactome's two ids and the data source, and its participants:
 * - a TemplateReaction (a black box with no input and one output): its products;
 * - otherwise its inputs on the left and outputs on the right, each once, with a
 *   Stoichiometry where it takes part more than once; and a BiochemicalReaction (not a
 *   Degradation) goes left to right and has its catalysts' EC numbers.
 *
 * Its catalyses are T013's, its literature and evidence T016's.
 */
class BioPAXInteractionBuilder(
    private val thisRLEvent: ReactionLikeEvent?,
    private val context: ConversionContext?,
) {
    private val thisModel = context?.model

    // Each entity once per file, as the release writes it (T011).
    private val entities = context?.let { BioPAXPhysicalEntityBuilder(it) }

    fun addReactomeRLEvent(): Interaction? = addBPReaction(thisRLEvent)

    private fun addBPReaction(event: ReactionLikeEvent?): Interaction? {
        if (event == null || thisModel == null) return null

        val type = kindOf(event)
        val bpReaction = thisModel.addNew(type, context!!.nextId(type.simpleName))
        BioPAX3Utils.addNames(event.name, event.displayName, bpReaction)
        // Add functional notes from Reactome Summation as BioPAX comment
        bpReaction.addComment(BioPAX3ReferenceUtils.getComment(event.summation))

        if (bpReaction is TemplateReaction) {
            // Its products only, and no catalysis: the release's converter returned here.
            event.output?.forEach { output ->
                val product = entities!!.physicalEntity(output)
                bpReaction.addProduct(product)
                // Paxtools adds a product to the participants too, and writes both; a
                // product is a participant by BioPAX's own definition, and the release
                // wrote it only as a product.
                bpReaction.removeParticipant(product)
            }
        } else {
            addParticipants(event, bpReaction as org.biopax.paxtools.model.level3.Conversion)
            if (bpReaction is BiochemicalReaction) {
                bpReaction.conversionDirection = ConversionDirectionType.LEFT_TO_RIGHT
                // Each catalyst's molecular function's EC number; the graph keeps one per
                // function where the release had several (differences.md).
                event.catalystActivity?.forEach { ca -> ca.activity?.ecNumber?.let { bpReaction.addECNumber(it) } }
            }
            // Catalysts
            event.catalystActivity?.forEach { _ ->
                val bpCatalysis = addBPCatalyst()
                bpCatalysis?.addControlled(bpReaction)
            }
        }
        BioPAXXrefBuilder(context).addReactomeIds(event, bpReaction)
        BioPAX3BasicElementsBuilder(event, context, bpReaction).addReactomeDataSource()

        // Evidence / datasource etc.
        BioPAX3BasicElementsBuilder(event, context, bpReaction).addEvidence()
        return bpReaction
    }

    /**
     * The release's converter's kinds: a black box with one input, no output and
     * "degradation" in its name is a Degradation; one with no input and one output, a
     * TemplateReaction; any other reaction a BiochemicalReaction.
     */
    private fun kindOf(event: ReactionLikeEvent): Class<out Interaction> {
        if (event is BlackBoxEvent) {
            val inputs = event.input ?: emptyList()
            val outputs = event.output ?: emptyList()
            if (inputs.size == 1 && outputs.isEmpty() && event.displayName.lowercase().contains("degradation")) {
                return Degradation::class.java
            }
            if (inputs.isEmpty() && outputs.size == 1) return TemplateReaction::class.java
        }
        return BiochemicalReaction::class.java
    }

    /**
     * Inputs on the left and outputs on the right, each once, with a Stoichiometry (a
     * new one each time) where the entity takes part more than once: the graph lists a
     * participant once for each time it takes part, as the release's database did.
     */
    private fun addParticipants(
        event: ReactionLikeEvent,
        conversion: org.biopax.paxtools.model.level3.Conversion,
    ) {
        counted(event.input).forEach { (entity, count) ->
            val bpEntity = entities!!.physicalEntity(entity)
            if (count > 1) conversion.addParticipantStoichiometry(stoichiometry(bpEntity, count))
            conversion.addLeft(bpEntity)
        }
        counted(event.output).forEach { (entity, count) ->
            val bpEntity = entities!!.physicalEntity(entity)
            if (count > 1) conversion.addParticipantStoichiometry(stoichiometry(bpEntity, count))
            conversion.addRight(bpEntity)
        }
    }

    // Each entity once, in the order it first appears, with the times it appears.
    private fun counted(participants: List<RPhysicalEntity>?): List<Pair<RPhysicalEntity, Int>> {
        val counts = LinkedHashMap<Long, Pair<RPhysicalEntity, Int>>()
        participants?.forEach { entity ->
            val seen = counts[entity.dbId]
            counts[entity.dbId] = entity to (seen?.second ?: 0) + 1
        }
        return counts.values.toList()
    }

    private fun stoichiometry(
        bpEntity: BpPhysicalEntity,
        count: Int,
    ): Stoichiometry =
        thisModel!!.addNew(Stoichiometry::class.java, context!!.nextId("Stoichiometry")).apply {
            stoichiometricCoefficient = count.toFloat()
            physicalEntity = bpEntity
        }

    private fun addBPCatalyst(): Catalysis? =
        thisModel?.addNew(
            Catalysis::class.java,
            context!!.nextId("Catalysis"),
        )
}
