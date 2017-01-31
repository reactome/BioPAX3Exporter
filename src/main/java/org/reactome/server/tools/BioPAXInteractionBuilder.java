package org.reactome.server.tools;

import org.biopax.paxtools.model.level3.*;
import org.reactome.server.graph.domain.model.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class BioPAXInteractionBuilder {

    private final org.reactome.server.graph.domain.model.ReactionLikeEvent thisRLEvent;

    private org.biopax.paxtools.model.Model thisModel;

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
        BiochemicalReaction bpReaction = thisModel.addNew(BiochemicalReaction.class, BioPAX3Utils.getTypeCount("BiochemicalReaction"));
        bpReaction.setDisplayName(event.getDisplayName());
        if (event.getCatalystActivity() != null) {
            for (CatalystActivity cat : event.getCatalystActivity()) {
                org.biopax.paxtools.model.level3.Catalysis bpCatalysis = addBPCatalyst();
                bpCatalysis.addControlled(bpReaction);
            }
        }

        BioPAX3BasicElementsBuilder elements = new BioPAX3BasicElementsBuilder(event, thisModel, bpReaction);
        elements.addEvidence();
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

