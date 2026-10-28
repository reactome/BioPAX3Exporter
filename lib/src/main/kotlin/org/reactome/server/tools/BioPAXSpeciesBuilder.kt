package org.reactome.server.tools

import org.reactome.server.graph.domain.model.Pathway
import org.reactome.server.graph.domain.model.Species

/**
 * Builds the BioPAX header (BioSource + Reactome dataSource) for a species-level dump.
 */
class BioPAXSpeciesBuilder(
    private val species: Species,
    private val context: ConversionContext,
) {
    private val model = context.model

    fun addReactomeSpecies() {
        // 1. BioSource *****************************************************
        // The one BioSource per taxon, which pathways and entity references share (T011)
        BioPAXXrefBuilder(context).bioSource(species)
        val sp = species

        // 2. Reactome dataSource *******************************************
        // fake Event needed by BasicElementsBuilder
        val dummyEvent =
            object : Pathway() {
                override fun getDisplayName(): String? = sp.displayName

                override fun getDbId(): Long? = sp.dbId
            }

        // 3. Tiny BioPAX Pathway header (real Entity) **********************
        val header: org.biopax.paxtools.model.level3.Pathway =
            model.addNew(
                org.biopax.paxtools.model.level3.Pathway::class.java,
                context.nextId("Pathway"),
            )
        header.displayName = sp.displayName

        // 4. Attach Reactome provenance to the header ***********************
        BioPAX3BasicElementsBuilder(
            dummyEvent, // first parameter: Reactome Event (can be null, but this gives names)
            context,
            header, // third parameter: BioPAX Entity
        ).addReactomeDataSource()
    }
}
