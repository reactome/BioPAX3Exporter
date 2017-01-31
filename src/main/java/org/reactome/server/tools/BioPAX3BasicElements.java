package org.reactome.server.tools;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Pathway;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.domain.model.Event;


import java.io.*;
import java.util.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class BioPAX3BasicElements {

    private org.reactome.server.graph.domain.model.Event thisReactomeEvent;

    private org.biopax.paxtools.model.Model thisBPModel;

    private org.biopax.paxtools.model.level3.Entity thisBPElement = null;

    BioPAX3BasicElements(org.reactome.server.graph.domain.model.Event event,
                         org.biopax.paxtools.model.Model model,
                         org.biopax.paxtools.model.level3.Entity bpElement) {
        thisReactomeEvent = event;
        thisBPModel = model;
        thisBPElement = bpElement;
    }

    // functions for adding BioSource information

    void addBioSourceInformation() {
        if (thisReactomeEvent == null || !(thisBPElement instanceof org.biopax.paxtools.model.level3.Pathway)) return;
        BioSource src = getBPBioSource();
        ((org.biopax.paxtools.model.level3.Pathway)(thisBPElement)).setOrganism(src);
    }

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

    private org.biopax.paxtools.model.level3.BioSource addBioSource(String species) {
        BioSource src = thisBPModel.addNew(org.biopax.paxtools.model.level3.BioSource.class, BioPAX3Utils.getTypeCount("BioSource"));
        Set<String> name = new TreeSet<String>();
        name.add(species);
        src.setName(name);
        return src;
    }


    void addReactomeDataSource() {
        Set<Provenance> provs = thisBPModel.getObjects(org.biopax.paxtools.model.level3.Provenance.class);
        Provenance src = BioPAX3Utils.getObjectFromSet(provs, "Provenance1");
        // // TODO: 30/01/2017 check that Provenance1 is teh reactome data source 
        if (src == null) {
            src = thisBPModel.addNew(org.biopax.paxtools.model.level3.Provenance.class, BioPAX3Utils.getTypeCount("Provenance"));
            src.addName("Reactome");
            src.addComment("http://www.reactome.org");
        }
        thisBPElement.addDataSource(src);
    }

    void addEvidence() {
        Set<Evidence> evids = thisBPModel.getObjects(org.biopax.paxtools.model.level3.Evidence.class);
        Evidence src = null; // todo do we already have this

        if (src == null) {
            src = thisBPModel.addNew(org.biopax.paxtools.model.level3.Evidence.class, BioPAX3Utils.getTypeCount("Evidence"));
        }
        thisBPElement.addEvidence(src);
    }

 }
