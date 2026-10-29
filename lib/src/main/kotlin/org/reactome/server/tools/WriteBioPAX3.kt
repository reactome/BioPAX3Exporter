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
        xmlBase = base(null)
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
        xmlBase = base(pathway.dbId)
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
        xmlBase = base(pathway.dbId)
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
        xmlBase = base(species.dbId)
    }

    /**
     * The release's base for a file's ids (T018): Reactome's BioPAX namespace, the release
     * where it is known, and the database id of the species or event the file is of, as
     * `http://www.reactome.org/biopax/98/170928#`.
     */
    private fun base(dbId: Long?): String {
        val parts = listOfNotNull("http://www.reactome.org/biopax", dbVersion.takeIf { it > 0 }, dbId)
        return parts.joinToString("/") + "#"
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
        println(String(owl(), Charsets.UTF_8))
    }

    /**
     * Write the BioPAX Model to a file.
     *
     * @param output File to use.
     */
    fun toFile(output: File) {
        output.writeBytes(owl())
    }

    /**
     * Write the BioPAX Model to a String.
     *
     * @return String representing the BioPAX Model.
     */
    override fun toString(): String = String(owl(), Charsets.UTF_8)

    /**
     * The model as OWL, as the release's writer wrote its literals (T018): each trimmed at
     * both ends, and a carriage return kept as a character reference, which an XML reader
     * would otherwise read as a line feed. A failure to write is not caught: a file half
     * written is not one to publish.
     */
    private fun owl(): ByteArray {
        val model = thisModel!!
        BioPAX3Utils.trimLiterals(model)
        val os = ByteArrayOutputStream()
        SimpleIOHandler().convertToOWL(model, os)
        return String(os.toByteArray(), Charsets.UTF_8).replace("\r", "&#13;").toByteArray(Charsets.UTF_8)
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
