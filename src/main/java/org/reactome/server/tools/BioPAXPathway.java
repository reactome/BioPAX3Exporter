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
import java.util.Set;
import java.util.TreeSet;

import org.biopax.paxtools.model.*;
import org.reactome.server.graph.domain.model.Pathway;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class BioPAXPathway {

    private final org.reactome.server.graph.domain.model.Pathway thisPathway;

    private org.biopax.paxtools.model.Model thisModel;

    /**
     * Construct an instance of the BioPAXPathway
     */
    BioPAXPathway() {
        thisPathway = null;
        thisModel = null;
    }

    /**
     * Construct an instance of the BioPAXPathway for the specified
     * Pathway.
     *
     * @param pathway Pathway from ReactomeDB
     */
    BioPAXPathway(org.reactome.server.graph.domain.model.Pathway pathway, org.biopax.paxtools.model.Model model) {
        thisPathway = pathway;
        thisModel = model;
    }

    void addReactomePathway() {
        addReactomePathway(thisPathway);
    }
    //////////////////////////////////////////////////////////////////////////////////

    // Private functions
    private org.biopax.paxtools.model.level3.Pathway addReactomePathway(org.reactome.server.graph.domain.model.Pathway pathway) {
        org.biopax.paxtools.model.level3.Pathway bpPath = addBPPathway(pathway);
        if (bpPath != null) {
            addChildPathways(pathway, bpPath);
        }
        return bpPath;
    }


    private void addChildPathways(org.reactome.server.graph.domain.model.Pathway pathway,
                                  org.biopax.paxtools.model.level3.Pathway bpPath) {
        if (pathway.getHasEvent() != null) {
            for (Event event : pathway.getHasEvent()) {
                addReactomeEvent(event, bpPath);
            }
        }
    }

    private void addReactomeEvent(Event event, org.biopax.paxtools.model.level3.Pathway bpPath) {
        PathwayStep step = addBPStep(bpPath);
        bpPath.addPathwayOrder(step);
        org.biopax.paxtools.model.level3.Process childPath = null;
        if (event instanceof Pathway) {
            childPath = addReactomePathway((Pathway) event);
        } else if (event instanceof org.reactome.server.graph.domain.model.ReactionLikeEvent) {
            BioPAXInteraction thisBPInteract = new BioPAXInteraction((ReactionLikeEvent) event, thisModel);
            childPath = thisBPInteract.addReactomeRLEvent();
        }

        if (childPath != null) {
            addComponentInformation(bpPath, childPath, step);
        }
    }

    private org.biopax.paxtools.model.level3.Pathway addBPPathway(org.reactome.server.graph.domain.model.Pathway pathway) {
        if (pathway == null) return null;
        org.biopax.paxtools.model.level3.Pathway bpPath =
                thisModel.addNew(org.biopax.paxtools.model.level3.Pathway.class, BioPAX3Utils.getTypeCount("Pathway"));

        addBioSourceInformation(bpPath, pathway.getSpeciesName());
        bpPath.setDisplayName(pathway.getDisplayName());

        return bpPath;
    }

    // add other information to Pathway

    // organism
    private void addBioSourceInformation(org.biopax.paxtools.model.level3.Pathway bpPath, String species) {
        BioSource src = addBPBioSource(bpPath, species);
        bpPath.setOrganism(src);
    }



    // pathwayComponent
    private void addComponentInformation(org.biopax.paxtools.model.level3.Pathway bpPath,
                                         org.biopax.paxtools.model.level3.Process childPath,
                                         PathwayStep step) {
        bpPath.addPathwayComponent(childPath);
        step.addStepProcess(childPath);
        if (childPath instanceof org.biopax.paxtools.model.level3.BiochemicalReaction &&
                childPath.getControlledOf().size() > 0) {
            for (Process p : childPath.getControlledOf()) {
                step.addStepProcess(p);
            }
        }
    }

    // pathwayOrder
    // this is added in the addReactomeEvent function as it needs to enter the PathwayStep information as well


    // add related Biopax classes

    // PathwayStep

    private org.biopax.paxtools.model.level3.PathwayStep addBPStep(org.biopax.paxtools.model.level3.Pathway bpPath) {
        if (bpPath == null) return null;
        return thisModel.addNew(org.biopax.paxtools.model.level3.PathwayStep.class, BioPAX3Utils.getTypeCount("PathwayStep"));
    }

    // Biosource
    private org.biopax.paxtools.model.level3.BioSource addBPBioSource(org.biopax.paxtools.model.level3.Pathway bpPath,
                                                                      String species) {
        if (bpPath == null) return null;
        // if the BioSource is already in the model we want to use that one
        Set<BioSource> sources = thisModel.getObjects(org.biopax.paxtools.model.level3.BioSource.class);
        BioSource src = BioPAX3Utils.getObjectFromSetByName(sources, species);
        if (src == null) {
            // we havent found it/ add a new one
            src = addBioSource(bpPath, species);
        }
        return src;
    }

    private org.biopax.paxtools.model.level3.BioSource addBioSource(org.biopax.paxtools.model.level3.Pathway bpPath,
                                                                      String species) {
        BioSource src = thisModel.addNew(org.biopax.paxtools.model.level3.BioSource.class, BioPAX3Utils.getTypeCount("BioSource"));
        Set<String> name = new TreeSet<String>();
        name.add(species);
        src.setName(name);
        return src;
    }
}

