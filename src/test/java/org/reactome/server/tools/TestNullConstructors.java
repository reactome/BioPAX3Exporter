package org.reactome.server.tools;

import com.martiansoftware.jsap.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.BiochemicalReaction;
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
public class TestNullConstructors

{
    @Test
    public void testBioPAXPathway() {
        BioPAXPathway bpPath = new BioPAXPathway();
        bpPath.addReactomePathway();
        assertTrue( "BioPAXPathway constructor failed", bpPath != null );
    }

    @Test
    public void testBioPAXInteraction() {
        BioPAXInteraction bpInteract = new BioPAXInteraction();
        assertTrue( "BioPAXInteraction constructor failed", bpInteract != null );

        BiochemicalReaction bpRn = bpInteract.addReactomeRLEvent();
        assertTrue("addRLE", bpRn == null);

    }

}
