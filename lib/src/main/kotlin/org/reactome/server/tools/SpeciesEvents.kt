package org.reactome.server.tools

import org.reactome.server.graph.domain.model.Event
import org.reactome.server.graph.service.AdvancedDatabaseObjectService
import org.reactome.server.graph.service.DatabaseObjectService

/**
 * The events a species' file is written from, as the release chose them (spec 001 T015;
 * Pathway-Exchange's SpeciesAllPathwaysConverter): the species' events whose species are
 * this one alone, less those another such event holds (InstanceUtilities.grepTopLevelEvents).
 * Everything else is written as part of one of them. A reaction that no event of this
 * species alone holds is one of them, written alone with no step, as the release would
 * write it; release 98 has none.
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

    fun topLevel(
        speciesDbId: Long,
        ads: AdvancedDatabaseObjectService,
        dbs: DatabaseObjectService,
    ): List<Event> =
        ads
            .getCustomQueryResults(Long::class.javaObjectType, TOP_LEVEL, mapOf<String, Any>("species" to speciesDbId))
            .map { dbs.findByIdNoRelations<Event>(it) }
}
