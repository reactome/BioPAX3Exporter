@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.reactome.server.tools

import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.*
import org.biopax.paxtools.model.level3.Process
import org.reactome.server.graph.domain.model.*
import org.reactome.server.graph.domain.model.Pathway
import java.util.HashMap

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class BioPAXPathwayBuilder(
    private val thisPathway: Pathway?,
    private var thisModel: Model?,
) {
    // 1) Add a cache (map) for storing Pathway objects you've already built
    private val visitedPathways = HashMap<Long, org.biopax.paxtools.model.level3.Pathway>()

    /**
     * Function to add the Reactome Pathway to the BioPAX model
     */
    fun addReactomePathway() {
        addReactomePathway(thisPathway)
    }
    // ////////////////////////////////////////////////////////////////////////////////

    // Private functions

    /**
     * Function to create a BioPAX pathway from the Reactome pathway given. This will recurse through
     * any child elements.
     *
     * @param pathway the Reactome pathway to be created in BioPAX
     *
     * @return the created BioPAX pathway
     */
    private fun addReactomePathway(pathway: Pathway?): org.biopax.paxtools.model.level3.Pathway? {
        if (pathway == null) return null

        // 2a) Check the cache
        val dbId = pathway.dbId
        if (visitedPathways.containsKey(dbId)) {
            // Already created this Pathway. Reuse the same BioPAX object
            return visitedPathways[dbId]
        }

        // 2b) Create a new BioPAX Pathway
        val bpPath = thisModel?.addNew(org.biopax.paxtools.model.level3.Pathway::class.java, BioPAX3Utils.getTypeCount("Pathway"))

        // 2c) Store it in the cache so we don't recreate it again
        bpPath?.let { visitedPathways[dbId] = it }

        // 2d) Set all properties
        bpPath?.displayName = pathway.displayName
        bpPath?.addComment(BioPAX3ReferenceUtils.getComment(pathway.summation))

        // Build the "basic elements" (organism, dataSource, etc.)
        val elements = BioPAX3BasicElementsBuilder(pathway, thisModel!!, bpPath!!)
        elements.addBioSourceInformation()
        elements.addReactomeDataSource()
        elements.addEvidence()

        // 2e) Recurse over child "hasEvent" (subpathways or RLEs)
        addChildPathways(pathway, bpPath)

        return bpPath
    }

    /**
     * Function to recurse through all the Reactome events associate with the given
     * Reactome Pathway and add information about these to the BioPAX model and the corresponding
     * BioPAX pathway
     *
     * @param pathway the Reactome pathway to be created in BioPAX
     * @param bpPath the BioPAX pathway to which the information is added
     */
    private fun addChildPathways(
        pathway: Pathway,
        bpPath: org.biopax.paxtools.model.level3.Pathway,
    ) {
        pathway.hasEvent?.forEach { event ->
            addReactomeEvent(event, bpPath)
        }
    }

    /**
     * Function to add information from a Reactome Event to the BioPAX model and
     * correponding BioPAX pathway
     *
     * @param event the Reactome event to be created in BioPAX
     * @param bpPath the BioPAX pathway to which the information is added
     */
    private fun addReactomeEvent(
        event: Event,
        bpPath: org.biopax.paxtools.model.level3.Pathway,
    ) {
        val step = addBPStep(bpPath)
        step?.let { bpPath.addPathwayOrder(it) }
        var childPath: org.biopax.paxtools.model.level3.Process? = null
        when (event) {
            is Pathway -> childPath = addReactomePathway(event)
            is ReactionLikeEvent -> {
                val thisBPInteract = BioPAXInteractionBuilder(event, thisModel!!)
                childPath = thisBPInteract.addReactomeRLEvent()
            }
            else -> {} // Handle other event types if needed
        }

        childPath?.let { addComponentInformation(bpPath, it, step!!) }
    }

    /**
     * Function to create an individual BioPAX pathway
     *
     * @param pathway the Reactome pathway to be created in BioPAX
     *
     * @return the BioPAX pathway created
     */
    private fun addBPPathway(pathway: Pathway?): org.biopax.paxtools.model.level3.Pathway? {
        if (pathway == null) return null
        val bpPath = thisModel?.addNew(org.biopax.paxtools.model.level3.Pathway::class.java, BioPAX3Utils.getTypeCount("Pathway"))
        bpPath?.let {
            val elements = BioPAX3BasicElementsBuilder(pathway, thisModel!!, it)
            elements.addBioSourceInformation()
            elements.addReactomeDataSource()
            elements.addEvidence()
            it.displayName = pathway.displayName
            it.addComment(BioPAX3ReferenceUtils.getComment(pathway.summation))
        }
        return bpPath
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////

    // add other information to Pathway

    // pathwayComponent

    /**
     * Function to associate a BioPAX Pathway with it's pathwayComponent elements
     *
     * BioPAX creates links between Pathways and the steps taken when the process
     * is activated by adding pathwayComponent and pathwayOrder to the Pathway.
     * The pathwayOrder referneces a PathwayStep which refers back to the pathwayComponent
     *
     * @param bpPath the parent BioPAX pathway to which the information should be added
     * @param childPath the child BioPAX Pathway that is the pathwayComponent
     * @param step the BioPAX PathwayStep associated with this component
     */
    private fun addComponentInformation(
        bpPath: org.biopax.paxtools.model.level3.Pathway,
        childPath: org.biopax.paxtools.model.level3.Process,
        step: PathwayStep,
    ) {
        bpPath.addPathwayComponent(childPath)
        step.addStepProcess(childPath)
        if (childPath is org.biopax.paxtools.model.level3.BiochemicalReaction && childPath.controlledOf.isNotEmpty()) {
            childPath.controlledOf.forEach { p ->
                step.addStepProcess(p)
            }
        }
    }

    // pathwayOrder
    // this is added in the addReactomeEvent function as it needs to enter the PathwayStep information as well

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////

    // add related Biopax classes

    // these are small classes so do not need separate code files

    // PathwayStep

    /**
     * Function to create a PathwayStep to be associated with the BioPAX pathway
     *
     * @param bpPath the BioPAX pathway corresponding to the PathwayStep
     *
     * @return the PathwayStep created
     *
     * NOTE: The argument is unused but I suspect I will need it as in the BioSource creation below
     */
    private fun addBPStep(bpPath: org.biopax.paxtools.model.level3.Pathway?): PathwayStep? {
        if (bpPath == null) return null
        return thisModel?.addNew(org.biopax.paxtools.model.level3.PathwayStep::class.java, BioPAX3Utils.getTypeCount("PathwayStep"))
    }
}
