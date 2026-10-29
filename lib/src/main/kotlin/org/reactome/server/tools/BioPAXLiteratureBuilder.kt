package org.reactome.server.tools

import org.biopax.paxtools.model.level3.Entity
import org.biopax.paxtools.model.level3.Evidence
import org.biopax.paxtools.model.level3.EvidenceCodeVocabulary
import org.biopax.paxtools.model.level3.PublicationXref
import org.biopax.paxtools.model.level3.UnificationXref
import org.reactome.server.graph.domain.model.Book
import org.reactome.server.graph.domain.model.Event
import org.reactome.server.graph.domain.model.EvidenceType
import org.reactome.server.graph.domain.model.InstanceEdit
import org.reactome.server.graph.domain.model.LiteratureReference
import org.reactome.server.graph.domain.model.Publication
import org.reactome.server.graph.domain.model.Summation
import org.reactome.server.graph.domain.model.URL

/**
 * An event's literature and provenance as the release's converter writes them (spec 001
 * T016): its summations as comments, their HTML kept; its literature and its summations'
 * as PublicationXrefs, each written once per file; for an inferred event, an Evidence for
 * each event it was inferred from; and its curation as "Authored: ", "Reviewed: " and
 * "Edited: " comments.
 */
class BioPAXLiteratureBuilder(
    private val context: ConversionContext,
) {
    private val model = context.model

    /** Each summation's text as a comment, as it is, where it has any. */
    fun addSummations(
        event: Event,
        bpEvent: Entity,
    ) {
        event.summation?.forEach { summation ->
            summation.text?.takeIf { it.isNotBlank() }?.let { bpEvent.addComment(it) }
        }
    }

    /** The event's own literature and its summations', each once. */
    fun addLiterature(
        event: Event,
        bpEvent: Entity,
    ) {
        literatureOf(event.literatureReference, event.summation).forEach { bpEvent.addXref(it) }
    }

    /**
     * For an event with an evidence type, an Evidence for each event it was inferred from,
     * new each time: that event's first summation as a comment, its literature and that
     * summation's as xrefs, and the evidence type as its code.
     */
    fun addEvidence(
        event: Event,
        bpEvent: Entity,
    ) {
        val type = event.evidenceType ?: return
        event.inferredFrom?.sortedBy { it.dbId }?.forEach { source ->
            val summation = source.summation?.firstOrNull()
            bpEvent.addEvidence(
                model.addNew(Evidence::class.java, context.nextId("Evidence")).apply {
                    literatureOf(source.literatureReference, listOfNotNull(summation)).forEach { addXref(it) }
                    summation?.text?.let { addComment(it) }
                    addEvidenceCode(evidenceCode(type))
                },
            )
        }
    }

    /** Who authored, reviewed and edited the event, a comment for each. */
    fun addCuration(
        event: Event,
        bpEvent: Entity,
    ) {
        addCuration("Authored", event.authored, bpEvent)
        addCuration("Reviewed", event.reviewed, bpEvent)
        addCuration("Edited", event.edited, bpEvent)
    }

    private fun addCuration(
        title: String,
        edits: List<InstanceEdit>?,
        bpEvent: Entity,
    ) {
        edits?.forEach { edit ->
            edit.displayName?.takeIf { it.isNotEmpty() }?.let { bpEvent.addComment("$title: $it") }
        }
    }

    // The literature named, then each summation's, each publication once.
    private fun literatureOf(
        literature: List<Publication>?,
        summations: List<Summation>?,
    ): List<PublicationXref> =
        ((literature ?: emptyList()) + (summations ?: emptyList()).flatMap { it.literatureReference ?: emptyList() })
            .distinctBy { it.dbId }
            .mapNotNull { publicationXref(it) }

    /**
     * A publication as the release's ReactomeToBioPAXPublicationConverter writes it: a
     * journal article by its PubMed id, a book by its ISBN, a web page by its address; each
     * with its year, title and authors, and an article or a book its source.
     */
    private fun publicationXref(publication: Publication): PublicationXref? {
        if (publication !is LiteratureReference && publication !is Book && publication !is URL) return null
        return context.once("publication:${publication.dbId}") {
            model.addNew(PublicationXref::class.java, context.nextId("PublicationXref")).apply {
                when (publication) {
                    is LiteratureReference -> {
                        publication.pubMedIdentifier?.let {
                            id = it.toString()
                            db = "Pubmed"
                        }
                        publication.year?.let { year = it }
                        publication.title?.let { title = it }
                        addAuthors(publication, this)
                        val source = StringBuilder()
                        publication.journal?.let { source.append(it) }
                        publication.volume?.let { source.append(" ").append(it).append(":") }
                        publication.pages?.let { source.append(it) }
                        if (source.isNotEmpty()) addSource(source.toString())
                    }

                    is Book -> {
                        publication.isbn?.let {
                            id = it
                            db = "ISBN"
                        }
                        publication.year?.let { year = it }
                        (publication.chapterTitle ?: publication.title)?.let { title = it }
                        addAuthors(publication, this)
                        val source = StringBuilder()
                        publication.title?.let { source.append(it).append(" (Book)") }
                        publication.pages?.let { source.append(": ").append(it) }
                        if (source.isNotEmpty()) addSource(source.toString())
                    }

                    is URL -> {
                        publication.uniformResourceLocator?.let { addUrl(it) }
                        publication.title?.let { title = it }
                        addAuthors(publication, this)
                    }
                }
            }
        }
    }

    private fun addAuthors(
        publication: Publication,
        xref: PublicationXref,
    ) {
        publication.author?.forEach { person -> person.displayName?.let { xref.addAuthor(it) } }
    }

    /**
     * An evidence type's vocabulary, once per file: "inferred from electronic annotation"
     * (the release's name for Reactome's "inferred by electronic annotation") with its
     * Evidence Code Ontology id, any other by its name as a GO id, as the release wrote it.
     */
    private fun evidenceCode(type: EvidenceType): EvidenceCodeVocabulary =
        context.once("evidenceCode:${type.dbId}") {
            var term = type.displayName
            if (term.equals("inferred by electronic annotation", ignoreCase = true)) term = ELECTRONIC
            model.addNew(EvidenceCodeVocabulary::class.java, context.nextId("EvidenceCodeVocabulary")).apply {
                addTerm(term)
                addXref(
                    model.addNew(UnificationXref::class.java, context.nextId("UnificationXref")).apply {
                        if (term == ELECTRONIC) {
                            db = "EVIDENCE CODE"
                            id = "ECO:0000203"
                        } else {
                            db = "GO"
                            id = term
                        }
                    },
                )
            }
        }

    private companion object {
        const val ELECTRONIC = "inferred from electronic annotation"
    }
}
