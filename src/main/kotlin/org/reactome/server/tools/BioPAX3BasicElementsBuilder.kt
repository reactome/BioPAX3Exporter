package org.reactome.server.tools;

import org.biopax.paxtools.model.level3.*;
import org.reactome.server.graph.domain.model.Event;

import java.util.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class BioPAX3BasicElementsBuilder(
    private val thisReactomeEvent: Event? = null,
    private val thisBPModel: org.biopax.paxtools.model.Model? = null,
    private var thisBPElement: org.biopax.paxtools.model.level3.Entity? = null
) {

    /**
     * Function to add the BioSource element to the Entity supplied
     */
    fun addBioSourceInformation() {
        // note this should only get added to a Pathway
        if (thisReactomeEvent == null || thisBPElement !is org.biopax.paxtools.model.level3.Pathway) {
            return;
        }
        val src = getBPBioSource();
        (thisBPElement as org.biopax.paxtools.model.level3.Pathway).organism = src;
    }

    /**
     * Function to add the Reacome Database as a BioPAX Provenance element
     */
    fun addReactomeDataSource() {
        val provs = thisBPModel?.getObjects(org.biopax.paxtools.model.level3.Provenance::class.java);
        var src = BioPAX3Utils.getObjectFromSet(provs, "Provenance1");
        // // TODO: 30/01/2017 check that Provenance1 is the reactome data source
        if (src == null) {
            src = thisBPModel?.addNew(org.biopax.paxtools.model.level3.Provenance::class.java, BioPAX3Utils.getTypeCount("Provenance"));
            src?.addName("Reactome");
            src?.addComment("http://www.reactome.org");
        }
        thisBPElement?.addDataSource(src);
    }

    /**
     * Function to add a BioPAX Evidence element (creating it if necessary)
     * and linking it to the BioPAX Entity supplied
     */
    fun addEvidence() {
        val evids = thisBPModel?.getObjects(org.biopax.paxtools.model.level3.Evidence::class.java);
        var src: Evidence? = null; // todo do we already have this

        if (src == null) {
            src = thisBPModel?.addNew(org.biopax.paxtools.model.level3.Evidence::class.java, BioPAX3Utils.getTypeCount("Evidence"));
        }
        thisBPElement?.addEvidence(src);
    }

    // private functions

    /**
     * Function to retrieve a BioPAX BioSource element; either finding an existing one or
     * creating a new one if no appropriate element already exists
     *
     * @return BioPAX BioSource element
     */
    private fun getBPBioSource(): org.biopax.paxtools.model.level3.BioSource {
        val species = thisReactomeEvent?.speciesName ?: ""
        // if the BioSource is already in the model we want to use that one
        val sources = thisBPModel?.getObjects(org.biopax.paxtools.model.level3.BioSource::class.java) ?: emptySet()
        var src = BioPAX3Utils.getObjectFromSetByName(sources, species)
        if (src == null) {
            // we havent found it/ add a new one
            src = addBioSource(species)
        }
        return src!!
    }

    /**
     * Function to create a new BioPAX BioSource element within the global model using the species supplied
     *
     * @param species String representing the name of the biological species Reactome Species/BioPAX BioSource
     *
     * @return BioPAX BioSource element created
     */
    private fun addBioSource(species: String): org.biopax.paxtools.model.level3.BioSource {
        val src = thisBPModel?.addNew(org.biopax.paxtools.model.level3.BioSource::class.java, BioPAX3Utils.getTypeCount("BioSource"))!!
        val name = TreeSet<String>()
        name.add(species)
        src.name = name
        return src
    }
}
