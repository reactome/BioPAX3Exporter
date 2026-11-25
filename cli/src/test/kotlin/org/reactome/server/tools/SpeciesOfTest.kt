package org.reactome.server.tools

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.reactome.server.graph.domain.model.DatabaseObject
import org.reactome.server.graph.domain.model.Pathway
import org.reactome.server.graph.domain.model.Species
import org.reactome.server.graph.service.DatabaseObjectService
import org.springframework.dao.DataAccessResourceFailureException

// A species named with -s: one that cannot be read is reported as itself and the run
// fails, as an event's id is, not ended by a stack trace (#103).
class SpeciesOfTest {
    // The failure is the launcher's own, kept between calls: reset, so that no test passes
    // on a failure an earlier one left (review of 25 Nov).
    @BeforeEach
    fun noFailureYet() = BioPAX3ExporterLauncher.resetFailed()

    private fun serviceAnswering(found: DatabaseObject?): DatabaseObjectService =
        mock(DatabaseObjectService::class.java).also {
            whenever(it.findByIdNoRelations<DatabaseObject>(any<Long>())).thenReturn(found)
        }

    @Test
    fun `an id that cannot be read is reported, not thrown`() {
        val service = mock(DatabaseObjectService::class.java)
        whenever(service.findByIdNoRelations<DatabaseObject>(any<Long>()))
            .thenThrow(DataAccessResourceFailureException("the graph is unreachable"))

        assertNull(BioPAX3ExporterLauncher.speciesOf(service, 48887L))
        assertTrue(BioPAX3ExporterLauncher.hasFailed())
    }

    @Test
    fun `an id that names no species fails`() {
        assertNull(BioPAX3ExporterLauncher.speciesOf(serviceAnswering(mock(Pathway::class.java)), 109581L))
        assertTrue(BioPAX3ExporterLauncher.hasFailed())
    }

    @Test
    fun `an id that names a species is it, and nothing fails`() {
        val human = mock(Species::class.java)
        assertSame(human, BioPAX3ExporterLauncher.speciesOf(serviceAnswering(human), 48887L))
        assertFalse(BioPAX3ExporterLauncher.hasFailed())
    }
}
