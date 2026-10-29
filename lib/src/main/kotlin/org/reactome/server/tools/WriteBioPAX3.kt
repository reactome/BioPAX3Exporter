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
    // The one event this writer converts, if it converts one (T019)
    private val thisEvent: Event?
    private var thisModel: Model? = null

    /** The release, as the ids' base names it. */
    var dbVersion: Int = 0
        private set

    /** The xml:base of the conversion's ids. */
    var xmlBase: String = ""
        private set

    /**
     * A writer for events not of one species or event (`-l`): its base names neither, as
     * the release's converter wrote it, `http://www.reactome.org/biopax#`.
     */
    constructor() {
        thisEvent = null
        xmlBase = base(null)
    }

    /**
     * A writer for events not of one species or event, with the release they are of: its
     * xrefs name the release, and its base, as the converter's did, does not.
     *
     * @param version Integer - version number of the database
     */
    constructor(version: Int) {
        thisEvent = null
        xmlBase = base(null)
        dbVersion = version
    }

    /**
     * A writer for one event, of any kind: a pathway with all it holds, or a reaction.
     *
     * @param event Event from ReactomeDB
     */
    constructor(event: Event) {
        thisEvent = event
        xmlBase = base(event.dbId)
    }

    /**
     * A writer for one event, of any kind, with the release it is of.
     *
     * @param event Event from ReactomeDB
     * @param version Integer - version number of the database
     */
    constructor(event: Event, version: Int) {
        thisEvent = event
        dbVersion = version
        xmlBase = base(event.dbId)
    }

    /**
     * A writer for a species' file.
     *
     * @param species Species from ReactomeDB
     * @param version Integer - version number of the database
     */
    constructor(species: Species, version: Int) {
        thisEvent = null
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
     * The BioPAX model of the event given (T019): a pathway with all it holds and its
     * steps, or a reaction (a BiochemicalReaction, Degradation or TemplateReaction, as its
     * kind is) with its entities, catalyses and controls, as the release's converter wrote
     * one event.
     */
    fun createModel() {
        createModelForEvents(listOf(thisEvent ?: error("no event to convert")))
    }

    /**
     * The BioPAX model of a species: its top-level events (SpeciesEvents), each written
     * with all it holds, once in the file, and no header pathway (T015).
     */
    fun createModelForSpecies(events: List<Event>) = createModelForEvents(events)

    /**
     * The BioPAX model of the events given, each written with all it holds, once in the
     * file, and their steps linked.
     */
    fun createModelForEvents(events: List<Event>) {
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
        // Written beside it, then renamed into place in one step (an atomic move replaces a
        // file where REPLACE_EXISTING deletes it first): a failure leaves no file half
        // written, and the file there before stays until the new one is whole
        val partial = File(output.absoluteFile.parentFile, ".${output.name}.partial")
        try {
            partial.writeBytes(owl())
            java.nio.file.Files
                .move(partial.toPath(), output.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        } finally {
            partial.delete()
        }
    }

    /**
     * Write the BioPAX Model to a String.
     *
     * @return String representing the BioPAX Model.
     */
    override fun toString(): String = String(owl(), Charsets.UTF_8)

    /**
     * The model as OWL, as the release's writer wrote its literals (T018): each trimmed at
     * both ends, and a carriage return kept as a character reference, `&#xD;` as the release
     * wrote it, which an XML parser would otherwise read as a line feed. A failure to write
     * is not caught: a file half written is not one to publish.
     */
    private fun owl(): ByteArray {
        val model = thisModel!!
        BioPAX3Utils.trimLiterals(model)
        val os = ByteArrayOutputStream()
        SimpleIOHandler().convertToOWL(model, os)
        return String(os.toByteArray(), Charsets.UTF_8).replace("\r", "&#xD;").toByteArray(Charsets.UTF_8)
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
