
@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.reactome.server.tools

import org.biopax.paxtools.model.level3.*
import org.reactome.server.graph.domain.model.Event
import java.util.List

class BioPAXEventBuilder(
    private val events: List<Event>?,
    private val context: ConversionContext?,
) {
    private val model = context?.model

    fun addReactomeEvents() {
        if (events == null || model == null) return

        events.forEach { event ->
            val process = model.addNew(Process::class.java, context!!.nextId("Process"))
            process.name = setOf(event.getDisplayName())

            val basicElements = BioPAX3BasicElementsBuilder(event, context, process)
            basicElements.addReactomeDataSource()
            basicElements.addEvidence()
        }
    }
}
