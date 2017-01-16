package org.reactome.server.tools;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.biopax.paxtools.model.level3.PathwayStep;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.domain.model.Event;


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.biopax.paxtools.model.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class WriteBioPAX3 {

    private final Pathway thisPathway;
    private final List<Event> thisListEvents;
    private Pathway parentPathway;

    private static Integer dbVersion = 0;

    private Integer stepNum = 0;
    private Integer pathwayNum = 0;
    private Integer biornNum = 0;

    private boolean addAnnotations = true;
    private boolean inTestMode = false;

    private boolean useEventOf = true;

    private BioPAXFactory bioPAXFactory = BioPAXLevel.L3.getDefaultFactory();
    private Model thisModel = null;

    public static String xmlBase;

    /**
     * Construct an instance of the WriteBioPAX3
     */
    public WriteBioPAX3(){
        thisPathway = null;
        thisListEvents = null;
        parentPathway = null;
        xmlBase = "http://www.reactome.org/biopax/#";
    }

    /**
     * Construct an instance of the WriteBioPAX3 for the specified
     * Pathway.
     *
     * @param pathway  Pathway from ReactomeDB
     */
    public WriteBioPAX3(Pathway pathway){
        thisPathway = pathway;
        thisListEvents = null;
        parentPathway = null;
        xmlBase = "http://www.reactome.org/biopax/" + dbVersion + "/" + thisPathway.getDbId() + "#";
    }

    /**
     * Construct an instance of the WriteBioPAX3 for the specified
     * Pathway.
     *
     * @param pathway Pathway from ReactomeDB
     * @param version Integer - version number of the database
     */
    public WriteBioPAX3(Pathway pathway, Integer version){
        thisPathway = pathway;
        thisListEvents = null;
        parentPathway = null;
        dbVersion = version;
        xmlBase = "http://www.reactome.org/biopax/" + dbVersion + "/" + thisPathway.getDbId() + "#";
    }

    /**
     * Create the BioPAX model using the Reactome Pathway specified in the constructor.
     */
    void createModel(){
        thisModel = bioPAXFactory.createModel();
        thisModel.setXmlBase(xmlBase);

        addAllPathways(thisPathway);
    }

    /**
     * Set the database version number.
     *
     * @param version  Integer the ReactomeDB version number being used.
     */
    public void setDBVersion(Integer version) {
        dbVersion = version;
    }

    //////////////////////////////////////////////////////////////////////////////////

    // Private functions



    //////////////////////////////////////////////////////////////////////////////////

    /**
     * Add elements from the given Pathway. This will rescurse
     * through child Events that represent Pathways.
     *
     * @param pathway  Pathway from ReactomeDB
     */
    private org.biopax.paxtools.model.level3.Pathway addAllPathways(Pathway pathway){
        org.biopax.paxtools.model.level3.Pathway bpPath = addPathway(pathway);
        PathwayStep step = addStep(bpPath);
        bpPath.addPathwayOrder(step);
        if (pathway.getHasEvent() != null) {
            for (Event event : pathway.getHasEvent()) {
                org.biopax.paxtools.model.level3.Pathway childPath = null;
                org.biopax.paxtools.model.level3.BiochemicalReaction childRn;
                childRn = addReaction(event);
                if (event instanceof Pathway){
                    Pathway path = ((Pathway)(event));
                    childPath = addAllPathways(path);
                }
                if (childRn != null){
                    addComponent(bpPath, childRn, step);
                }
                if (childPath != null) {
                    addComponent(bpPath, childPath, step);
                }
            }
        }
        return bpPath;
    }

    private void addComponent(org.biopax.paxtools.model.level3.Pathway bpPath,
                              org.biopax.paxtools.model.level3.Process childPath,
                              PathwayStep step) {
        bpPath.addPathwayComponent(childPath);
        step.addStepProcess(childPath);

    }
    /**
     * Overloaded addReaction function to cast an Event to a Reaction.
     *
     * @param event  Event from ReactomeDB
     */
    private org.biopax.paxtools.model.level3.BiochemicalReaction addReaction(org.reactome.server.graph.domain.model.Event event){
        org.biopax.paxtools.model.level3.BiochemicalReaction rn = null;
        if (event instanceof org.reactome.server.graph.domain.model.ReactionLikeEvent) {
            rn = addReaction((org.reactome.server.graph.domain.model.ReactionLikeEvent ) (event));
        }
        return rn;
    }

    private org.biopax.paxtools.model.level3.Pathway addPathway(org.reactome.server.graph.domain.model.Pathway pathway) {
        org.biopax.paxtools.model.level3.Pathway bpPath =
                thisModel.addNew(org.biopax.paxtools.model.level3.Pathway.class, getPathNumber());

//        bpPath.setOrganism();
        bpPath.setDisplayName(pathway.getDisplayName());
//        PathwayStep step = addStep(bpPath);
//        bpPath.addPathwayOrder(step);

        return bpPath;
    }

    private org.biopax.paxtools.model.level3.PathwayStep addStep(org.biopax.paxtools.model.level3.Pathway bpPath) {
        return thisModel.addNew(org.biopax.paxtools.model.level3.PathwayStep.class, getStepNumber());
    }

//    private String getReactomeId(DatabaseObject object){
//        String resource = "http://identifiers.org/reactome/REACTOME:" + object.getStId();
//        return resource;
//
//    }
    /**
     * Adds the given Reactome Reaction to the SBML model as an SBML Reaction.
     * This in turn adds SBML species and SBML compartments.
     *
     * @param event  Reaction from ReactomeDB
     */
    private org.biopax.paxtools.model.level3.BiochemicalReaction addReaction(org.reactome.server.graph.domain.model.ReactionLikeEvent event){
        BiochemicalReaction bpReaction = thisModel.addNew(BiochemicalReaction.class, getBioReactionNumber());
        bpReaction.setDisplayName(event.getDisplayName());
        return bpReaction;


    }

    private String getStepNumber() {
        stepNum++;
        return xmlBase + "PathwayStep" + stepNum;
    }

    private String getPathNumber() {
        pathwayNum++;
        return xmlBase + "Pathway" + pathwayNum;
    }

    private String getBioReactionNumber() {
        biornNum++;
        return xmlBase + "BiochemicalReaction" + biornNum;
    }

    ///////////////////////////////////////////////////////////////////////////////////

    // functions to output resulting document

    /**
     * Write the Biopax Model to std output.
     */
    public void toStdOut()    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        SimpleIOHandler out = new SimpleIOHandler();
        out.convertToOWL(thisModel, os);
        String output;
        try {
            output = new String(os.toByteArray(), "UTF-8");
        }
        catch (Exception e) {
            output = "failed to write";
        }
        System.out.print(output);
    }

    /**
     * Write the BioPAX Model to a file.
     *
     * @param output File to use.
     */
    void toFile(File output)    {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        SimpleIOHandler out = new SimpleIOHandler();
        out.convertToOWL(thisModel, os);
        try {
            if (!output.exists()) {
                final boolean newFile = output.createNewFile();
            }
            FileOutputStream fop = new FileOutputStream(output);
            fop.write(os.toByteArray());
            fop.flush();
            fop.close();
        }
        catch (Exception e)
        {
            System.out.println("failed to write " + output.getName());
        }
    }

    /**
     * Write the BioPAX Model to a String.
     *
     * @return  String representing the BioPAX Model.
     */
    public String toString()    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        SimpleIOHandler out = new SimpleIOHandler();
        out.convertToOWL(thisModel, os);
        String output;
        try {
            output = new String(os.toByteArray(), "UTF-8");
        }
        catch (Exception e) {
            output = "failed to write";
        }
        return output;
    }

    //////////////////////////////////////////////////////////////////////////////////

    // functions to facilitate testing


    /**
     * Set the addAnnotation flag.
     * This allows testing with and without annotations
     *
     * @param flag  Boolean indicating whether to write out annotations
     */
    void setAnnotationFlag(Boolean flag){
        addAnnotations = flag;
    }

    /**
     * Set the inTestMode flag.
     * This allows testing with/without certain things
     *
     * @param flag  Boolean indicating whether tests are running
     */
    void setInTestModeFlag(Boolean flag){
        inTestMode = flag;
    }


    /**
     * Gets the BioPAX model created
     *
     * @return thisModel BioPAX Model created
     */
    Model getModel() {
        return thisModel;
    }

}
