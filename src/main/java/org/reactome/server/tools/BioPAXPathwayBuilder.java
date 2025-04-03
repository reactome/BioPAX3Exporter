package org.reactome.server.tools;

import java.util.HashMap;
import java.util.Map;

import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.reactome.server.graph.domain.model.*;


import org.reactome.server.graph.domain.model.Pathway;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class BioPAXPathwayBuilder {

    private final org.reactome.server.graph.domain.model.Pathway thisPathway;

    private org.biopax.paxtools.model.Model thisModel;

    // 1) Add a cache (map) for storing Pathway objects you've already built
    private Map<Long, org.biopax.paxtools.model.level3.Pathway> visitedPathways = new HashMap<>();


    /**
     * Construct an instance of the BioPAXPathwayBuilder
     */
    BioPAXPathwayBuilder() {
        thisPathway = null;
        thisModel = null;
    }

    /**
     * Construct an instance of the BioPAXPathwayBuilder for the specified
     * Pathway and Model.
     *
     * @param pathway Pathway from ReactomeDB
     * @param model the BioPAX model being constructed from the Pathway
     */
    BioPAXPathwayBuilder(org.reactome.server.graph.domain.model.Pathway pathway, org.biopax.paxtools.model.Model model) {
        thisPathway = pathway;
        thisModel = model;
    }

    /**
     * Function to add the Reactome Pathway to the BioPAX model
     */
    void addReactomePathway() {
        addReactomePathway(thisPathway);
    }
    //////////////////////////////////////////////////////////////////////////////////

    // Private functions

    /**
     * Function to create a BioPAX pathway from the Reactome pathway given. This will recurse through
     * any child elements.
     *
     * @param pathway the Reactome pathway to be created in BioPAX
     *
     * @return the created BioPAX pathway
     */
    private org.biopax.paxtools.model.level3.Pathway addReactomePathway(org.reactome.server.graph.domain.model.Pathway pathway) {
        if (pathway == null) return null;

        // 2a) Check the cache
        Long dbId = pathway.getDbId();
        if (visitedPathways.containsKey(dbId)) {
            // Already created this Pathway. Reuse the same BioPAX object
            return visitedPathways.get(dbId);
        }
    
        // 2b) Create a new BioPAX Pathway
        org.biopax.paxtools.model.level3.Pathway bpPath =
             thisModel.addNew(org.biopax.paxtools.model.level3.Pathway.class,
                              BioPAX3Utils.getTypeCount("Pathway"));
    
        // 2c) Store it in the cache so we don’t recreate it again
        visitedPathways.put(dbId, bpPath);
    
        // 2d) Set all properties
        bpPath.setDisplayName(pathway.getDisplayName());
        bpPath.addComment(BioPAX3ReferenceUtils.getComment(pathway.getSummation()));
    
        // Build the “basic elements” (organism, dataSource, etc.)
        BioPAX3BasicElementsBuilder elements = new BioPAX3BasicElementsBuilder(pathway, thisModel, bpPath);
        elements.addBioSourceInformation();
        elements.addReactomeDataSource();
        elements.addEvidence();
    
        // 2e) Recurse over child “hasEvent” (subpathways or RLEs)
        addChildPathways(pathway, bpPath);
    
        return bpPath;        
    }


    /**
     * Function to recurse through all the Reactome events associate with the given
     * Reactome Pathway and add information about these to the BioPAX model and the corresponding
     * BioPAX pathway
     *
     * @param pathway the Reactome pathway to be created in BioPAX
     * @param bpPath the BioPAX pathway to which the information is added
     */
    private void addChildPathways(org.reactome.server.graph.domain.model.Pathway pathway,
                                  org.biopax.paxtools.model.level3.Pathway bpPath) {
        if (pathway.getHasEvent() != null) {
            for (Event event : pathway.getHasEvent()) {
                addReactomeEvent(event, bpPath);
            }
        }
    }

    /**
     * Function to add information from a Reactome Event to the BioPAX model and
     * correponding BioPAX pathway
     *
     * @param event the Reactome event to be created in BioPAX
     * @param bpPath the BioPAX pathway to which the information is added
     */
    private void addReactomeEvent(Event event, org.biopax.paxtools.model.level3.Pathway bpPath) {
        PathwayStep step = addBPStep(bpPath);
        bpPath.addPathwayOrder(step);
        org.biopax.paxtools.model.level3.Process childPath = null;
        if (event instanceof Pathway) {
            childPath = addReactomePathway((Pathway) event);
        } else if (event instanceof org.reactome.server.graph.domain.model.ReactionLikeEvent) {
            BioPAXInteractionBuilder thisBPInteract = new BioPAXInteractionBuilder((ReactionLikeEvent) event, thisModel);
            childPath = thisBPInteract.addReactomeRLEvent();
        }

        if (childPath != null) {
            addComponentInformation(bpPath, childPath, step);
        }
    }

    /**
     * Function to create an individual BioPAX pathway
     *
     * @param pathway the Reactome pathway to be created in BioPAX
     *
     * @return the BioPAX pathway created
     */
    private org.biopax.paxtools.model.level3.Pathway addBPPathway(org.reactome.server.graph.domain.model.Pathway pathway) {
        if (pathway == null) return null;
        org.biopax.paxtools.model.level3.Pathway bpPath =
                thisModel.addNew(org.biopax.paxtools.model.level3.Pathway.class, BioPAX3Utils.getTypeCount("Pathway"));
        BioPAX3BasicElementsBuilder elements = new BioPAX3BasicElementsBuilder(pathway, thisModel, bpPath);
        elements.addBioSourceInformation();
        elements.addReactomeDataSource();
        elements.addEvidence();
        bpPath.setDisplayName(pathway.getDisplayName());
        bpPath.addComment(BioPAX3ReferenceUtils.getComment(pathway.getSummation()));
        return bpPath;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////

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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
    private org.biopax.paxtools.model.level3.PathwayStep addBPStep(org.biopax.paxtools.model.level3.Pathway bpPath) {
        if (bpPath == null) return null;
        return thisModel.addNew(org.biopax.paxtools.model.level3.PathwayStep.class, BioPAX3Utils.getTypeCount("PathwayStep"));
    }
}

