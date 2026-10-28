package org.reactome.server.tools

import org.biopax.paxtools.model.level3.BioSource
import org.biopax.paxtools.model.level3.RelationshipTypeVocabulary
import org.biopax.paxtools.model.level3.RelationshipXref
import org.biopax.paxtools.model.level3.UnificationXref
import org.biopax.paxtools.model.level3.XReferrable
import org.reactome.server.graph.domain.model.DatabaseIdentifier
import org.reactome.server.graph.domain.model.DatabaseObject
import org.reactome.server.graph.domain.model.ExternalOntology
import org.reactome.server.graph.domain.model.GO_Term
import org.reactome.server.graph.domain.model.ReferenceEntity
import org.reactome.server.graph.domain.model.ReferenceIsoform
import org.reactome.server.graph.domain.model.Taxon

/**
 * The cross-references and vocabularies the release's converter writes, each once
 * per file (spec 001 T011): Reactome's two ids, a reference's own id, a
 * relationship to another database, its type, and the organism.
 */
class BioPAXXrefBuilder(
    private val context: ConversionContext,
) {
    private val model = context.model

    /**
     * Reactome's two ids on an individual: the database id of the release and, where
     * there is one, the stable id with its version.
     */
    fun addReactomeIds(
        instance: DatabaseObject,
        bpElement: XReferrable,
    ) {
        bpElement.addXref(databaseIdXref(instance))
        stableIdXref(instance)?.let { bpElement.addXref(it) }
    }

    /**
     * A catalyst activity's ids (T013): its database id as a RelationshipXref of type
     * "Same Catalyst Activity", as the release writes it, and its stable id where it has one.
     */
    fun addCatalystActivityIds(
        activity: DatabaseObject,
        bpElement: XReferrable,
    ) {
        val dbId = activity.dbId
        bpElement.addXref(
            context.once("databaseIdRelationship:$dbId") {
                model.addNew(RelationshipXref::class.java, context.nextId("RelationshipXref")).apply {
                    db = "Reactome Database ID Release ${context.release}"
                    id = dbId.toString()
                    addComment(databaseIdComment(dbId))
                    relationshipType = relationshipType(SAME_CATALYST_ACTIVITY)
                }
            },
        )
        stableIdXref(activity)?.let { bpElement.addXref(it) }
    }

    /**
     * A GO term as a RelationshipXref, once per term: its accession with "GO:", under
     * "GENE ONTOLOGY", of the given type (a catalysis's molecular function, T013; an
     * event's biological process, T014).
     */
    fun goTerm(
        term: GO_Term,
        type: String,
    ): RelationshipXref =
        context.once("goTerm:${term.dbId}") {
            model.addNew(RelationshipXref::class.java, context.nextId("RelationshipXref")).apply {
                db = "GENE ONTOLOGY"
                term.accession?.let { id = if (it.startsWith("GO:")) it else "GO:$it" }
                relationshipType = relationshipType(type)
            }
        }

    private fun databaseIdComment(dbId: Long) =
        "Database identifier. Use this URL to connect to the web page of this instance in Reactome: " +
            "http://www.reactome.org/cgi-bin/eventbrowser?DB=gk_current&ID=$dbId"

    private fun databaseIdXref(instance: DatabaseObject): UnificationXref {
        val dbId = instance.dbId
        return context.once("databaseId:$dbId") {
            model.addNew(UnificationXref::class.java, context.nextId("UnificationXref")).apply {
                db = "Reactome Database ID Release ${context.release}"
                id = dbId.toString()
                addComment(databaseIdComment(dbId))
            }
        }
    }

    // The graph holds the version only inside stIdVersion ("R-HSA-1.2").
    private fun stableIdXref(instance: DatabaseObject): UnificationXref? {
        val stId = instance.stId ?: return null
        val version = instance.stIdVersion?.removePrefix("$stId.")?.takeIf { it != instance.stIdVersion } ?: return null
        val key = "$stId.$version"
        return context.once("stableId:$key") {
            model.addNew(UnificationXref::class.java, context.nextId("UnificationXref")).apply {
                db = "Reactome"
                id = stId
                idVersion = version
                addComment(
                    "Reactome stable identifier. Use this URL to connect to the web page of this instance in Reactome: " +
                        "http://www.reactome.org/cgi-bin/eventbrowser_st_id?ST_ID=$key",
                )
            }
        }
    }

    /**
     * A reference entity's (or a PSI-MOD term's) own UnificationXref: its identifier in
     * its database, an isoform's variant identifier under "UniProt Isoform", and
     * "MOD:", "GO:" and "CHEBI:" before the identifiers of those three. None without
     * an identifier or a database.
     */
    fun referenceXref(reference: DatabaseObject): UnificationXref? {
        val isIsoform = reference is ReferenceIsoform
        val identifier =
            (if (reference is ReferenceIsoform) reference.variantIdentifier else null)
                ?: when (reference) {
                    is ReferenceEntity -> reference.identifier
                    is ExternalOntology -> reference.identifier
                    else -> null
                }
                ?: return null
        var dbName = databaseOf(reference) ?: return null
        if (dbName.equals("UniProt", ignoreCase = true) && isIsoform) dbName = "UniProt Isoform"
        val id = if (dbName == "MOD" || dbName == "GO" || dbName == "ChEBI") dbName.uppercase() + ":" + identifier else identifier
        return context.once("referenceXref:${reference.dbId}") {
            model.addNew(UnificationXref::class.java, context.nextId("UnificationXref")).apply {
                db = dbName
                this.id = id
            }
        }
    }

    /**
     * A RelationshipXref of type "additional information" to another database's
     * record: an entity's or an event's cross-reference, or a disease.
     */
    fun additionalInformation(record: DatabaseObject): RelationshipXref =
        context.once("relationship:${record.dbId}") {
            model.addNew(RelationshipXref::class.java, context.nextId("RelationshipXref")).apply {
                relationshipType = relationshipType(ADDITIONAL_INFORMATION)
                databaseOf(record)?.let { db = it }
                when (record) {
                    is DatabaseIdentifier -> record.identifier
                    is ExternalOntology -> record.identifier
                    else -> null
                }?.let { id = it }
            }
        }

    /** A relationship type, once per term, with its PSI-MI xref where it has one. */
    fun relationshipType(term: String): RelationshipTypeVocabulary =
        context.once("relationshipType:$term") {
            model.addNew(RelationshipTypeVocabulary::class.java, context.nextId("RelationshipTypeVocabulary")).apply {
                addTerm(term)
                MI_TERMS[term]?.let { mi ->
                    addXref(
                        model.addNew(UnificationXref::class.java, context.nextId("UnificationXref")).apply {
                            db = "MI"
                            id = mi
                        },
                    )
                }
            }
        }

    /**
     * The organism: one BioSource per taxon, named by it, with its first
     * cross-reference (NCBI Taxonomy) as a UnificationXref.
     */
    fun bioSource(taxon: Taxon): BioSource =
        context.once("bioSource:${taxon.dbId}") {
            model.addNew(BioSource::class.java, context.nextId("BioSource")).apply {
                addName(taxon.displayName)
                taxon.crossReference?.firstOrNull()?.let { record ->
                    addXref(
                        model.addNew(UnificationXref::class.java, context.nextId("UnificationXref")).apply {
                            databaseOf(record)?.let { db = it }
                            record.identifier?.let { id = it }
                        },
                    )
                }
            }
        }

    // The database's name as the graph keeps it on the record (its reference
    // database's display name), which needs no read of the database itself.
    private fun databaseOf(record: DatabaseObject): String? =
        when (record) {
            is ReferenceEntity -> record.databaseName
            is DatabaseIdentifier -> record.databaseName
            is ExternalOntology -> record.databaseName
            else -> null
        }

    companion object {
        const val ADDITIONAL_INFORMATION = "additional information"
        const val GO_BIOLOGICAL_PROCESS = "gene ontology term for cellular process"
        const val GO_MOLECULAR_FUNCTION = "gene ontology term for cellular function"
        const val SAME_CATALYST_ACTIVITY = "Same Catalyst Activity"

        // The release's converter's PSI-MI terms for its relationship types.
        private val MI_TERMS =
            mapOf(
                GO_MOLECULAR_FUNCTION to "MI:0355",
                GO_BIOLOGICAL_PROCESS to "MI:0359",
                ADDITIONAL_INFORMATION to "MI:0361",
            )
    }
}
