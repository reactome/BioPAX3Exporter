@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.reactome.server.tools
import org.biopax.paxtools.model.level3.*
import org.reactome.server.graph.domain.model.Event
import java.util.*

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class BioPAX3BasicElementsBuilder(
    private val thisReactomeEvent: Event? = null,
    private val context: ConversionContext? = null,
    private var thisBPElement: org.biopax.paxtools.model.level3.Entity? = null,
) {
    private val thisBPModel = context?.model

    /**
     * Function to add the BioSource element to the Entity supplied
     */
    fun addBioSourceInformation() {
        // note this should only get added to a Pathway
        if (thisReactomeEvent == null || thisBPElement !is org.biopax.paxtools.model.level3.Pathway) {
            return
        }
        val src = getBPBioSource() ?: return
        (thisBPElement as org.biopax.paxtools.model.level3.Pathway).organism = src
    }

    /**
     * Reactome as the element's data source: one Provenance per file, as the release's
     * converter wrote it, kept in the context (looking it up among the model's objects
     * read every object in the file each time, and the conversion of a large species took
     * hours).
     */
    fun addReactomeDataSource() {
        val src =
            context!!.once("provenance") {
                thisBPModel!!.addNew(org.biopax.paxtools.model.level3.Provenance::class.java, context.nextId("Provenance")).apply {
                    addName("Reactome")
                    addComment("http://www.reactome.org")
                }
            }
        thisBPElement?.addDataSource(src)
    }

    // private functions

    /**
     * Function to retrieve a BioPAX BioSource element; either finding an existing one or
     * creating a new one if no appropriate element already exists
     *
     * @return BioPAX BioSource element
     */
    private fun getBPBioSource(): org.biopax.paxtools.model.level3.BioSource? {
        // The event's first species, the one BioSource per taxon that entity
        // references share too (T011)
        val species = thisReactomeEvent?.species?.firstOrNull() ?: return null
        return BioPAXXrefBuilder(context!!).bioSource(species)
    }
}
