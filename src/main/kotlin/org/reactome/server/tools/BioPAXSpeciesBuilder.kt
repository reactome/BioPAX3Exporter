package org.reactome.server.tools

import org.biopax.paxtools.model.Model
import org.biopax.paxtools.model.level3.BioSource
import org.biopax.paxtools.model.level3.Entity
import org.reactome.server.graph.domain.model.Species
import org.reactome.server.graph.domain.model.Pathway

/**
 * Builds the BioPAX header (BioSource + Reactome dataSource) for a species-level dump.
 */
class BioPAXSpeciesBuilder(
    private val species: Species,
    private val model: Model
) {
    fun addReactomeSpecies() {
        /* 1. BioSource ******************************************************/
        val bioSource = model.addNew(
            BioSource::class.java,
            BioPAX3Utils.getTypeCount("BioSource")
        )
        bioSource.name = setOf(species.displayName)
        val sp = species   

        /* 2. Reactome dataSource ********************************************/
        // fake Event needed by BasicElementsBuilder
        val dummyEvent = object : Pathway() {
            override fun getDisplayName(): String? = sp.displayName
            override fun getDbId(): Long?        = sp.dbId
        }

        /* 3. Tiny BioPAX Pathway header (real Entity) ***********************/
        val header: org.biopax.paxtools.model.level3.Pathway = 
        model.addNew(
            org.biopax.paxtools.model.level3.Pathway::class.java,
            BioPAX3Utils.getTypeCount("Pathway")
        )
        header.displayName = sp.displayName

        /* 4. Attach Reactome provenance to the header ************************/
        BioPAX3BasicElementsBuilder(
            dummyEvent,       // first parameter: Reactome Event (can be null, but this gives names)
            model,
            header            // third parameter: BioPAX Entity
        ).addReactomeDataSource()
    }
}
