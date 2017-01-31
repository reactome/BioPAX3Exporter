package org.reactome.server.tools;

import org.biopax.paxtools.model.level3.*;


import java.util.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class BioPAX3BasicElementsBuilder {

    private final org.reactome.server.graph.domain.model.Event thisReactomeEvent;

    private org.biopax.paxtools.model.Model thisBPModel;

    private org.biopax.paxtools.model.level3.Entity thisBPElement = null;

    /**
     * Constructor for the class to build basic BioPAX elements
     *
     * @param event Reactome Event
     * @param model global BioPAX Model being constructed
     * @param bpElement BioPAX Entity to which new elements are added
     */
    BioPAX3BasicElementsBuilder(org.reactome.server.graph.domain.model.Event event,
                         org.biopax.paxtools.model.Model model,
                         org.biopax.paxtools.model.level3.Entity bpElement) {
        thisReactomeEvent = event;
        thisBPModel = model;
        thisBPElement = bpElement;
    }

    // public functions

    /**
     * Function to add the BioSource element to the Entity supplied
     */
    void addBioSourceInformation() {
        // note this should only get added to a Pathway
        if (thisReactomeEvent == null || !(thisBPElement instanceof org.biopax.paxtools.model.level3.Pathway)) {
            return;
        }
        BioSource src = getBPBioSource();
        ((org.biopax.paxtools.model.level3.Pathway)(thisBPElement)).setOrganism(src);
    }

    /**
     * Function to add the Reacome Database as a BioPAX Provenance element
     */
    void addReactomeDataSource() {
        Set<Provenance> provs = thisBPModel.getObjects(org.biopax.paxtools.model.level3.Provenance.class);
        Provenance src = BioPAX3Utils.getObjectFromSet(provs, "Provenance1");
        // // TODO: 30/01/2017 check that Provenance1 is the reactome data source
        if (src == null) {
            src = thisBPModel.addNew(org.biopax.paxtools.model.level3.Provenance.class, BioPAX3Utils.getTypeCount("Provenance"));
            src.addName("Reactome");
            src.addComment("http://www.reactome.org");
        }
        thisBPElement.addDataSource(src);
    }

    /**
     * Function to add a BioPAX Evidence element (creating it if necessary)
     * and linking it to the BioPAX Entity supplied
     */
    void addEvidence() {
        Set<Evidence> evids = thisBPModel.getObjects(org.biopax.paxtools.model.level3.Evidence.class);
        Evidence src = null; // todo do we already have this

        if (src == null) {
            src = thisBPModel.addNew(org.biopax.paxtools.model.level3.Evidence.class, BioPAX3Utils.getTypeCount("Evidence"));
        }
        thisBPElement.addEvidence(src);
    }

    // private functions

    /**
     * Function to retrieve a BioPAX BioSource element; either finding an existing one or
     * creating a new one if no appropriate element already exists
     *
     * @return BioPAX BioSource element
     */
    private org.biopax.paxtools.model.level3.BioSource getBPBioSource() {
        String species = thisReactomeEvent.getSpeciesName();
        // if the BioSource is already in the model we want to use that one
        Set<BioSource> sources = thisBPModel.getObjects(org.biopax.paxtools.model.level3.BioSource.class);
        BioSource src = BioPAX3Utils.getObjectFromSetByName(sources, species);
        if (src == null) {
            // we havent found it/ add a new one
            src = addBioSource(species);
        }
        return src;
    }

    /**
     * Function to create a new BioPAX BioSource element within the global model using the species supplied
     *
     * @param species String representing the name of the biological species Reactome Species/BioPAX BioSource
     *
     * @return BioPAX BioSource element created
     */
    private org.biopax.paxtools.model.level3.BioSource addBioSource(String species) {
        BioSource src = thisBPModel.addNew(org.biopax.paxtools.model.level3.BioSource.class, BioPAX3Utils.getTypeCount("BioSource"));
        Set<String> name = new TreeSet<String>();
        name.add(species);
        src.setName(name);
        return src;
    }


 }
