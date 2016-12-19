package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.config.GraphQANeo4jConfig;


/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
@RunWith(value=Suite.class)

//@Suite.SuiteClasses(value={WriteSBMLCatalystTest.class})

@Suite.SuiteClasses(value={
                            WriteSBMLNoEventsTest.class, WriteSBMLNoEventsAnnotTest.class,
                            WriteSBMLNoPathwayTest.class, WriteSBMLNoPathwayAnnotTest.class,
                            WriteSBMLSingleReactionTest.class, WriteSBMLSingleAnnotatedReactionTest.class,
                            WriteSBMLCatalystTest.class, WriteSBMLFailedReaction.class,
                            WriteSBMLReactionTest.class, WriteSBMLPathwayTest.class,
                            WriteSBMLTopLevelPathTest.class, WriteSBMLBlackBoxTest.class,
                            WriteSBMLEntityTest.class, WriteSBMLGenomeEncodedEntityTest.class,
                            WriteSBMLOtherEntityTest.class, WriteSBMLCandidateSetEntityTest.class,
                            WriteSBMLPolymerEntityTest.class, WriteSBMLPolymerisationTest.class,
                            WriteSBMLDepolymerisationTest.class, WriteSBMLNegativeRegulatorTest.class,
                            WriteSBMLPositiveRegulatorTest.class, WriteSBMLGeneRegulationTest.class,
                            WriteSBMLConstructorTest.class, WriteSBMLListEventsTest.class,
                            WriteSBMLMissingPubTest.class, WriteSBMLNotTranslationalModTest.class,
                            WriteSBMLCatalystNoPETest.class, WriteSBMLListEventsNoParentTest.class,
                            WriteSBMLBadNotesTest.class})

public class Test {

    @BeforeClass
    public static void setup()  throws JSAPException {
        SimpleJSAP jsap = new SimpleJSAP(SBMLExporterLauncher.class.getName(), "A tool for generating SBML files",
                new Parameter[]{
                        new FlaggedOption("host", JSAP.STRING_PARSER, "localhost", JSAP.REQUIRED, 'h', "host", "The neo4j host"),
                        new FlaggedOption("port", JSAP.STRING_PARSER, "7474", JSAP.REQUIRED, 'b', "port", "The neo4j port"),
                        new FlaggedOption("user", JSAP.STRING_PARSER, null, JSAP.REQUIRED, 'u', "user", "The neo4j user"),
                        new FlaggedOption("password", JSAP.STRING_PARSER, null, JSAP.REQUIRED, 'p', "password", "The neo4j password")
                }
        );
        String[] args = {"-h", "localhost", "-b", "7474", "-u", "neo4j", "-p", "j16a3s27"};
        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("port"), config.getString("user"), config.getString("password"), GraphQANeo4jConfig.class);
    }

}
