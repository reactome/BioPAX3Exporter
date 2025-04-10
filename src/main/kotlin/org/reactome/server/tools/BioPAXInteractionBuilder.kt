package org.reactome.server.tools;

import java.util.HashMap;
import java.util.Map;

import org.biopax.paxtools.model.level3.*;
import org.reactome.server.graph.domain.model.*;
import org.biopax.paxtools.model.Model;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class BioPAXInteractionBuilder(
    private val thisRLEvent: ReactionLikeEvent?,
    private var thisModel: Model?
) {

    /**
     * Function to add the Reactome ReactionLikeEvent to the BioPAX model
     */
    fun addReactomeRLEvent(): BiochemicalReaction? {
        return addBPReaction(thisRLEvent)
    }

    //////////////////////////////////////////////////////////////////////////////////

    // Private functions

    /**
     * Function to create a BioPAX BiochemicalReaction from the Reactome ReactionLikeEvent given
     *
     * @param event the Reactome ReactionLikeEvent to be created in BioPAX
     *
     * @return the created BioPAX BiochemicalReaction
     */
    private fun addBPReaction(event: ReactionLikeEvent?): BiochemicalReaction? {
        if (event == null) return null

        // Create a new BioPAX BiochemicalReaction
        val bpReaction = thisModel?.addNew(BiochemicalReaction::class.java, BioPAX3Utils.getTypeCount("BiochemicalReaction"))
        
        // Set properties
        bpReaction?.displayName = event.displayName

        // Handle catalyst activities
        event.catalystActivity?.forEach { cat ->
            val bpCatalysis = addBPCatalyst()
            bpCatalysis?.addControlled(bpReaction)
        }

        // Build the "basic elements" (organism, dataSource, etc.)
        val elements = BioPAX3BasicElementsBuilder(event, thisModel!!, bpReaction!!)
        elements.addEvidence()

        return bpReaction
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // add related Biopax classes

    // these are small classes so do not need separate code files

    // Catalysis

    /**
     * Function to create a BioPAX Catalysis
     *
     * @return the BioPAX Catalysis created
     *
     * NOTE: will more than one reaction use the same catalysis ?????
     */
    private fun addBPCatalyst(): Catalysis? {
        return thisModel?.addNew(
            Catalysis::class.java,
            BioPAX3Utils.getTypeCount("Catalysis")
        )
    }
}

