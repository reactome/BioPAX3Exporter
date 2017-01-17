package org.reactome.server.tools;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.domain.model.Event;


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.biopax.paxtools.model.*;
import org.reactome.server.graph.domain.model.Pathway;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class BioPAXInteraction {

    private final org.reactome.server.graph.domain.model.ReactionLikeEvent thisRLEvent;

    private org.biopax.paxtools.model.Model thisModel;

    /**
     * Construct an instance of the BioPAXInteraction
     */
    BioPAXInteraction() {
        thisModel = null;
        thisRLEvent = null;
    }

    BioPAXInteraction(org.reactome.server.graph.domain.model.ReactionLikeEvent reaction, org.biopax.paxtools.model.Model model) {
        thisRLEvent = reaction;
        thisModel = model;
    }

    org.biopax.paxtools.model.level3.BiochemicalReaction addReactomeRLEvent() {
        return addBPReaction(thisRLEvent);
    }
    //////////////////////////////////////////////////////////////////////////////////

    // Private functions

    /**
     * Adds the given Reactome Reaction to the SBML model as an SBML Reaction.
     * This in turn adds SBML species and SBML compartments.
     *
     * @param event Reaction from ReactomeDB
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

        return bpReaction;
    }

    private org.biopax.paxtools.model.level3.Catalysis addBPCatalyst() {
        return thisModel.addNew(org.biopax.paxtools.model.level3.Catalysis.class, BioPAX3Utils.getTypeCount("Catalysis"));
    }
}

