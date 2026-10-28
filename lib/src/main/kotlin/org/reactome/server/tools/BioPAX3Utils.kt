@file:Suppress(
    "ktlint:standard:no-wildcard-imports",
)

package org.reactome.server.tools

import org.biopax.paxtools.model.BioPAXElement
import java.util.*

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

object BioPAX3Utils {
    /**
     * Names as the release's converter writes an entity's or an event's (T011, T012): the
     * shortest name (the first of the shortest) as the display name and the rest as names;
     * the display name stands in when there is no name.
     */
    fun addNames(
        names: List<String>?,
        displayName: String?,
        bpElement: org.biopax.paxtools.model.level3.Named,
    ) {
        val all = names?.toMutableList() ?: mutableListOf()
        if (all.isEmpty() && displayName != null) all.add(displayName)
        val shortest = all.minByOrNull { it.length } ?: return
        bpElement.displayName = shortest
        all.remove(shortest)
        all.forEach { bpElement.addName(it) }
    }

    /**
     * Generic function to retrieve an object from a set based on its ID
     *
     * paxtools uses Sets to store the individual types of element added to the
     * model. This is a generic function based on the BioPAXElement class - all
     * of which have an ID
     *
     * @param setObjects the set of objects to query
     * @param id the full id to match (ConversionContext.id gives it)
     *
     * @return the object from the set with that id, or null if there is none
     */
    fun <T : BioPAXElement> getObjectFromSet(
        setObjects: Set<T>?,
        id: String?,
    ): T? {
        if (setObjects.isNullOrEmpty() || id.isNullOrEmpty()) {
            return null
        }
        return setObjects.find { it.rdfId == id }
    }

    /**
     * Generic function to retrieve an object from a set based on its Set of names
     *
     * paxtools uses Sets to store the individual types of element added to the
     * model. This is a generic function based on the Named class - all
     * of which have a Set<String> names
     *
     * @param setObjects the set of objects to query
     * @param name String representing a name to match
     *
     * @return the object from the set that matches the name argument passed
     * OR null if no such object exists
     */
    fun <T : org.biopax.paxtools.model.level3.Named> getObjectFromSetByName(
        setObjects: Set<T>?,
        name: String?,
    ): T? {
        if (setObjects.isNullOrEmpty() || name.isNullOrEmpty()) {
            return null
        }
        val names = TreeSet<String>().apply { add(name) }
        return setObjects.find { it.name == names }
    }

    /**
     * @param setObjects the set of objects to query
     * @param id the full id to match (ConversionContext.id gives it)
     *
     * @return true if the Set contains an element with that id; false otherwise
     */
    fun <T : BioPAXElement> contains(
        setObjects: Set<T>?,
        id: String?,
    ): Boolean {
        if (setObjects.isNullOrEmpty() || id.isNullOrEmpty()) {
            return false
        }
        return setObjects.any { it.rdfId == id }
    }

    /**
     * Function to determine whether a set contains an element with the given name
     *
     * @param setObjects the set of objects to query
     * @param name String representing a name to match
     *
     * @return true if the Set contains an element matching the name given; false otherwise
     */
    fun <T : org.biopax.paxtools.model.level3.Named> containsName(
        setObjects: Set<T>?,
        name: String?,
    ): Boolean {
        if (setObjects.isNullOrEmpty() || name.isNullOrEmpty()) {
            return false
        }
        val names = TreeSet<String>().apply { add(name) }
        return setObjects.any { it.name == names }
    }
}
