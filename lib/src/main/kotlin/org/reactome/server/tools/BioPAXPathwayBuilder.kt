package org.reactome.server.tools

import org.biopax.paxtools.model.level3.Interaction
import org.biopax.paxtools.model.level3.PathwayStep
import org.biopax.paxtools.model.level3.Process
import org.reactome.server.graph.domain.model.Event
import org.reactome.server.graph.domain.model.Pathway
import org.reactome.server.graph.domain.model.ReactionLikeEvent

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 *
 * Events as the release's converter writes them into one file (spec 001 T015): each
 * event once, shared by every pathway that holds it, kept in the [ConversionContext].
 *
 * A pathway is made, then each of its events (its components, each with its own
 * PathwayStep, one per event however many pathways hold it), then its organism and its
 * own properties: names, Reactome's ids, its summations and literature, GO biological
 * process, its evidence if inferred, data source, curation and diseases (T016). Once
 * every event is written, [addStepLinks] gives each step its next steps and its event's
 * catalyses and controls, as the converter's last pass does.
 */
class BioPAXPathwayBuilder(
    private val thisPathway: Pathway?,
    private val context: ConversionContext?,
) {
    private val thisModel = context?.model

    /** The pathway given, all it holds, and its steps' links. */
    fun addReactomePathway() {
        if (thisPathway == null || context == null) return
        addEvent(thisPathway)
        addStepLinks()
    }

    /**
     * An event as it is written into this file, once: a pathway with all it holds, or a
     * reaction. Nothing for any other kind of event.
     */
    fun addEvent(event: Event): Process? =
        when (event) {
            is Pathway -> {
                addPathway(event)
            }

            is ReactionLikeEvent -> {
                context!!.once("event:${event.dbId}") { BioPAXInteractionBuilder(event, context).addReactomeRLEvent()!! }
            }

            else -> {
                null
            }
        }

    private fun addPathway(pathway: Pathway): org.biopax.paxtools.model.level3.Pathway =
        context!!.onceKeptFirst(
            "event:${pathway.dbId}",
            // Made and kept first, as the converter makes it, and its events next
            { thisModel!!.addNew(org.biopax.paxtools.model.level3.Pathway::class.java, context.nextId("Pathway")) },
        ) { bpPath ->
            pathway.hasEvent?.forEach { child ->
                val bpChild = addEvent(child) ?: return@forEach
                bpPath.addPathwayComponent(bpChild)
                bpPath.addPathwayOrder(stepFor(child, bpChild))
            }
            val elements = BioPAX3BasicElementsBuilder(pathway, context, bpPath)
            elements.addBioSourceInformation()
            // Then what every event has, in the converter's order
            BioPAX3Utils.addNames(pathway.name, pathway.displayName, bpPath)
            val xrefs = BioPAXXrefBuilder(context)
            val literature = BioPAXLiteratureBuilder(context)
            xrefs.addReactomeIds(pathway, bpPath)
            literature.addSummations(pathway, bpPath)
            literature.addLiterature(pathway, bpPath)
            xrefs.addBiologicalProcess(pathway, bpPath)
            literature.addEvidence(pathway, bpPath)
            elements.addReactomeDataSource()
            literature.addCuration(pathway, bpPath)
            xrefs.addDiseases(pathway, bpPath)
        }

    // One step for each event, made the first time a pathway holds it; its process is the event.
    private fun stepFor(
        event: Event,
        bpEvent: Process,
    ): PathwayStep =
        context!!
            .steps
            .getOrPut(event.dbId) {
                event to
                    thisModel!!.addNew(PathwayStep::class.java, context.nextId("PathwayStep")).apply {
                        addStepProcess(bpEvent)
                    }
            }.second

    /**
     * Each step's links, once every event is written: the step of each event that precedes
     * its event gets it as a next step, and it gets its event's catalyses and controls as
     * processes. An event with no step here (none of this file's pathways holds it) is
     * linked to nothing, as the converter left it.
     */
    fun addStepLinks() {
        val steps = context!!.steps
        steps.values.forEach { (event, step) ->
            event.precedingEvent?.forEach { preceding -> steps[preceding.dbId]?.second?.addNextStep(step) }
            step.stepProcess
                .filterIsInstance<Interaction>()
                .flatMap { it.controlledOf }
                .forEach { step.addStepProcess(it) }
        }
    }
}
