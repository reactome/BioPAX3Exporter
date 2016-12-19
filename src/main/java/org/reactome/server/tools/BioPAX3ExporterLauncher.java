
package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import com.sun.org.apache.xpath.internal.operations.Neg;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.service.GeneralService;
import org.reactome.server.graph.service.SchemaService;
import org.reactome.server.graph.service.SpeciesService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.config.GraphQANeo4jConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class BioPAX3ExporterLauncher {

    private static String outputdir = ".";

    // arguments to determine what to output
    private static long singleId = 0;
    private static long speciesId = 0;
    private static long[] multipleIds;
    private static long[] multipleEvents;

    private enum Status {
        SINGLE_PATH, ALL_PATWAYS, ALL_PATHWAYS_SPECIES, MULTIPLE_PATHS, MULTIPLE_EVENTS
    }
    private static Status outputStatus = Status.SINGLE_PATH;

    private static int dbVersion = 0;


    public static void main(String[] args) throws JSAPException {

        SimpleJSAP jsap = new SimpleJSAP(SBMLExporterLauncher.class.getName(), "A tool for generating SBML files",
                new Parameter[]{
                        new FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.REQUIRED, 'h', "host", "The neo4j host"),
                        new FlaggedOption("port", JSAP.STRING_PARSER, "7474", JSAP.NOT_REQUIRED, 'b', "port", "The neo4j port"),
                        new FlaggedOption("user", JSAP.STRING_PARSER, "neo4j", JSAP.REQUIRED, 'u', "user", "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER, "reactome", JSAP.REQUIRED, 'p', "password", "The neo4j password"),
                        new FlaggedOption("outdir", JSAP.STRING_PARSER, ".", JSAP.REQUIRED, 'o', "outdir", "The output directory"),
                        new FlaggedOption("toplevelpath", JSAP.LONG_PARSER, "0", JSAP.NOT_REQUIRED, 't', "toplevelpath", "A single id of a pathway"),
                        new FlaggedOption("species", JSAP.LONG_PARSER, "0", JSAP.NOT_REQUIRED, 's', "species", "The id of a species"),
                }
        );
        FlaggedOption m =  new FlaggedOption("multiple", JSAP.LONG_PARSER, null, JSAP.NOT_REQUIRED, 'm', "multiple", "A list of ids of Pathways");
        m.setList(true);
        m.setListSeparator(',');
        jsap.registerParameter(m);

        FlaggedOption loe =  new FlaggedOption("listevents", JSAP.LONG_PARSER, null, JSAP.NOT_REQUIRED, 'l', "listevents", "A list of ids of Events to be output as a single model");
        loe.setList(true);
        loe.setListSeparator(',');
        jsap.registerParameter(loe);

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), GraphQANeo4jConfig.class);

        GeneralService genericService = ReactomeGraphCore.getService(GeneralService.class);
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        SpeciesService speciesService = ReactomeGraphCore.getService(SpeciesService.class);
        SchemaService schemaService = ReactomeGraphCore.getService(SchemaService.class);

        outputStatus = Status.SINGLE_PATH;
        parseAdditionalArguments(config);

        if (!singleArgumentSupplied()) {
            System.err.println("Too many arguments detected. Expected either no pathway arguments or one of -t, -s, -m, -l.");
        }
        else {
            dbVersion = genericService.getDBVersion();

            switch (outputStatus) {
                case SINGLE_PATH:
                    Pathway pathway = null;
                    try {
                        pathway = (Pathway) databaseObjectService.findByIdNoRelations(singleId);
                    } catch (Exception e) {
                        System.err.println(singleId + " is not the identifier of a valid Pathway object");
                    }
                    if (pathway != null) {
                        outputPath(pathway);
                    }
                    break;
                case ALL_PATWAYS:
                    for (Species s : speciesService.getSpecies()) {
                        outputPathsForSpecies(s, schemaService);
                    }
                    break;
                case ALL_PATHWAYS_SPECIES:
                    Species species = null;
                    try {
                        species = (Species) databaseObjectService.findByIdNoRelations(speciesId);
                    } catch (Exception e) {
                        System.err.println(speciesId + " is not the identifier of a valid Species object");
                    }
                    if (species != null) {
                        outputPathsForSpecies(species, schemaService);
                    }
                    break;
                case MULTIPLE_PATHS:
                    for (long id : multipleIds) {
                        pathway = null;
                        try {
                            pathway = (Pathway) databaseObjectService.findByIdNoRelations(id);
                        } catch (Exception e) {
                            System.err.println(id + " is not the identifier of a valid Pathway object");
                        }
                        if (pathway != null) {
                            outputPath(pathway);
                        }
                    }
                case MULTIPLE_EVENTS:
                    List<Event> eventList = new ArrayList<Event>();
                    boolean valid = true;
                    for (long id: multipleEvents) {
                        Event event;
                        try {
                            event = (Event) databaseObjectService.findByIdNoRelations(id);
                            eventList.add(event);
                        } catch (Exception e) {
                            valid = false;
                            System.err.println(id + " is not the identifier of a valid Event object");
                        }
                    }
                    if (valid && eventList.size() > 0){
                        outputEvents(eventList);
                    }


                default:
                    break;
            }
        }

    }

    /**
     *  function to get the command line arguments and determine the requested output
     *
     * @param config JSAPResult result of first parse
     */
    private static void parseAdditionalArguments(JSAPResult config) {
        outputdir = config.getString("outdir");

        singleId = config.getLong("toplevelpath");
        speciesId = config.getLong("species");
        multipleIds = config.getLongArray("multiple");
        multipleEvents = config.getLongArray("listevents");

        if (singleId == 0) {
            if (speciesId == 0) {
                if (multipleIds.length > 0){
                    outputStatus = Status.MULTIPLE_PATHS;
                }
                else if (multipleEvents.length > 0){
                    outputStatus = Status.MULTIPLE_EVENTS;
                }
                else {
                    outputStatus = Status.ALL_PATWAYS;
                }
            }
            else {
                outputStatus = Status.ALL_PATHWAYS_SPECIES;
            }
        }
    }

    /**
     *  function to check that only one argument relating to teh pathway has been given
     *
     * @return true if only one argument, false if more than one
     */
    private static boolean singleArgumentSupplied(){
        if (singleId != 0) {
            // have -t shouldnt have anything else
            if (speciesId != 0){
                return false;
            }
            else if (multipleIds.length > 0) {
                return false;
            }
            else if (multipleEvents.length > 0) {
                return false;
            }
        }
        else if (speciesId != 0) {
            // have -s shouldnt have anything else
            if (multipleIds.length > 0) {
                return false;
            }
            else if (multipleEvents.length > 0) {
                return false;
            }
        }
        else if (multipleIds.length > 0){
            // have -m shouldnt hve anythoing else
            if (multipleEvents.length > 0) {
                return false;
            }

        }
        return true;
    }

    /**
     * Output all Pathways for the given Species
     *
     * @param species ReactomeDB Species
     * @param schemaService database service to use
     */
    private static void outputPathsForSpecies(Species species, SchemaService schemaService) {
        for (Pathway path : schemaService.getByClass(Pathway.class, species)){
            outputPath(path);
        }
    }

    /**
     * Create the output file and write the SBML file for this path
     *
     * @param path ReactomeDB Pathway to output
     */
    private static void outputPath(Pathway path) {
        String filename = path.getDbId() + ".xml";
        File out = new File(outputdir, filename);
        WriteSBML sbml = new WriteSBML(path, dbVersion);
        sbml.setAnnotationFlag(true);
        sbml.createModel();
//        sbml.toStdOut();
        sbml.toFile(out.getPath());
    }

    private static void outputEvents(List<Event> loe){
        WriteSBML sbml = new WriteSBML(loe, dbVersion);
        sbml.setAnnotationFlag(true);
        sbml.createModel();
//        sbml.toStdOut();
        String filename = sbml.getModelId() + ".xml";
        File out = new File(outputdir, filename);
        sbml.toFile(out.getPath());

    }
}

