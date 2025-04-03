package org.reactome.server.tools;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.reactome.server.graph.domain.model.*;
import org.biopax.validator.BiopaxIdentifier;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.ValidatorUtils;
import org.biopax.validator.api.beans.Validation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.List;
import org.biopax.paxtools.model.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class WriteBioPAX3 {
    private static Validator validator;
    // Initialize validator when the class is first loaded
    static {
        try {
            ApplicationContext ctx = new ClassPathXmlApplicationContext(
                "classpath:META-INF/spring/appContext-validator.xml",
                "classpath:META-INF/spring/appContext-loadTimeWeaving.xml"
            );
            validator = (Validator) ctx.getBean("validator");
            System.out.println("BioPAX Validator initialized.");
        } catch (Exception e) {
            System.err.println("Error initializing BioPAX Validator: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private final org.reactome.server.graph.domain.model.Pathway thisPathway;
    private final List<Event> thisListEvents;
    private Pathway parentPathway;

    private static Integer dbVersion = 0;

    private Integer stepNum = 0;
    private Integer pathwayNum = 0;
    private Integer biornNum = 0;
    private Integer catNum = 0;

    private boolean useEventOf = true;

    private BioPAXFactory bioPAXFactory = BioPAXLevel.L3.getDefaultFactory();
    private org.biopax.paxtools.model.Model thisModel = null;

    static String xmlBase;

    /**
     * Construct an instance of the WriteBioPAX3
     */
    public WriteBioPAX3(){
        thisPathway = null;
        thisListEvents = null;
        parentPathway = null;
        xmlBase = "http://www.reactome.org/biopax/#";
        BioPAX3Utils.clearCounterArray();
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
        BioPAX3Utils.clearCounterArray();
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
        BioPAX3Utils.clearCounterArray();
    }

    /**
     * Create the BioPAX model using the Reactome Pathway specified in the constructor.
     */
    void createModel(){
        thisModel = bioPAXFactory.createModel();
        thisModel.setXmlBase(xmlBase);
        BioPAXPathwayBuilder thisBPPath = new BioPAXPathwayBuilder(thisPathway, thisModel);

        thisBPPath.addReactomePathway();
        // validateBioPAXFile();
    }

    public void validateBioPAXFile(File file) {
        try {
            Resource owlResource = new org.springframework.core.io.FileSystemResource(file);
            Validation result = new Validation(new BiopaxIdentifier(), owlResource.getDescription(), false, null, 0, "notstrict");

            validator.importModel(result, owlResource.getInputStream());
            validator.validate(result);

            // Save validation results
            PrintWriter writer = new PrintWriter(file.getPath() + "_validation.xml");
            ValidatorUtils.write(result, writer, null);
            writer.close();

            System.out.println("Validation completed for: " + file.getName());

            // Cleanup
            validator.getResults().remove(result);
        } catch (IOException e) {
            System.err.println("BioPAX validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Set the database version number.
     *
     * @param version  Integer the ReactomeDB version number being used.
     */
    public void setDBVersion(Integer version) {
        dbVersion = version;
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
     * Gets the BioPAX model created
     *
     * @return thisModel BioPAX Model created
     */
    Model getModel() {
        return thisModel;
    }

}
