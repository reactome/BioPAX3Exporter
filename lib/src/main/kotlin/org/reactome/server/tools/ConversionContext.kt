package org.reactome.server.tools

import org.biopax.paxtools.model.BioPAXElement
import org.biopax.paxtools.model.BioPAXLevel
import org.biopax.paxtools.model.Model

/**
 * One conversion's state (spec 001 T008): the model it writes, its xml:base, the
 * release it writes from, the counters that number its ids, and what it has
 * already written. Each conversion has its own, so two conversions at once number
 * their ids apart; nothing a conversion writes is kept anywhere else.
 */
class ConversionContext(
    val xmlBase: String,
    /** The release, as the "Reactome Database ID Release {n}" xrefs name it (T011). */
    val release: Int,
    /** A new model on the xml:base; a test may give its own. */
    val model: Model =
        BioPAXLevel.L3.defaultFactory
            .createModel()
            .also { it.xmlBase = xmlBase },
) {
    private val counters = HashMap<String, Int>()

    // What has been written, by a key naming the kind and the graph's dbId (or a
    // term): the release's converter writes an entity, its reference, an xref or a
    // vocabulary once per file and points at it from everywhere else (T011).
    private val written = HashMap<String, BioPAXElement>()

    /**
     * Each event's PathwayStep, one per event however many pathways hold it, in the order
     * they were made: once every pathway is written, each step gets its next steps and its
     * event's catalyses and controls (T015).
     */
    val steps = LinkedHashMap<Long, Pair<org.reactome.server.graph.domain.model.Event, org.biopax.paxtools.model.level3.PathwayStep>>()

    /**
     * The id of the next individual of a class: the class's name and its count in
     * this conversion, from 1 (`Pathway1`, `Pathway2`, ...), on the xml:base.
     */
    fun nextId(name: String): String {
        val count = (counters[name] ?: 0) + 1
        counters[name] = count
        return id(name + count)
    }

    /** The full id, on the xml:base, of an individual's own part of it. */
    fun id(local: String): String = xmlBase + local

    /**
     * The individual written for a key, or the one [make] writes, kept for the key.
     * It is kept only once [make] returns, as the release's converter keeps it: a
     * complex's components are written while the complex is.
     */
    fun <T : BioPAXElement> once(
        key: String,
        make: () -> T,
    ): T {
        @Suppress("UNCHECKED_CAST")
        written[key]?.let { return it as T }
        val made = make()
        written[key] = made
        return made
    }

    /**
     * The element written for [key], or a new one from [make], kept before [fill] gives it
     * its contents: the release's converter keeps a pathway before handling its events, so
     * an event that reaches the pathway again finds it there. If [fill] throws, the element
     * stays kept; nothing catches that, so the run ends.
     */
    fun <T : BioPAXElement> onceKeptFirst(
        key: String,
        make: () -> T,
        fill: (T) -> Unit,
    ): T {
        @Suppress("UNCHECKED_CAST")
        written[key]?.let { return it as T }
        val made = make()
        written[key] = made
        fill(made)
        return made
    }
}
