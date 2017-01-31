package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;

import java.util.Set;
import java.util.TreeSet;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class TestBioPAX3Utils

{
    private static WriteBioPAX3 testWrite = null;


    @BeforeClass
    public static void setup()  throws JSAPException {
        DatabaseObjectService databaseObjectService = ReactomeGraphCore.getService(DatabaseObjectService.class);
        String dbid = "R-SCE-1474244"; // pathway with a single child reaction
        Pathway pathway = (Pathway) databaseObjectService.findById(dbid);
        testWrite = new WriteBioPAX3(pathway, 77);
    }

    @Test
    public void testGetID() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        assertEquals("getID", BioPAX3Utils.getID("xyz"), "http://www.reactome.org/biopax/77/9010984#xyz");
    }

    @Test
    public void testGetTypeCount() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }
        assertEquals(BioPAX3Utils.getTypeCount("Sarah"), "http://www.reactome.org/biopax/77/9010984#Sarah1");
        assertEquals(BioPAX3Utils.getTypeCount("Pathway"), "http://www.reactome.org/biopax/77/9010984#Pathway4");
    }

    @Test
    public void testGetNullObjects() {
        Set<org.biopax.paxtools.model.level3.Pathway> emptySet = new TreeSet<org.biopax.paxtools.model.level3.Pathway>();
        assertTrue("get from empty set", BioPAX3Utils.getObjectFromSet(emptySet, "") == null);
        assertTrue("get from null set", BioPAX3Utils.getObjectFromSet(null, "") == null);
        assertTrue("get name from empty set", BioPAX3Utils.getObjectFromSetByName(emptySet, "") == null);
        assertTrue("get name from null set", BioPAX3Utils.getObjectFromSetByName(null, "") == null);
        assertFalse("contains id from empty set", BioPAX3Utils.contains(emptySet, ""));
        assertFalse("contains id from null set", BioPAX3Utils.contains(null, ""));
        assertFalse("contains name from empty set", BioPAX3Utils.containsName(emptySet, ""));
        assertFalse("contains name from null set", BioPAX3Utils.containsName(null, ""));
    }

    @Test
    public void testGetObjects() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<org.biopax.paxtools.model.level3.Pathway> pathways = model.getObjects(org.biopax.paxtools.model.level3.Pathway.class);
        assertTrue("get existing", BioPAX3Utils.getObjectFromSet(pathways, "Pathway3") != null);
        assertTrue("contains existing", BioPAX3Utils.contains(pathways, "Pathway3"));
        assertTrue("get not existing", BioPAX3Utils.getObjectFromSet(pathways, "Pathway6") == null);
        assertFalse("contains not existing", BioPAX3Utils.contains(pathways, "Pathway6"));
        assertTrue("get empty", BioPAX3Utils.getObjectFromSet(pathways, "") == null);
        assertFalse("contains empty", BioPAX3Utils.contains(pathways, ""));
        assertTrue("get null", BioPAX3Utils.getObjectFromSet(pathways, null) == null);
        assertFalse("contains null", BioPAX3Utils.contains(pathways, null));
    }

    @Test
    public void testGetNamedObjects() {
        org.biopax.paxtools.model.Model model = testWrite.getModel();
        if (model == null) {
            testWrite.createModel();
            model = testWrite.getModel();
        }

        Set<org.biopax.paxtools.model.level3.BioSource> sources = model.getObjects(org.biopax.paxtools.model.level3.BioSource.class);
        assertTrue("get existing", BioPAX3Utils.getObjectFromSetByName(sources, "Saccharomyces cerevisiae") != null);
        assertTrue("contains existing", BioPAX3Utils.containsName(sources, "Saccharomyces cerevisiae"));
        assertTrue("get not existing", BioPAX3Utils.getObjectFromSetByName(sources, "Pathway6") == null);
        assertFalse("contains not existing", BioPAX3Utils.containsName(sources, "Pathway6"));
        assertTrue("get empty", BioPAX3Utils.getObjectFromSetByName(sources, "") == null);
        assertFalse("contains empty", BioPAX3Utils.containsName(sources, ""));
        assertTrue("get null", BioPAX3Utils.getObjectFromSetByName(sources, null) == null);
        assertFalse("contains null", BioPAX3Utils.containsName(sources, null));
    }
//
//    @Test
//    public void testBiochemicalReaction() {
//        org.biopax.paxtools.model.Model model = testWrite.getModel();
//        if (model == null) {
//            testWrite.createModel();
//            model = testWrite.getModel();
//        }
//
//        Set<org.biopax.paxtools.model.level3.BiochemicalReaction> reactions = model.getObjects(org.biopax.paxtools.model.level3.BiochemicalReaction.class);
//        assertEquals("num reactions", reactions.size(), 1);
//
//        org.biopax.paxtools.model.level3.BiochemicalReaction p = BioPAX3Utils.getObjectFromSet(reactions, "BiochemicalReaction1");
//
//        assertTrue("reaction", p != null);
//
//        assertEquals("display name", p.getDisplayName(), "PHYKPL tetramer hydrolyses 5PHL");
//
//        org.biopax.paxtools.model.level3.BiochemicalReaction p1 = BioPAX3Utils.getObjectFromSet(reactions, "BiochemicalReaction2");
//
//        assertTrue("non existant reaction", p1 == null);
//    }
//
//    @Test
//    public void testCatalyst() {
//        org.biopax.paxtools.model.Model model = testWrite.getModel();
//        if (model == null) {
//            testWrite.createModel();
//            model = testWrite.getModel();
//        }
//
//        Set<org.biopax.paxtools.model.level3.Catalysis> cats = model.getObjects(org.biopax.paxtools.model.level3.Catalysis.class);
//        assertEquals("num catalysts", cats.size(), 1);
//
//        org.biopax.paxtools.model.level3.Catalysis p = BioPAX3Utils.getObjectFromSet(cats, "Catalysis1");
//
//        assertTrue("catalyst", p != null);
//
//        Set<org.biopax.paxtools.model.level3.Process> order = p.getControlled();
//        assertEquals("num orders", order.size(), 1);
//
//        assertTrue("order value", BioPAX3Utils.contains(order, "BiochemicalReaction1"));
//    }
//
//@Test
//    public void testPathwayStep1() {
//        org.biopax.paxtools.model.Model model = testWrite.getModel();
//        if (model == null) {
//            testWrite.createModel();
//            model = testWrite.getModel();
//        }
//
//        Set<org.biopax.paxtools.model.level3.PathwayStep> pathsteps = model.getObjects(org.biopax.paxtools.model.level3.PathwayStep.class);
//        assertEquals("num pathway steps", pathsteps.size(), 3);
//
//        org.biopax.paxtools.model.level3.PathwayStep p = BioPAX3Utils.getObjectFromSet(pathsteps, "PathwayStep1");
//
//        assertTrue("path step1", p != null);
//
//        Set<org.biopax.paxtools.model.level3.Process> components = p.getStepProcess();
//        assertEquals("num step processes", components.size(), 1);
//
//        assertTrue("step process value", BioPAX3Utils.contains(components, "Pathway2"));
//    }
//
//    @Test
//    public void testPathwayStep2() {
//        org.biopax.paxtools.model.Model model = testWrite.getModel();
//        if (model == null) {
//            testWrite.createModel();
//            model = testWrite.getModel();
//        }
//
//        Set<org.biopax.paxtools.model.level3.PathwayStep> pathsteps = model.getObjects(org.biopax.paxtools.model.level3.PathwayStep.class);
//        assertEquals("num pathway steps", pathsteps.size(), 3);
//
//        org.biopax.paxtools.model.level3.PathwayStep p = BioPAX3Utils.getObjectFromSet(pathsteps, "PathwayStep2");
//
//        assertTrue("path step1", p != null);
//
//        Set<org.biopax.paxtools.model.level3.Process> components = p.getStepProcess();
//        assertEquals("num step processes", components.size(), 1);
//
//        assertTrue("step process value", BioPAX3Utils.contains(components, "Pathway3"));
//    }
//
//    @Test
//    public void testPathwayStep3() {
//        org.biopax.paxtools.model.Model model = testWrite.getModel();
//        if (model == null) {
//            testWrite.createModel();
//            model = testWrite.getModel();
//        }
//
//        Set<org.biopax.paxtools.model.level3.PathwayStep> pathsteps = model.getObjects(org.biopax.paxtools.model.level3.PathwayStep.class);
//        assertEquals("num pathway steps", pathsteps.size(), 3);
//
//        org.biopax.paxtools.model.level3.PathwayStep p = BioPAX3Utils.getObjectFromSet(pathsteps, "PathwayStep3");
//
//        assertTrue("path step1", p != null);
//
//        Set<org.biopax.paxtools.model.level3.Process> components = p.getStepProcess();
//        assertEquals("num step processes", components.size(), 2);
//
//        assertTrue("step process value", BioPAX3Utils.contains(components, "BiochemicalReaction1"));
//        assertTrue("step process value", BioPAX3Utils.contains(components, "Catalysis1"));
//    }
//
//    @Test
//    public void testBiosource1() {
//        org.biopax.paxtools.model.Model model = testWrite.getModel();
//        if (model == null) {
//            testWrite.createModel();
//            model = testWrite.getModel();
//        }
//
//        Set<String> name = new TreeSet<String>();
//        name.add("Saccharomyces cerevisiae");
//
//        Set<org.biopax.paxtools.model.level3.BioSource> sources = model.getObjects(org.biopax.paxtools.model.level3.BioSource.class);
//        assertTrue("num biosources", sources.size() == 1);
//
//        org.biopax.paxtools.model.level3.BioSource p = BioPAX3Utils.getObjectFromSet(sources, "BioSource1");
//        assertTrue("biosource1", p != null);
//        assertEquals("src name", p.getName(), name);
//
//    }
//
//    public void testBiosourceFromPathway1() {
//        org.biopax.paxtools.model.Model model = testWrite.getModel();
//        if (model == null) {
//            testWrite.createModel();
//            model = testWrite.getModel();
//        }
//
//        Set<String> name = new TreeSet<String>();
//        name.add("Saccharomyces cerevisiae");
//
//        Set<org.biopax.paxtools.model.level3.Pathway> pathways = model.getObjects(org.biopax.paxtools.model.level3.Pathway.class);
//        org.biopax.paxtools.model.level3.Pathway p = BioPAX3Utils.getObjectFromSet(pathways, "Pathway1");
//        BioSource src = p.getOrganism();
//        assertTrue("source", src != null);
//        assertEquals("src name", src.getName(), name);
//    }
//
//    public void testBiosourceFromPathway2() {
//        org.biopax.paxtools.model.Model model = testWrite.getModel();
//        if (model == null) {
//            testWrite.createModel();
//            model = testWrite.getModel();
//        }
//
//        Set<String> name = new TreeSet<String>();
//        name.add("Saccharomyces cerevisiae");
//
//        Set<org.biopax.paxtools.model.level3.Pathway> pathways = model.getObjects(org.biopax.paxtools.model.level3.Pathway.class);
//        org.biopax.paxtools.model.level3.Pathway p = BioPAX3Utils.getObjectFromSet(pathways, "Pathway2");
//        BioSource src = p.getOrganism();
//        assertTrue("source", src != null);
//        assertEquals("src name", src.getName(), name);
//    }
//
//    public void testBiosourceFromPathway3() {
//        org.biopax.paxtools.model.Model model = testWrite.getModel();
//        if (model == null) {
//            testWrite.createModel();
//            model = testWrite.getModel();
//        }
//
//        Set<String> name = new TreeSet<String>();
//        name.add("Saccharomyces cerevisiae");
//
//        Set<org.biopax.paxtools.model.level3.Pathway> pathways = model.getObjects(org.biopax.paxtools.model.level3.Pathway.class);
//        org.biopax.paxtools.model.level3.Pathway p = BioPAX3Utils.getObjectFromSet(pathways, "Pathway3");
//        BioSource src = p.getOrganism();
//        assertTrue("source", src != null);
//        assertEquals("src name", src.getName(), name);
//    }

}
