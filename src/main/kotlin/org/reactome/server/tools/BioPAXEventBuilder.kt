
@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.reactome.server.tools

import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.*
import org.reactome.server.graph.domain.model.Event
import java.util.List

class BioPAXEventBuilder(
    private val events: List<Event>?,
    private val model: Model?,
) {
    fun addReactomeEvents() {
        if (events == null || model == null) return

        events.forEach { event ->
            val process = model.addNew(Process::class.java, BioPAX3Utils.getTypeCount("Process"))
            process.name = setOf(event.getDisplayName())

            val basicElements = BioPAX3BasicElementsBuilder(event, model, process)
            basicElements.addReactomeDataSource()
            basicElements.addEvidence()
        }
    }
}
