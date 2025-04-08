package org.reactome.server.tools;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.reactome.server.graph.domain.model.Species;
import org.reactome.server.graph.domain.model.Event;
import org.reactome.server.graph.domain.model.DatabaseObject;

class BioPAXSpeciesBuilder(
    private val species: Species?,
    private val model: Model?
) {
    fun addReactomeSpecies() {
        if (species == null || model == null) return
        
        val bioSource = model.addNew(BioSource::class.java, BioPAX3Utils.getTypeCount("BioSource"))
        bioSource.name = setOf(species.getDisplayName())
        
        // Create a dummy Event object for the BioPAX3BasicElementsBuilder
        val dummyEvent = object : DatabaseObject() {
            override fun getDisplayName(): String = species.getDisplayName()
            override fun getDbId(): Long = species.getDbId()
            fun getSpeciesName(): String = species.getDisplayName()
        }
        
        val basicElements = BioPAX3BasicElementsBuilder(dummyEvent as Event, model, bioSource as Entity)
        basicElements.addReactomeDataSource()
    }
} 