package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.service.SpeciesService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.biopax.validator.BiopaxIdentifier;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.ValidatorUtils;
import org.biopax.validator.api.beans.Validation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import java.io.PrintWriter;
import java.io.IOException;

// import org.reactome.server.tools.config.GraphQANeo4jConfig;

import java.io.File;
import java.util.ArrayList;
// import java.util.List;
import java.util.stream.Collectors;


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
        private var model: org.biopax.paxtools.model.Model? = null

        enum class Status {
            SINGLE_PATH, ALL_PATWAYS, ALL_PATHWAYS_SPECIES, MULTIPLE_PATHS, MULTIPLE_EVENTS
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val jsap = SimpleJSAP(
                    BioPAX3ExporterLauncher::class.java.name,
                    "A tool for generating SBML files",
                    arrayOf(
                        FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.REQUIRED, 'h', "host", "The neo4j host"),
                        FlaggedOption("port", JSAP.STRING_PARSER, "7474", JSAP.NOT_REQUIRED, 'b', "port", "The neo4j port"),
                        FlaggedOption("user", JSAP.STRING_PARSER, "neo4j", JSAP.REQUIRED, 'u', "user", "The neo4j user"),
                        FlaggedOption("password", JSAP.STRING_PARSER, "reactome", JSAP.REQUIRED, 'p', "password", "The neo4j password"),
                        FlaggedOption("outdir", JSAP.STRING_PARSER, ".", JSAP.REQUIRED, 'o', "outdir", "The output directory"),
                        FlaggedOption("toplevelpath", JSAP.LONG_PARSER, "0", JSAP.NOT_REQUIRED, 't', "toplevelpath", "A single id of a pathway"),
                        FlaggedOption("species", JSAP.LONG_PARSER, "0", JSAP.NOT_REQUIRED, 's', "species", "The id of a species")
                    )
                )

                val m = FlaggedOption("multiple", JSAP.LONG_PARSER, null, JSAP.NOT_REQUIRED, 'm', "multiple", "A list of ids of Pathways")
                m.isList = true
                m.listSeparator = ','
                jsap.registerParameter(m)

                val loe = FlaggedOption("listevents", JSAP.LONG_PARSER, null, JSAP.NOT_REQUIRED, 'l', "listevents", "A list of ids of Events to be output as a single model")
                loe.isList = true
                loe.listSeparator = ','
                jsap.registerParameter(loe)

                val config = jsap.parse(args)
                if (!config.success()) System.exit(1)

                // Build a valid Bolt URI from host/port
                val boltUrl = "bolt://${config.getString("host")}:${config.getString("port")}"

                ReactomeGraphCore.initialise(
                    boltUrl,
                    config.getString("user"),
                    config.getString("password")
                )

                val genericService = ReactomeGraphCore.getService(GeneralService::class.java)
                val databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService::class.java)
                val speciesService = ReactomeGraphCore.getService(SpeciesService::class.java)
                val schemaService = ReactomeGraphCore.getService(SchemaService::class.java)

                outputStatus = Status.SINGLE_PATH
                parseAdditionalArguments(config)

                if (!singleArgumentSupplied()) {
                    System.err.println("Too many arguments detected. Expected either no pathway arguments or one of -t, -s, -m, -l.")
                } else {
                    dbVersion = genericService.dbInfo.version

                    when (outputStatus) {
                        Status.SINGLE_PATH -> {
                            var pathway: Pathway? = null
                            try {
                                pathway = databaseObjectService.findByIdNoRelations(singleId) as? Pathway
                            } catch (e: Exception) {
                                System.err.println("$singleId is not the identifier of a valid Pathway object")
                            }
                            pathway?.let { outputPath(it) }
                        }
                        Status.ALL_PATWAYS -> {
                            speciesService.species.forEach { s ->
                                outputPathsForSpecies(s)
                            }
                        }
                        Status.ALL_PATHWAYS_SPECIES -> {
                            var species: Species? = null
                            try {
                                species = databaseObjectService.findByIdNoRelations(speciesId) as? Species
                            } catch (e: Exception) {
                                System.err.println("$speciesId is not the identifier of a valid Species object")
                            }
                            species?.let { outputPathsForSpecies(it) }
                        }
                        Status.MULTIPLE_PATHS -> {
                            multipleIds.forEach { id ->
                                var pathway: Pathway? = null
                                try {
                                    pathway = databaseObjectService.findByIdNoRelations(id) as? Pathway
                                } catch (e: Exception) {
                                    System.err.println("$id is not the identifier of a valid Pathway object")
                                }
                                pathway?.let { outputPath(it) }
                            }
                        }
                        Status.MULTIPLE_EVENTS -> {
                            // Declare eventList explicitly as a java.util.ArrayList
                            val eventList: java.util.ArrayList<Event> = java.util.ArrayList()
                            var valid = true
                            multipleEvents.forEach { id ->
                                try {
                                    val event = databaseObjectService.findByIdNoRelations(id) as? Event
                                    event?.let { eventList.add(it) }
                                } catch (e: Exception) {
                                    valid = false
                                    System.err.println("$id is not the identifier of a valid Event object")
                                }
                            }
                            if (valid && eventList.isNotEmpty()) {
                                // Pass eventList directly; it is already a java.util.ArrayList<Event>
                                outputEvents(eventList as java.util.List<Event>)
                            }
                        }
                    }
                }
            } catch (e: JSAPException) {
                e.printStackTrace()
            }
        }

        private fun parseAdditionalArguments(config: JSAPResult) {
            outputdir = config.getString("outdir")
            singleId = config.getLong("toplevelpath")
            speciesId = config.getLong("species")
            multipleIds = config.getLongArray("multiple")
            multipleEvents = config.getLongArray("listevents")

            outputStatus = when {
                singleId != 0L -> Status.SINGLE_PATH
                speciesId != 0L -> Status.ALL_PATHWAYS_SPECIES
                multipleIds.isNotEmpty() -> Status.MULTIPLE_PATHS
                multipleEvents.isNotEmpty() -> Status.MULTIPLE_EVENTS
                else -> Status.ALL_PATWAYS
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

        private fun outputPath(pathway: Pathway) {
            val filename = "${pathway.getDbId()}.owl"
            val outputFile = File(outputdir, filename)
            val bp = WriteBioPAX3(pathway, dbVersion)
            bp.createModel()
            bp.toFile(outputFile)
        }

        private fun outputPathsForSpecies(species: Species) {
            val pathways: List<Pathway> = getPathwaysForSpecies(species)
            if (pathways.isEmpty()) {
                println("No pathways found for species ${species.displayName}")
                return
            }
            
            // Create a single file for all pathways of this species
            val writer = WriteBioPAX3(species, dbVersion)
            writer.createModelForSpecies(species, pathways)

            /* --------- NEW : add events that are NOT in a pathway --------- */

            // 1. all events for this species
            val allEvents = ReactomeGraphCore
                .getService(SchemaService::class.java)
                .getByClass(Event::class.java, species)
                .toList()

            // 2. collect every event that is part of any pathway
            val eventsInPathways = mutableSetOf<Event>()
            fun collect(evts: Collection<Event>) {
                evts.forEach { ev ->
                    eventsInPathways += ev
                    if (ev is Pathway) collect(ev.hasEvent ?: emptyList())
                }
            }
            pathways.forEach { collect(it.hasEvent ?: emptyList()) }

            // 3. the “orphans” are those that never appeared in a pathway
            val orphanEvents = allEvents.filterNot { eventsInPathways.contains(it) }
    /* -------------------------------------------------------------- */
            // Create output directory if it doesn't exist
            val outputDir = File(outputdir)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val outputFile = File(outputDir, "${species.displayName.replace(" ", "_")}.owl")
            writer.toFile(outputFile)
            println("Generated BioPAX file for species ${species.displayName}: ${outputFile.absolutePath}")
        }

        private fun outputEvents(events: java.util.List<Event>) {
            val filename = "events.owl"
            val outputFile = File(outputdir, filename)
            // Create a dummy pathway for the events
            val dummyPathway = object : Pathway() {
                override fun getDisplayName(): String = "Events"
                override fun getDbId(): Long = 0L
            }
            val bp = WriteBioPAX3(dummyPathway, dbVersion)
            bp.createModel()
            bp.toFile(outputFile)
        }

        private fun getPathwaysForSpecies(species: Species): List<Pathway> =
            ReactomeGraphCore
            .getService(SchemaService::class.java)
            .getByClass(Pathway::class.java, species)
            .toList()
    }
}

