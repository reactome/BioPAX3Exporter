@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.reactome.server.tools

import org.biopax.paxtools.io.SimpleIOHandler
import org.biopax.paxtools.model.*
import org.reactome.server.graph.domain.model.*
import java.io.*

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class WriteBioPAX3 {
    private val thisPathway: Pathway?
    private val thisListEvents: List<Event>?
    private var parentPathway: Pathway? = null
    private var stepNum: Int = 0
    private var pathwayNum: Int = 0
    private var biornNum: Int = 0
    private var catNum: Int = 0
    private var useEventOf: Boolean = true
    private var thisModel: Model? = null

    /** The release, as the ids' base names it. */
    var dbVersion: Int = 0
        private set

    /** The xml:base of the conversion's ids. */
    var xmlBase: String = ""
        private set

    /**
     * Construct an instance of the WriteBioPAX3
     */
    constructor() {
        thisPathway = null
        thisListEvents = null
        parentPathway = null
        xmlBase = "https://www.reactome.org/biopax/#"
    }

    /**
     * Construct an instance of the WriteBioPAX3 for the specified
     * Pathway.
     *
     * @param pathway  Pathway from ReactomeDB
     */
    constructor(pathway: Pathway) {
        thisPathway = pathway
        thisListEvents = null
        parentPathway = null
        xmlBase = "https://www.reactome.org/biopax/$dbVersion/${pathway.dbId}#"
    }

    /**
     * Construct an instance of the WriteBioPAX3 for the specified
     * Pathway.
     *
     * @param pathway Pathway from ReactomeDB
     * @param version Integer - version number of the database
     */
    constructor(pathway: Pathway, version: Int) {
        thisPathway = pathway
        thisListEvents = null
        parentPathway = null
        dbVersion = version
        xmlBase = "https://www.reactome.org/biopax/$dbVersion/${pathway.dbId}#"
    }

    /**
     * Construct an instance of the WriteBioPAX3 for multiple pathways of a species
     *
     * @param species Species from ReactomeDB
     * @param version Integer - version number of the database
     */
    constructor(species: Species, version: Int) {
        thisPathway = null
        thisListEvents = null
        parentPathway = null
        dbVersion = version
        xmlBase = "https://www.reactome.org/biopax/$dbVersion/species/${species.dbId}#"
    }

    /**
     * Create the BioPAX model using the Reactome Pathway specified in the constructor.
     */
    fun createModel() {
        val context = ConversionContext(xmlBase, dbVersion)
        thisModel = context.model
        val thisBPPath = BioPAXPathwayBuilder(thisPathway!!, context)
        thisBPPath.addReactomePathway()
    }

    /**
     * Create the BioPAX model of a species: its top-level events (SpeciesEvents), each
     * written with all it holds, once in the file, and no header pathway (T015).
     */
    fun createModelForSpecies(events: List<Event>) {
        val context = ConversionContext(xmlBase, dbVersion)
        thisModel = context.model
        val builder = BioPAXPathwayBuilder(null, context)
        events.forEach { builder.addEvent(it) }
        builder.addStepLinks()
    }

    // /////////////////////////////////////////////////////////////////////////////////

    // functions to output resulting document

    /**
     * Write the Biopax Model to std output.
     */
    fun toStdOut() {
        val os = ByteArrayOutputStream()
        val out = SimpleIOHandler()
        out.convertToOWL(thisModel, os)
        val output =
            try {
                String(os.toByteArray(), Charsets.UTF_8)
            } catch (e: Exception) {
                "failed to write"
            }
        println(output)
    }

    /**
     * Write the BioPAX Model to a file.
     *
     * @param output File to use.
     */
    fun toFile(output: File) {
        val os = ByteArrayOutputStream()
        val out = SimpleIOHandler()
        out.convertToOWL(thisModel, os)
        try {
            if (!output.exists()) {
                output.createNewFile()
            }
            FileOutputStream(output).use { fop ->
                fop.write(os.toByteArray())
                fop.flush()
            }
        } catch (e: Exception) {
            println("failed to write ${output.name}")
        }
    }

    /**
     * Write the BioPAX Model to a String.
     *
     * @return String representing the BioPAX Model.
     */
    override fun toString(): String {
        val os = ByteArrayOutputStream()
        val out = SimpleIOHandler()
        out.convertToOWL(thisModel, os)
        return try {
            String(os.toByteArray(), Charsets.UTF_8)
        } catch (e: Exception) {
            "failed to write"
        }
    }

    // ////////////////////////////////////////////////////////////////////////////////

    // functions to facilitate testing

    /**
     * Gets the BioPAX model created
     *
     * @return thisModel BioPAX Model created
     */
    fun getModel(): Model? = thisModel
}
