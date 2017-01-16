package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.service.DatabaseObjectService;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.config.GraphQANeo4jConfig;

import java.util.Iterator;
import java.util.Set;

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

            for (org.biopax.paxtools.model.level3.Process process: components) {
                assertEquals("component value", process.getRDFId(), BioPAX3Utils.getID("Pathway2"));
            }

            Set<org.biopax.paxtools.model.level3.PathwayStep> order = p.getPathwayOrder();
            assertEquals("num orders", order.size(), 1);

            for (org.biopax.paxtools.model.level3.PathwayStep o: order) {
                assertEquals("order value", o.getRDFId(), BioPAX3Utils.getID("PathwayStep1"));
            }
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

            for (org.biopax.paxtools.model.level3.Process process: components) {
                assertEquals("component value", process.getRDFId(), BioPAX3Utils.getID("Pathway3"));
            }

            Set<org.biopax.paxtools.model.level3.PathwayStep> order = p.getPathwayOrder();
            assertEquals("num orders", order.size(), 1);

            for (org.biopax.paxtools.model.level3.PathwayStep o: order) {
                assertEquals("order value", o.getRDFId(), BioPAX3Utils.getID("PathwayStep2"));
            }
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

            for (org.biopax.paxtools.model.level3.Process process: components) {
                assertEquals("component value", process.getRDFId(), BioPAX3Utils.getID("BiochemicalReaction1"));
            }

            Set<org.biopax.paxtools.model.level3.PathwayStep> order = p.getPathwayOrder();
            assertEquals("num orders", order.size(), 1);

            for (org.biopax.paxtools.model.level3.PathwayStep o: order) {
                assertEquals("order value", o.getRDFId(), BioPAX3Utils.getID("PathwayStep3"));
            }
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

            for (org.biopax.paxtools.model.level3.Process process: components) {
                assertEquals("step process value", process.getRDFId(), BioPAX3Utils.getID("Pathway2"));
            }
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

            for (org.biopax.paxtools.model.level3.Process process: components) {
                assertEquals("step process value", process.getRDFId(), BioPAX3Utils.getID("Pathway3"));
            }
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
            assertEquals("num step processes", components.size(), 1);

            for (org.biopax.paxtools.model.level3.Process process: components) {
                assertEquals("step process value", process.getRDFId(), BioPAX3Utils.getID("BiochemicalReaction1"));
            }
        }
}
