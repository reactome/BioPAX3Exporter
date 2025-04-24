@file:Suppress(
    "ktlint:standard:no-wildcard-imports",
)

package org.reactome.server.tools

import org.biopax.paxtools.model.BioPAXElement
import java.util.*

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class TypeCounter(
    val name: String,
) {
    var count: Int = 0
        private set

    fun incrementCount() {
        count++
    }
}

object BioPAX3Utils {
    private val count = ArrayList<TypeCounter>()

    /**
     * Function to return the next ID to use for a given class name
     *
     * @param name the name of the BioPax class
     *
     * @return the full qualified ID of the next object of the given class
     */
    fun getTypeCount(name: String): String {
        if (count.isEmpty()) {
            count.add(TypeCounter("Pathway"))
        }
        var id = ""
        for (tc in count) {
            if (tc.name == name) {
                tc.incrementCount()
                id = getID(name + tc.count)
                break
            }
        }
        if (id.isEmpty()) {
            val tc1 = TypeCounter(name)
            tc1.incrementCount()
            count.add(tc1)
            id = getID(name + tc1.count)
        }
        return id
    }

    /**
     * Function to ensure counter is cleared with each new run of WriteBioPAX3
     */
    fun clearCounterArray() {
        if (count.isNotEmpty()) {
            count.clear()
        }
    }

    /**
     * Function to return full ID based on the xml base registered
     *
     * @param id the unique part of the ID to append
     *
     * @return fully qualified ID
     */
    fun getID(id: String): String = WriteBioPAX3.xmlBase + id

    /**
     * Generic function to retrieve an object from a set based on its ID
     *
     * paxtools uses Sets to store the individual types of element added to the
     * model. This is a generic function based on the BioPAXElement class - all
     * of which have an ID
     *
     * @param setObjects the set of objects to query
     * @param id the unqualified id to match
     *
     * @return the object from the set that matches the fully qualified ID created
     * from the id argument passed OR null if no such object exists
     */
    fun <T : BioPAXElement> getObjectFromSet(
        setObjects: Set<T>?,
        id: String?,
    ): T? {
        if (setObjects.isNullOrEmpty() || id.isNullOrEmpty()) {
            return null
        }
        return setObjects.find { it.rdfId == getID(id) }
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
     * @param id the unqualified id to match
     *
     * @return true if the Set contains an element matching the id given; false otherwise
     */
    fun <T : BioPAXElement> contains(
        setObjects: Set<T>?,
        id: String?,
    ): Boolean {
        if (setObjects.isNullOrEmpty() || id.isNullOrEmpty()) {
            return false
        }
        return setObjects.any { it.rdfId == getID(id) }
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
