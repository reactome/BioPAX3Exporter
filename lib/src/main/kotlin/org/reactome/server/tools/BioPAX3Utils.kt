@file:Suppress(
    "ktlint:standard:no-wildcard-imports",
)

package org.reactome.server.tools

import org.biopax.paxtools.controller.PropertyEditor
import org.biopax.paxtools.controller.SimpleEditorMap
import org.biopax.paxtools.model.BioPAXElement
import java.util.*

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

object BioPAX3Utils {
    /**
     * Every string an individual holds, trimmed at both ends, as the release's writer wrote
     * its literals (T018): JDOM trimmed every text it wrote with Java's `String.trim()`,
     * which takes off the characters up to a space and no other (a no-break space stays).
     *
     * Paxtools keeps a display name among the names, and removing a name equal to the
     * display name clears it; this works because Paxtools gives the display name's editor
     * before the names', which sets the trimmed display name before the untrimmed name is
     * replaced (a test holds it).
     */
    fun trimLiterals(model: org.biopax.paxtools.model.Model) {
        model.objects.forEach { element ->
            SimpleEditorMap.L3.getEditorsOf(element).filter { it.range == String::class.java }.forEach { editor ->
                @Suppress("UNCHECKED_CAST")
                val strings = editor as PropertyEditor<BioPAXElement, String>
                strings.getValueFromBean(element).filterNotNull().filter { it != javaTrim(it) }.forEach { value ->
                    if (strings.isMultipleCardinality) strings.removeValueFromBean(value, element)
                    strings.setValueToBean(javaTrim(value), element)
                }
            }
        }
    }

    // Java's String.trim(): the characters up to U+0020 only, as the release's writer had it
    private fun javaTrim(value: String) = value.trim { it <= ' ' }

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
}
