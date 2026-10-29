package org.reactome.server.tools

import org.biopax.paxtools.model.level3.BiochemicalReaction
import org.biopax.paxtools.model.level3.EvidenceCodeVocabulary
import org.biopax.paxtools.model.level3.PublicationXref
import org.biopax.paxtools.model.level3.UnificationXref
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.reactome.server.graph.domain.model.Book
import org.reactome.server.graph.domain.model.Event
import org.reactome.server.graph.domain.model.EvidenceType
import org.reactome.server.graph.domain.model.InstanceEdit
import org.reactome.server.graph.domain.model.LiteratureReference
import org.reactome.server.graph.domain.model.Pathway
import org.reactome.server.graph.domain.model.Person
import org.reactome.server.graph.domain.model.Publication
import org.reactome.server.graph.domain.model.Reaction
import org.reactome.server.graph.domain.model.Summation
import org.reactome.server.graph.domain.model.URL
import org.biopax.paxtools.model.level3.Pathway as BPPathway

/**
 * Events' summations, literature, evidence and curation as the release's converter
 * writes them (spec 001 T016), on a real Paxtools model. Graph-core's objects are mocks,
 * each made before the next is stubbed; the people are placeholders.
 */
class BioPAXLiteratureTest {
    private val context = ConversionContext("http://www.reactome.org/biopax/98/1#", 98)

    private fun person(n: Int): Person = mock(Person::class.java).also { whenever(it.displayName).thenReturn("Person$n, P") }

    private fun article(
        dbId: Long,
        pubMed: Int?,
        volume: Int? = 12,
    ): LiteratureReference {
        val authors = listOf(person(1), person(2))
        return mock(LiteratureReference::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.pubMedIdentifier).thenReturn(pubMed)
            whenever(it.year).thenReturn(2001)
            whenever(it.title).thenReturn("Article $dbId")
            whenever(it.journal).thenReturn("J Biol")
            whenever(it.volume).thenReturn(volume)
            whenever(it.pages).thenReturn("1-9")
            whenever(it.author).thenReturn(authors)
        }
    }

    private fun summation(
        text: String?,
        literature: List<Publication> = emptyList(),
    ): Summation =
        mock(Summation::class.java).also {
            whenever(it.text).thenReturn(text)
            whenever(it.literatureReference).thenReturn(literature)
        }

    private fun edit(name: String?): InstanceEdit = mock(InstanceEdit::class.java).also { whenever(it.displayName).thenReturn(name) }

    private fun reaction(dbId: Long): Reaction =
        mock(Reaction::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn("reaction $dbId")
        }

    private fun electronic(): EvidenceType =
        mock(EvidenceType::class.java).also {
            whenever(it.dbId).thenReturn(700)
            whenever(it.displayName).thenReturn("inferred by electronic annotation")
        }

    private fun write(event: Reaction): BiochemicalReaction =
        BioPAXInteractionBuilder(event, context).addReactomeRLEvent() as BiochemicalReaction

    private fun BiochemicalReaction.publications() = xref.filterIsInstance<PublicationXref>()

    @Test
    fun `each summation is a comment, its HTML kept, and a blank one is none`() {
        val event = reaction(1)
        val summations = listOf(summation("<p>One</p> two"), summation("Three<br>"), summation(" "), summation(null))
        whenever(event.summation).thenReturn(summations)

        assertEquals(setOf("<p>One</p> two", "Three<br>"), write(event).comment)
    }

    @Test
    fun `an article by its PubMed id, with year, title, authors and source`() {
        val event = reaction(1)
        val literature = listOf<Publication>(article(10, 123456))
        whenever(event.literatureReference).thenReturn(literature)

        val xref = write(event).publications().single()

        assertEquals("Pubmed" to "123456", xref.db to xref.id)
        assertEquals(2001, xref.year)
        assertEquals("Article 10", xref.title)
        assertEquals(setOf("Person1, P", "Person2, P"), xref.author)
        assertEquals(setOf("J Biol 12:1-9"), xref.source)
    }

    @Test
    fun `an article with no PubMed id has neither db nor id, and a source without volume`() {
        val event = reaction(1)
        val literature = listOf<Publication>(article(10, null, volume = null))
        whenever(event.literatureReference).thenReturn(literature)

        val xref = write(event).publications().single()

        assertEquals(null to null, xref.db to xref.id)
        assertEquals(setOf("J Biol1-9"), xref.source)
    }

    @Test
    fun `a book by its ISBN, titled by its chapter, and a web page by its address`() {
        val authors = listOf(person(3))
        val book =
            mock(Book::class.java).also {
                whenever(it.dbId).thenReturn(20)
                whenever(it.isbn).thenReturn("0-00-000000-0")
                whenever(it.year).thenReturn(1999)
                whenever(it.chapterTitle).thenReturn("A chapter")
                whenever(it.title).thenReturn("A book")
                whenever(it.pages).thenReturn("5-6")
                whenever(it.author).thenReturn(authors)
            }
        val page =
            mock(URL::class.java).also {
                whenever(it.dbId).thenReturn(21)
                whenever(it.uniformResourceLocator).thenReturn("https://example.org/page")
                whenever(it.title).thenReturn("A page")
                whenever(it.author).thenReturn(authors)
            }
        val event = reaction(1)
        whenever(event.literatureReference).thenReturn(listOf(book, page))

        val (bpBook, bpPage) = write(event).publications().partition { it.db == "ISBN" }

        bpBook.single().let {
            assertEquals("0-00-000000-0", it.id)
            assertEquals(1999, it.year)
            assertEquals("A chapter", it.title)
            assertEquals(setOf("A book (Book): 5-6"), it.source)
            assertEquals(setOf("Person3, P"), it.author)
        }
        bpPage.single().let {
            assertEquals(setOf("https://example.org/page"), it.url)
            assertEquals("A page", it.title)
            assertEquals(emptySet<String>(), it.source)
            assertEquals(null, it.db)
        }
    }

    @Test
    fun `an event's literature and its summations', each publication written once in the file`() {
        val shared = article(10, 111)
        val first = reaction(1)
        whenever(first.literatureReference).thenReturn(listOf(shared))
        val summations = listOf(summation("text", listOf(shared, article(11, 222))))
        whenever(first.summation).thenReturn(summations)
        val second = reaction(2)
        whenever(second.literatureReference).thenReturn(listOf(shared))

        val bpFirst = write(first)
        val bpSecond = write(second)

        assertEquals(setOf("111", "222"), bpFirst.publications().map { it.id }.toSet())
        assertSame(bpFirst.publications().single { it.id == "111" }, bpSecond.publications().single())
        assertEquals(2, context.model.getObjects(PublicationXref::class.java).size)
    }

    @Test
    fun `an inferred event's evidence has its source's first summation, literature and the code`() {
        val source = reaction(100)
        val sourceLiterature = listOf<Publication>(article(10, 111))
        whenever(source.literatureReference).thenReturn(sourceLiterature)
        // Only the first summation counts, as the converter read one value
        val summations = listOf(summation("Source text", listOf(article(11, 222))), summation("Later", listOf(article(12, 333))))
        whenever(source.summation).thenReturn(summations)
        val type = electronic()
        val inferred = reaction(1)
        whenever(inferred.inferredFrom).thenReturn(setOf<Event>(source))
        whenever(inferred.evidenceType).thenReturn(type)

        val evidence = write(inferred).evidence.single()

        assertEquals(setOf("Source text"), evidence.comment)
        assertEquals(setOf("111", "222"), evidence.xref.map { it.id }.toSet())
        val code = evidence.evidenceCode.single()
        assertEquals(setOf("inferred from electronic annotation"), code.term)
        (code.xref.single() as UnificationXref).let { assertEquals("EVIDENCE CODE" to "ECO:0000203", it.db to it.id) }
    }

    @Test
    fun `one evidence for each source, new each time, and one vocabulary for the type`() {
        val type = electronic()
        val sources = listOf(reaction(100), reaction(101))
        val first = reaction(1)
        whenever(first.inferredFrom).thenReturn(sources.toSet())
        whenever(first.evidenceType).thenReturn(type)
        val second = reaction(2)
        whenever(second.inferredFrom).thenReturn(setOf<Event>(sources[0]))
        whenever(second.evidenceType).thenReturn(type)

        val bpFirst = write(first)
        val bpSecond = write(second)

        assertEquals(2, bpFirst.evidence.size)
        assertTrue(bpSecond.evidence.single() !in bpFirst.evidence)
        assertEquals(1, context.model.getObjects(EvidenceCodeVocabulary::class.java).size)
    }

    @Test
    fun `another evidence type is written by its name as a GO id`() {
        val type =
            mock(EvidenceType::class.java).also {
                whenever(it.dbId).thenReturn(701)
                whenever(it.displayName).thenReturn("EXP")
            }
        val event = reaction(1)
        val sources = setOf<Event>(reaction(100))
        whenever(event.inferredFrom).thenReturn(sources)
        whenever(event.evidenceType).thenReturn(type)

        val code =
            write(event)
                .evidence
                .single()
                .evidenceCode
                .single()

        assertEquals(setOf("EXP"), code.term)
        (code.xref.single() as UnificationXref).let { assertEquals("GO" to "EXP", it.db to it.id) }
    }

    @Test
    fun `no evidence without an evidence type, nor for an event not inferred`() {
        val untyped = reaction(1)
        val sources = setOf<Event>(reaction(100))
        whenever(untyped.inferredFrom).thenReturn(sources)

        assertEquals(emptySet<Any>(), write(untyped).evidence)
        assertEquals(emptySet<Any>(), write(reaction(2)).evidence)
    }

    @Test
    fun `who authored, reviewed and edited it, a comment each, an unnamed edit none`() {
        val event = reaction(1)
        val authored = listOf(edit("Placeholder edit 1"))
        val reviewed = listOf(edit("Placeholder edit 2"), edit(""))
        val edited = listOf(edit("Placeholder edit 3"))
        whenever(event.authored).thenReturn(authored)
        whenever(event.reviewed).thenReturn(reviewed)
        whenever(event.edited).thenReturn(edited)

        assertEquals(
            setOf("Authored: Placeholder edit 1", "Reviewed: Placeholder edit 2", "Edited: Placeholder edit 3"),
            write(event).comment,
        )
    }

    @Test
    fun `a pathway has its summations, literature, evidence and curation too`() {
        val type = electronic()
        val source = reaction(100)
        val authored = listOf(edit("Placeholder edit 1"))
        val literature = listOf<Publication>(article(10, 111))
        val summations = listOf(summation("<b>About</b>"))
        val pathway =
            mock(Pathway::class.java).also {
                whenever(it.dbId).thenReturn(1)
                whenever(it.displayName).thenReturn("pathway 1")
                whenever(it.summation).thenReturn(summations)
                whenever(it.literatureReference).thenReturn(literature)
                whenever(it.inferredFrom).thenReturn(setOf<Event>(source))
                whenever(it.evidenceType).thenReturn(type)
                whenever(it.authored).thenReturn(authored)
            }

        BioPAXPathwayBuilder(pathway, context).addReactomePathway()

        val bp = context.model.getObjects(BPPathway::class.java).single()
        assertEquals(setOf("<b>About</b>", "Authored: Placeholder edit 1"), bp.comment)
        assertEquals(
            "111",
            bp.xref
                .filterIsInstance<PublicationXref>()
                .single()
                .id,
        )
        assertEquals(1, bp.evidence.size)
    }
}
