package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.BioSource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.config.GraphQANeo4jConfig;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class WriteBioPAX3SingleReactionTest

{
    private static WriteBioPAX3 testWrite;


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-SCE-1474244"; // pathway with a single child reaction
        Pathway pathway = (Pathway) databaseObjectService.findById(dbid);
        testWrite = new WriteBioPAX3(pathway, 99);
    }

    @Test
    public void testConstructor()
    {
        assertTrue( "WriteSBML constructor failed", testWrite != null );
    }


    @Test
    public void testCreateModel() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        assertEquals("biopax level", model.getLevel(), BioPAXLevel.L3);

        String thisXmlBase = "http://www.reactome.org/biopax/99/9010984#";

        assertEquals("xmlbase", model.getXmlBase(), thisXmlBase);
    }

    @Test
    public void testPathway1() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<org.biopax.paxtools.model.level3.Pathway> pathways = model.getObjects(org.biopax.paxtools.model.level3.Pathway.class);
        assertEquals("num pathways", pathways.size(), 3);

        org.biopax.paxtools.model.level3.Pathway p = BioPAX3Utils.getObjectFromSet(pathways, "Pathway1");

        assertTrue("path1", p != null);

        assertEquals("display name", p.getDisplayName(), "Extracellular matrix organization");

        Set<org.biopax.paxtools.model.level3.Process> components = p.getPathwayComponent();
        assertEquals("num components", components.size(), 1);
        assertTrue("step process value", BioPAX3Utils.contains(components, "Pathway2"));

        Set<org.biopax.paxtools.model.level3.PathwayStep> order = p.getPathwayOrder();
        assertEquals("num orders", order.size(), 1);
        assertTrue("order value", BioPAX3Utils.contains(order, "PathwayStep1"));
    }

    @Test
    public void testPathway2() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<org.biopax.paxtools.model.level3.Pathway> pathways = model.getObjects(org.biopax.paxtools.model.level3.Pathway.class);
        assertEquals("num pathways", pathways.size(), 3);

        org.biopax.paxtools.model.level3.Pathway p = BioPAX3Utils.getObjectFromSet(pathways, "Pathway2");

        assertTrue("path2", p != null);

        assertEquals("display name", p.getDisplayName(), "Degradation of the extracellular matrix");

        Set<org.biopax.paxtools.model.level3.Process> components = p.getPathwayComponent();
        assertEquals("num components", components.size(), 1);
        assertTrue("step process value", BioPAX3Utils.contains(components, "Pathway3"));

        Set<org.biopax.paxtools.model.level3.PathwayStep> order = p.getPathwayOrder();
        assertEquals("num orders", order.size(), 1);
        assertTrue("order value", BioPAX3Utils.contains(order, "PathwayStep2"));
    }

    @Test
    public void testPathway3() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<org.biopax.paxtools.model.level3.Pathway> pathways = model.getObjects(org.biopax.paxtools.model.level3.Pathway.class);
        assertEquals("num pathways", pathways.size(), 3);

        org.biopax.paxtools.model.level3.Pathway p = BioPAX3Utils.getObjectFromSet(pathways, "Pathway3");

        assertTrue("path2", p != null);

        assertEquals("display name", p.getDisplayName(), "Collagen degradation");

        Set<org.biopax.paxtools.model.level3.Process> components = p.getPathwayComponent();
        assertEquals("num components", components.size(), 1);
        assertTrue("step process value", BioPAX3Utils.contains(components, "BiochemicalReaction1"));

        Set<org.biopax.paxtools.model.level3.PathwayStep> order = p.getPathwayOrder();
        assertEquals("num orders", order.size(), 1);
        assertTrue("order value", BioPAX3Utils.contains(order, "PathwayStep3"));

        Set<org.biopax.paxtools.model.level3.Evidence> evidence = p.getEvidence();
        assertEquals("num evidence", evidence.size(), 1);
        assertTrue("order value", BioPAX3Utils.contains(evidence, "Evidence3"));
    }

    @Test
    public void testBiochemicalReaction() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<org.biopax.paxtools.model.level3.BiochemicalReaction> reactions = model.getObjects(org.biopax.paxtools.model.level3.BiochemicalReaction.class);
        assertEquals("num reactions", reactions.size(), 1);

        org.biopax.paxtools.model.level3.BiochemicalReaction p = BioPAX3Utils.getObjectFromSet(reactions, "BiochemicalReaction1");

        assertTrue("reaction", p != null);

        assertEquals("display name", p.getDisplayName(), "PHYKPL tetramer hydrolyses 5PHL");

        org.biopax.paxtools.model.level3.BiochemicalReaction p1 = BioPAX3Utils.getObjectFromSet(reactions, "BiochemicalReaction2");

        assertTrue("non existant reaction", p1 == null);

        Set<org.biopax.paxtools.model.level3.Evidence> evidence = p.getEvidence();
        assertEquals("num evidence", evidence.size(), 1);
        assertTrue("order value", BioPAX3Utils.contains(evidence, "Evidence4"));
    }

    @Test
    public void testCatalyst() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<org.biopax.paxtools.model.level3.Catalysis> cats = model.getObjects(org.biopax.paxtools.model.level3.Catalysis.class);
        assertEquals("num catalysts", cats.size(), 1);

        org.biopax.paxtools.model.level3.Catalysis p = BioPAX3Utils.getObjectFromSet(cats, "Catalysis1");

        assertTrue("catalyst", p != null);

        Set<org.biopax.paxtools.model.level3.Process> order = p.getControlled();
        assertEquals("num orders", order.size(), 1);

        assertTrue("order value", BioPAX3Utils.contains(order, "BiochemicalReaction1"));
    }

@Test
    public void testPathwayStep1() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<org.biopax.paxtools.model.level3.PathwayStep> pathsteps = model.getObjects(org.biopax.paxtools.model.level3.PathwayStep.class);
        assertEquals("num pathway steps", pathsteps.size(), 3);

        org.biopax.paxtools.model.level3.PathwayStep p = BioPAX3Utils.getObjectFromSet(pathsteps, "PathwayStep1");

        assertTrue("path step1", p != null);

        Set<org.biopax.paxtools.model.level3.Process> components = p.getStepProcess();
        assertEquals("num step processes", components.size(), 1);

        assertTrue("step process value", BioPAX3Utils.contains(components, "Pathway2"));
    }

    @Test
    public void testPathwayStep2() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<org.biopax.paxtools.model.level3.PathwayStep> pathsteps = model.getObjects(org.biopax.paxtools.model.level3.PathwayStep.class);
        assertEquals("num pathway steps", pathsteps.size(), 3);

        org.biopax.paxtools.model.level3.PathwayStep p = BioPAX3Utils.getObjectFromSet(pathsteps, "PathwayStep2");

        assertTrue("path step1", p != null);

        Set<org.biopax.paxtools.model.level3.Process> components = p.getStepProcess();
        assertEquals("num step processes", components.size(), 1);

        assertTrue("step process value", BioPAX3Utils.contains(components, "Pathway3"));
    }

    @Test
    public void testPathwayStep3() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<org.biopax.paxtools.model.level3.PathwayStep> pathsteps = model.getObjects(org.biopax.paxtools.model.level3.PathwayStep.class);
        assertEquals("num pathway steps", pathsteps.size(), 3);

        org.biopax.paxtools.model.level3.PathwayStep p = BioPAX3Utils.getObjectFromSet(pathsteps, "PathwayStep3");

        assertTrue("path step1", p != null);

        Set<org.biopax.paxtools.model.level3.Process> components = p.getStepProcess();
        assertEquals("num step processes", components.size(), 2);

        assertTrue("step process value", BioPAX3Utils.contains(components, "BiochemicalReaction1"));
        assertTrue("step process value", BioPAX3Utils.contains(components, "Catalysis1"));
    }

    @Test
    public void testBiosource1() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<String> name = new TreeSet<String>();
        name.add("Saccharomyces cerevisiae");

        Set<org.biopax.paxtools.model.level3.BioSource> sources = model.getObjects(org.biopax.paxtools.model.level3.BioSource.class);
        assertTrue("num biosources", sources.size() == 1);

        org.biopax.paxtools.model.level3.BioSource p = BioPAX3Utils.getObjectFromSet(sources, "BioSource1");
        assertTrue("biosource1", p != null);
        assertEquals("src name", p.getName(), name);

    }

    @Test
    public void testBiosourceFromPathway1() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<String> name = new TreeSet<String>();
        name.add("Saccharomyces cerevisiae");

        Set<org.biopax.paxtools.model.level3.Pathway> pathways = model.getObjects(org.biopax.paxtools.model.level3.Pathway.class);
        org.biopax.paxtools.model.level3.Pathway p = BioPAX3Utils.getObjectFromSet(pathways, "Pathway1");
        BioSource src = p.getOrganism();
        assertTrue("source", src != null);
        assertEquals("src name", src.getName(), name);
    }

    @Test
    public void testBiosourceFromPathway2() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<String> name = new TreeSet<String>();
        name.add("Saccharomyces cerevisiae");

        Set<org.biopax.paxtools.model.level3.Pathway> pathways = model.getObjects(org.biopax.paxtools.model.level3.Pathway.class);
        org.biopax.paxtools.model.level3.Pathway p = BioPAX3Utils.getObjectFromSet(pathways, "Pathway2");
        BioSource src = p.getOrganism();
        assertTrue("source", src != null);
        assertEquals("src name", src.getName(), name);
    }

    @Test
    public void testBiosourceFromPathway3() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<String> name = new TreeSet<String>();
        name.add("Saccharomyces cerevisiae");

        Set<org.biopax.paxtools.model.level3.Pathway> pathways = model.getObjects(org.biopax.paxtools.model.level3.Pathway.class);
        org.biopax.paxtools.model.level3.Pathway p = BioPAX3Utils.getObjectFromSet(pathways, "Pathway3");
        BioSource src = p.getOrganism();
        assertTrue("source", src != null);
        assertEquals("src name", src.getName(), name);
    }

    @Test
    public void testProvenance1() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<String> name = new TreeSet<String>();
        name.add("Reactome");

        Set<String> comment = new TreeSet<String>();
        comment.add("http://www.reactome.org");

        Set<org.biopax.paxtools.model.level3.Provenance> sources = model.getObjects(org.biopax.paxtools.model.level3.Provenance.class);
        assertTrue("num Provenances", sources.size() == 1);

        org.biopax.paxtools.model.level3.Provenance p = BioPAX3Utils.getObjectFromSet(sources, "Provenance1");
        assertTrue("Provenance1", p != null);
        assertEquals("src name", p.getName(), name);
        assertEquals("prov comment", p.getComment(), comment);
    }

    @Test
    public void testProvenanceFromPathway1() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<String> name = new TreeSet<String>();
        name.add("Reactome");

        Set<String> comment = new TreeSet<String>();
        comment.add("http://www.reactome.org");

        Set<org.biopax.paxtools.model.level3.Pathway> pathways = model.getObjects(org.biopax.paxtools.model.level3.Pathway.class);
        org.biopax.paxtools.model.level3.Pathway p = BioPAX3Utils.getObjectFromSet(pathways, "Pathway1");
        org.biopax.paxtools.model.level3.Provenance src = BioPAX3Utils.getObjectFromSet(p.getDataSource(), "Provenance1");
        assertTrue("source", src != null);
        assertEquals("src name", src.getName(), name);
        assertEquals("prov comment", src.getComment(), comment);
    }

    @Test
    public void testEvidence() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<org.biopax.paxtools.model.level3.Evidence> evid = model.getObjects(org.biopax.paxtools.model.level3.Evidence.class);
        assertEquals("num evidences", evid.size(), 4);

    }
}
