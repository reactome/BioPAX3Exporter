// package org.reactome.server.tools

// import org.biopax.paxtools.model.Model
// import org.biopax.paxtools.model.level3.BioSource
// import org.biopax.paxtools.model.level3.Entity
// import org.reactome.server.graph.domain.model.Species
// import org.reactome.server.graph.domain.model.Pathway

// /**
//  * Builds species-specific BioPAX elements for a given Reactome species.
//  */
// class BioPAXSpeciesBuilder(
//     private val species: Species?,
//     private val model: Model?
// ) {
//     fun addReactomeSpecies() {
//         // Ensure we have both species and model
//         if (species == null || model == null) return

//         // Create and initialize a BioSource for this species
//         val bioSource = model.addNew(
//             BioSource::class.java,
//             BioPAX3Utils.getTypeCount("BioSource")
//         )
//         bioSource.name = setOf(species.displayName)

//         // Create a dummy Event by subclassing Pathway (implements Event)
//         val dummyEvent = object : Pathway() {
//             override fun getDisplayName(): String = species.displayName
//             override fun getDbId(): Long = species.dbId
//         }

//         // Use dummyEvent to add Reactome data source elements
//         val basicElements = BioPAX3BasicElementsBuilder(
//             dummyEvent,
//             model,
//             bioSource as Entity
//         )
//         basicElements.addReactomeDataSource()
//     }
// }
