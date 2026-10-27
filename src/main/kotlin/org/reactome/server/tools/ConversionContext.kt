package org.reactome.server.tools

import org.biopax.paxtools.model.BioPAXLevel
import org.biopax.paxtools.model.Model

/**
 * One conversion's state (spec 001 T008): the model it writes, its xml:base, the
 * release it is of, and the counters that number its ids. Each conversion has its
 * own, so two conversions at once number their ids apart; nothing a conversion
 * writes is kept anywhere else.
 */
class ConversionContext(
    val dbVersion: Int,
    val xmlBase: String,
    /** A new model on the xml:base; a test may give its own. */
    val model: Model =
        BioPAXLevel.L3.defaultFactory
            .createModel()
            .also { it.xmlBase = xmlBase },
) {
    private val counters = HashMap<String, Int>()

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
}
