package org.reactome.server.tools;

import java.util.HashMap;
import java.util.Map;

import org.biopax.paxtools.model.level3.*;
import org.reactome.server.graph.domain.model.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class BioPAXInteractionBuilder {

    private final org.reactome.server.graph.domain.model.ReactionLikeEvent thisRLEvent;

    private org.biopax.paxtools.model.Model thisModel;

    // A cache to store already-created BiochemicalReactions keyed by the Reactome dbId.
    private static Map<Long, BiochemicalReaction> visitedReactions = new HashMap<>();



    /**
     * Construct an instance of the BioPAXInteractionBuilder
     */
    BioPAXInteractionBuilder() {
        thisModel = null;
        thisRLEvent = null;
    }

    /**
     * Construct an instance of the BioPAXInteractionBuilder for the specified
     * ReactionLikeEvent and Model.
     *
     * @param reaction ReactionLikeEvent from ReactomeDB
     * @param model the BioPAX model being constructed from the event
     */
    BioPAXInteractionBuilder(org.reactome.server.graph.domain.model.ReactionLikeEvent reaction, org.biopax.paxtools.model.Model model) {
        thisRLEvent = reaction;
        thisModel = model;
    }

    /**
     * Function to create a BioPAX BiochemicalReaction and relevant information
     *
     * @return the BioPAX BiochemicalReaction
     */
    org.biopax.paxtools.model.level3.BiochemicalReaction addReactomeRLEvent() {
        return addBPReaction(thisRLEvent);
    }
    //////////////////////////////////////////////////////////////////////////////////

    // Private functions

    /**
     * Function to add a BioPAX BiochemicalReaction
     *
     * @param event ReactionLikeEvent from ReactomeDB
     *
     * @return the BioPAX BiochemicalReaction created
     *
     * NOTE: Need to add whole loads of stuff
     */
    private org.biopax.paxtools.model.level3.BiochemicalReaction addBPReaction(org.reactome.server.graph.domain.model.ReactionLikeEvent event) {
        if (event == null) return null;

        // 1) Check if we've already created a BiochemicalReaction for this DB ID
        Long dbId = event.getDbId();
        if (visitedReactions.containsKey(dbId)) {
            // Already processed => re-use the existing BiochemicalReaction
            return visitedReactions.get(dbId);
        }

        // 2) Otherwise, create a NEW BiochemicalReaction
        BiochemicalReaction bpReaction = thisModel.addNew(
                BiochemicalReaction.class, 
                BioPAX3Utils.getTypeCount("BiochemicalReaction")
        );

        // 3) Save to the cache so we never build another for this same event
        visitedReactions.put(dbId, bpReaction);

        // 4) Set the display name (so we only call getDisplayName() this once)
        bpReaction.setDisplayName(event.getDisplayName());

        // 5) Process Catalyst Activity or other properties
        if (event.getCatalystActivity() != null) {
            for (org.reactome.server.graph.domain.model.CatalystActivity cat : event.getCatalystActivity()) {
                // Create a Catalysis object
                org.biopax.paxtools.model.level3.Catalysis bpCatalysis = addBPCatalyst();
                // Link it to this BiochemicalReaction
                bpCatalysis.addControlled(bpReaction);
            }
        }

        // 6) Possibly add references, evidence, etc.
        BioPAX3BasicElementsBuilder elements = new BioPAX3BasicElementsBuilder(event, thisModel, bpReaction);
        elements.addEvidence();

        // 7) Return newly built reaction
        return bpReaction;

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

    private org.biopax.paxtools.model.level3.Catalysis addBPCatalyst() {
        return thisModel.addNew(org.biopax.paxtools.model.level3.Catalysis.class, BioPAX3Utils.getTypeCount("Catalysis"));
    }
}

