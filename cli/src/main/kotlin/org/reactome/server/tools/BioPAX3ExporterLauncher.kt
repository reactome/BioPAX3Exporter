@file:Suppress(
    "ktlint:standard:no-wildcard-imports",
    "ktlint:standard:import-ordering",
)

package org.reactome.server.tools
import com.martiansoftware.jsap.*
import org.reactome.server.graph.domain.model.*
import org.reactome.server.graph.service.AdvancedDatabaseObjectService
import org.reactome.server.graph.service.DatabaseObjectService
import org.reactome.server.graph.service.GeneralService
import org.reactome.server.graph.service.SpeciesService
import org.reactome.server.graph.utils.ReactomeGraphCore

import java.io.File

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class BioPAX3ExporterLauncher {
    companion object {
        private var outputdir: String = "."
        private var singleId: Long = 0
        private var speciesId: Long = 0
        private var multipleIds: LongArray = LongArray(0)
        private var multipleEvents: LongArray = LongArray(0)
        private var outputStatus: Status = Status.SINGLE_PATH
        private var dbVersion: Int = 0

        enum class Status {
            SINGLE_PATH,
            ALL_PATHWAYS,
            ALL_PATHWAYS_SPECIES,
            MULTIPLE_PATHS,
            MULTIPLE_EVENTS,
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val jsap =
                    SimpleJSAP(
                        BioPAX3ExporterLauncher::class.java.name,
                        "A tool for generating BioPAX Level 3 files",
                        arrayOf(
                            FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.REQUIRED, 'h', "host", "The neo4j host"),
                            FlaggedOption("port", JSAP.STRING_PARSER, "7687", JSAP.NOT_REQUIRED, 'b', "port", "The neo4j port"),
                            FlaggedOption("user", JSAP.STRING_PARSER, "neo4j", JSAP.REQUIRED, 'u', "user", "The neo4j user"),
                            FlaggedOption("password", JSAP.STRING_PARSER, "reactome", JSAP.REQUIRED, 'p', "password", "The neo4j password"),
                            FlaggedOption("outdir", JSAP.STRING_PARSER, ".", JSAP.REQUIRED, 'o', "outdir", "The output directory"),
                            FlaggedOption(
                                "toplevelpath",
                                JSAP.LONG_PARSER,
                                "0",
                                JSAP.NOT_REQUIRED,
                                't',
                                "toplevelpath",
                                "The id of an event, a pathway or a reaction, written to {id}.owl",
                            ),
                            FlaggedOption("species", JSAP.LONG_PARSER, "0", JSAP.NOT_REQUIRED, 's', "species", "The id of a species"),
                        ),
                    )

                val m =
                    FlaggedOption(
                        "multiple",
                        JSAP.LONG_PARSER,
                        null,
                        JSAP.NOT_REQUIRED,
                        'm',
                        "multiple",
                        "A list of ids of events, each written to {id}.owl",
                    )
                m.isList = true
                m.listSeparator = ','
                jsap.registerParameter(m)

                val loe =
                    FlaggedOption(
                        "listevents",
                        JSAP.LONG_PARSER,
                        null,
                        JSAP.NOT_REQUIRED,
                        'l',
                        "listevents",
                        "A list of ids of events, written together as one model to events.owl",
                    )
                loe.isList = true
                loe.listSeparator = ','
                jsap.registerParameter(loe)

                val config = jsap.parse(args)
                if (!config.success()) System.exit(1)
                // --help: the usage is printed; nothing to convert
                if (jsap.messagePrinted()) System.exit(0)

                // Build a valid Bolt URI from host/port
                val boltUrl = "bolt://${config.getString("host")}:${config.getString("port")}"

                ReactomeGraphCore.initialise(
                    boltUrl,
                    config.getString("user"),
                    config.getString("password"),
                )

                val genericService = ReactomeGraphCore.getService(GeneralService::class.java)
                val databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService::class.java)
                val speciesService = ReactomeGraphCore.getService(SpeciesService::class.java)

                outputStatus = Status.SINGLE_PATH
                parseAdditionalArguments(config)
                // Every mode writes into it: made here, once
                File(outputdir).mkdirs()

                if (!singleArgumentSupplied()) {
                    fail("Too many arguments detected. Expected either no pathway arguments or one of -t, -s, -m, -l.")
                } else {
                    dbVersion = genericService.dbInfo.version

                    when (outputStatus) {
                        Status.SINGLE_PATH -> {
                            eventOf(databaseObjectService, singleId)?.let { outputEvent(it) }
                        }

                        Status.ALL_PATHWAYS -> {
                            speciesService.species.forEach { s ->
                                outputPathsForSpecies(s)
                            }
                        }

                        Status.ALL_PATHWAYS_SPECIES -> {
                            val species = databaseObjectService.findByIdNoRelations<DatabaseObject>(speciesId) as? Species
                            if (species == null) fail("$speciesId is not the identifier of a Species")
                            species?.let { outputPathsForSpecies(it) }
                        }

                        Status.MULTIPLE_PATHS -> {
                            multipleIds.forEach { id -> eventOf(databaseObjectService, id)?.let { outputEvent(it) } }
                        }

                        Status.MULTIPLE_EVENTS -> {
                            // One model of all the events listed, or none if any is not an event (T019)
                            val events = multipleEvents.map { eventOf(databaseObjectService, it) }
                            if (events.all { it != null }) outputEvents(events.filterNotNull())
                        }
                    }
                }
                if (failed) System.exit(1)
            } catch (e: JSAPException) {
                e.printStackTrace()
            }
        }

        // Set when an id names nothing to convert: the run says which, and ends in failure.
        private var failed = false

        private fun fail(message: String) {
            System.err.println(message)
            failed = true
        }

        /**
         * The event an id names, of a kind the release converts (a pathway or a reaction,
         * T019), or none, said so; an error reading it is reported as itself.
         */
        private fun eventOf(
            service: DatabaseObjectService,
            id: Long,
        ): Event? {
            val found =
                try {
                    service.findByIdNoRelations<DatabaseObject>(id)
                } catch (e: org.springframework.dao.DataAccessException) {
                    fail("$id could not be read: ${e.message}")
                    return null
                }
            return when (found) {
                is Pathway, is ReactionLikeEvent -> {
                    found as Event
                }

                is Event -> {
                    fail("$id is an event of a kind not converted (${found.schemaClass})")
                    null
                }

                else -> {
                    fail("$id is not the identifier of an Event")
                    null
                }
            }
        }

        private fun parseAdditionalArguments(config: JSAPResult) {
            outputdir = config.getString("outdir")
            singleId = config.getLong("toplevelpath")
            speciesId = config.getLong("species")
            multipleIds = config.getLongArray("multiple")
            multipleEvents = config.getLongArray("listevents")

            outputStatus =
                when {
                    singleId != 0L -> Status.SINGLE_PATH
                    speciesId != 0L -> Status.ALL_PATHWAYS_SPECIES
                    multipleIds.isNotEmpty() -> Status.MULTIPLE_PATHS
                    multipleEvents.isNotEmpty() -> Status.MULTIPLE_EVENTS
                    else -> Status.ALL_PATHWAYS
                }
        }

        private fun singleArgumentSupplied(): Boolean {
            var count = 0
            if (singleId != 0L) count++
            if (speciesId != 0L) count++
            if (multipleIds.isNotEmpty()) count++
            if (multipleEvents.isNotEmpty()) count++
            return count <= 1
        }

        // One event, of any kind, to {dbId}.owl
        private fun outputEvent(event: Event) {
            val bp = WriteBioPAX3(event, dbVersion)
            bp.createModel()
            bp.toFile(File(outputdir, "${event.dbId}.owl"))
        }

        private fun outputPathsForSpecies(species: Species) {
            // The species' top-level events, as the release chose them (T015)
            val events =
                SpeciesEvents.topLevel(
                    species.dbId,
                    ReactomeGraphCore.getService(AdvancedDatabaseObjectService::class.java),
                    ReactomeGraphCore.getService(DatabaseObjectService::class.java),
                )
            if (events.isEmpty()) {
                println("No events found for species ${species.displayName}")
                return
            }

            // One file for all of this species' events
            val writer = WriteBioPAX3(species, dbVersion)
            writer.createModelForSpecies(events)

            val outputDir = File(outputdir)

            val outputFile = File(outputDir, "${species.displayName.replace(" ", "_")}.owl")
            writer.toFile(outputFile)
            println("Generated BioPAX file for species ${species.displayName}: ${outputFile.absolutePath}")
        }

        // The events listed (-l), each with all it holds, as one model in events.owl
        private fun outputEvents(events: List<Event>) {
            val bp = WriteBioPAX3(dbVersion)
            bp.createModelForEvents(events)
            bp.toFile(File(outputdir, "events.owl"))
        }
    }
}
