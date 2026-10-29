package org.reactome.server.tools

import org.reactome.server.graph.domain.model.Event
import org.reactome.server.graph.service.AdvancedDatabaseObjectService
import org.reactome.server.graph.service.DatabaseObjectService

/**
 * The events a species' file is written from, as the release chose them (spec 001 T015;
 * Pathway-Exchange's SpeciesAllPathwaysConverter): the species' events whose species are
 * this one alone, less those another such event holds (InstanceUtilities.grepTopLevelEvents).
 * What they hold is written as part of them. A reaction of this species alone that no
 * such event holds is one of them, written alone, as the release would write it; release
 * 98 has none. An event of two species that no such event holds is not written, as in
 * the release.
 */
object SpeciesEvents {
    // In dbId order, so a file numbers its ids the same on every run.
    private const val TOP_LEVEL =
        "MATCH (s:Species {dbId: \$species})<-[:species]-(e:Event) " +
            "WHERE NOT EXISTS { MATCH (e)-[:species]->(o:Species) WHERE o.dbId <> \$species } " +
            "  AND NOT EXISTS { " +
            "    MATCH (p:Event)-[:hasEvent]->(e) " +
            "    WHERE (p)-[:species]->(s) " +
            "      AND NOT EXISTS { MATCH (p)-[:species]->(o:Species) WHERE o.dbId <> \$species } " +
            "  } " +
            "RETURN e.dbId ORDER BY e.dbId"

    /** The top-level events' database ids, in order. */
    fun topLevelIds(
        speciesDbId: Long,
        ads: AdvancedDatabaseObjectService,
    ): List<Long> = ads.getCustomQueryResults(Long::class.javaObjectType, TOP_LEVEL, mapOf<String, Any>("species" to speciesDbId)).toList()

    /**
     * The top-level events, each loaded as it is reached and held by nothing here after:
     * a species' events, with all a conversion loads beneath them, do not fit in memory at
     * once (T020).
     */
    fun topLevel(
        speciesDbId: Long,
        ads: AdvancedDatabaseObjectService,
        dbs: DatabaseObjectService,
    ): Iterable<Event> = loaded(topLevelIds(speciesDbId, ads), dbs)

    /** The events of the ids given, each loaded as it is reached and held by nothing here after. */
    fun loaded(
        ids: List<Long>,
        dbs: DatabaseObjectService,
    ): Iterable<Event> = Iterable { ids.asSequence().map { dbs.findByIdNoRelations<Event>(it) }.iterator() }
}
