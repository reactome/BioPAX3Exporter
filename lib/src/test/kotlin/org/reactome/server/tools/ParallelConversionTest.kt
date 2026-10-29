package org.reactome.server.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.reactome.server.graph.domain.model.CatalystActivity
import org.reactome.server.graph.domain.model.Complex
import org.reactome.server.graph.domain.model.EntityWithAccessionedSequence
import org.reactome.server.graph.domain.model.Event
import org.reactome.server.graph.domain.model.GenomeEncodedEntity
import org.reactome.server.graph.domain.model.Pathway
import org.reactome.server.graph.domain.model.PhysicalEntity
import org.reactome.server.graph.domain.model.Reaction
import org.reactome.server.graph.domain.model.SimpleEntity
import org.reactome.server.graph.domain.model.Species
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Two conversions at once write, byte for byte, what each writes alone (spec 001
 * T008): each has its own ConversionContext, so nothing one numbers or adds reaches
 * another. Eight pathways, converted in parallel twenty times, each compared with its
 * single run.
 */
class ParallelConversionTest {
    // Graph-core's objects load their relationships through the graph's services, so
    // they are mocks, as in the other tests: all stubbed here, before any thread reads
    // them, which Mockito allows.
    private inline fun <reified T : PhysicalEntity> entity(dbId: Long): PhysicalEntity =
        mock(T::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn("entity $dbId")
        }

    private fun reaction(dbId: Long): Reaction {
        // Each mock made before the next is stubbed: Mockito refuses one made mid-stubbing.
        val inputs = listOf(entity<SimpleEntity>(dbId * 10 + 1), entity<EntityWithAccessionedSequence>(dbId * 10 + 2))
        // A GenomeEncodedEntity is written as a plain PhysicalEntity: the builder's last branch.
        val outputs = listOf(entity<Complex>(dbId * 10 + 3), entity<GenomeEncodedEntity>(dbId * 10 + 4))
        val catalysts = listOf(mock(CatalystActivity::class.java), mock(CatalystActivity::class.java))
        return mock(Reaction::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn("reaction $dbId")
            whenever(it.speciesName).thenReturn("Homo sapiens")
            whenever(it.input).thenReturn(inputs)
            whenever(it.output).thenReturn(outputs)
            whenever(it.catalystActivity).thenReturn(catalysts)
        }
    }

    // A pathway's organism is its first species, one BioSource per taxon (T011).
    private val human =
        mock(Species::class.java).also {
            whenever(it.dbId).thenReturn(48887)
            whenever(it.displayName).thenReturn("Homo sapiens")
        }

    private fun pathway(
        dbId: Long,
        events: List<Event>,
    ): Pathway =
        mock(Pathway::class.java).also {
            whenever(it.dbId).thenReturn(dbId)
            whenever(it.displayName).thenReturn("pathway $dbId")
            whenever(it.speciesName).thenReturn("Homo sapiens")
            whenever(it.species).thenReturn(listOf(human))
            whenever(it.hasEvent).thenReturn(events)
        }

    private fun pathway(n: Long): Pathway {
        val reactions = (1L..3L).map { reaction(n * 100 + it) }
        val child = pathway(n * 10 + 1, reactions.take(2))
        return pathway(n, listOf(child, reactions[2]))
    }

    private fun species(n: Long): Species =
        mock(Species::class.java).also {
            whenever(it.dbId).thenReturn(48887 + n)
            whenever(it.displayName).thenReturn("species $n")
        }

    private fun convertSpecies(
        species: Species,
        pathways: List<Pathway>,
    ): String =
        WriteBioPAX3(species, 97).run {
            createModelForSpecies(pathways)
            toString()
        }

    private fun convert(pathway: Pathway): String =
        WriteBioPAX3(pathway, 97).run {
            createModel()
            toString()
        }

    @Test
    fun `conversions at once write what each writes alone`() {
        val pathways = (1L..8L).map(::pathway)
        // Two species, each with two pathways of their own (T015: no header pathway).
        val species = (1L..2L).map { n -> species(n) to listOf(pathway(10 + 2 * n), pathway(11 + 2 * n)) }
        val conversions: List<() -> String> =
            pathways.map { { convert(it) } } + species.map { (s, ps) -> { convertSpecies(s, ps) } }
        val alone = conversions.map { it() }
        val pool = Executors.newFixedThreadPool(conversions.size)
        try {
            repeat(20) { round ->
                val together = pool.invokeAll(conversions.map { Callable { it() } }).map { it.get() }
                conversions.indices.forEach { i ->
                    assertEquals(alone[i], together[i], "conversion ${i + 1}, round ${round + 1}")
                }
            }
        } finally {
            pool.shutdown()
        }
    }
}
