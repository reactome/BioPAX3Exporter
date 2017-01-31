package org.reactome.server.tools;

import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
public class TestNullConstructors

{
    @Test
    public void testBioPAXPathway() {
        BioPAXPathwayBuilder bpPath = new BioPAXPathwayBuilder();
        bpPath.addReactomePathway();
        assertTrue( "BioPAXPathwayBuilder constructor failed", bpPath != null );
    }

    @Test
    public void testBioPAXInteraction() {
        BioPAXInteractionBuilder bpInteract = new BioPAXInteractionBuilder();
        assertTrue( "BioPAXInteractionBuilder constructor failed", bpInteract != null );

        BiochemicalReaction bpRn = bpInteract.addReactomeRLEvent();
        assertTrue("addRLE", bpRn == null);

    }

}
