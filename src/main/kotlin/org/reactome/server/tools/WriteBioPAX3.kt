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
import org.biopax.paxtools.model.*;

import java.io.*;
// import java.util.List;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class WriteBioPAX3 {
    companion object {
        var validator: Validator? = null
        var dbVersion: Int = 0
        var xmlBase: String = ""

        init {
            try {
                // Add JVM arguments to open required modules
                System.setProperty("java.vm.vendor", "Oracle Corporation")
                System.setProperty("java.vm.name", "Java HotSpot(TM) 64-Bit Server VM")
                System.setProperty("java.vm.version", "17")
                System.setProperty("java.vm.specification.version", "17")
                System.setProperty("java.vm.specification.vendor", "Oracle Corporation")
                System.setProperty("java.vm.specification.name", "Java Virtual Machine Specification")
                
                val ctx = ClassPathXmlApplicationContext(
                    "classpath:META-INF/spring/appContext-validator.xml",
                    "classpath:META-INF/spring/appContext-loadTimeWeaving.xml"
                )
                validator = ctx.getBean("validator") as Validator
                println("BioPAX Validator initialized.")
            } catch (e: Exception) {
                System.err.println("Error initializing BioPAX Validator: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private val thisPathway: Pathway?
    private val thisListEvents: List<Event>?
    private var parentPathway: Pathway? = null
    private var stepNum: Int = 0
    private var pathwayNum: Int = 0
    private var biornNum: Int = 0
    private var catNum: Int = 0
    private var useEventOf: Boolean = true
    private val bioPAXFactory = BioPAXLevel.L3.defaultFactory
    private var thisModel: Model? = null

    /**
     * Construct an instance of the WriteBioPAX3
     */
    constructor() {
        thisPathway = null
        thisListEvents = null
        parentPathway = null
        xmlBase = "http://www.reactome.org/biopax/#"
        BioPAX3Utils.clearCounterArray()
    }

    /**
     * Construct an instance of the WriteBioPAX3 for the specified
     * Pathway.
     *
     * @param pathway  Pathway from ReactomeDB
     */
    constructor(pathway: Pathway) {
        thisPathway = pathway
        thisListEvents = null
        parentPathway = null
        xmlBase = "http://www.reactome.org/biopax/$dbVersion/${pathway.dbId}#"
        BioPAX3Utils.clearCounterArray()
    }

    /**
     * Construct an instance of the WriteBioPAX3 for the specified
     * Pathway.
     *
     * @param pathway Pathway from ReactomeDB
     * @param version Integer - version number of the database
     */
    constructor(pathway: Pathway, version: Int) {
        thisPathway = pathway
        thisListEvents = null
        parentPathway = null
        dbVersion = version
        xmlBase = "http://www.reactome.org/biopax/$dbVersion/${pathway.dbId}#"
        BioPAX3Utils.clearCounterArray()
    }

    /**
     * Construct an instance of the WriteBioPAX3 for multiple pathways of a species
     *
     * @param species Species from ReactomeDB
     * @param version Integer - version number of the database
     */
    constructor(species: Species, version: Int) {
        thisPathway = null
        thisListEvents = null
        parentPathway = null
        dbVersion = version
        xmlBase = "http://www.reactome.org/biopax/$dbVersion/species/${species.dbId}#"
        BioPAX3Utils.clearCounterArray()
    }

    /**
     * Create the BioPAX model using the Reactome Pathway specified in the constructor.
     */
    fun createModel() {
        thisModel = bioPAXFactory.createModel()
        thisModel?.xmlBase = xmlBase
        val thisBPPath = BioPAXPathwayBuilder(thisPathway!!, thisModel!!)
        thisBPPath.addReactomePathway()
    }

    /**
     * Create the BioPAX model using multiple pathways for a species
     */
    fun createModelForSpecies(species: Species, pathways: List<Pathway>) {
        thisModel = bioPAXFactory.createModel()
        thisModel?.xmlBase = xmlBase
        
        // Add species information first
        val speciesBuilder = BioPAXSpeciesBuilder(species, thisModel!!)
        speciesBuilder.addReactomeSpecies()
        
        // Add each pathway
        pathways.forEach { pathway ->
            val thisBPPath = BioPAXPathwayBuilder(pathway, thisModel!!)
            thisBPPath.addReactomePathway()
        }
    }

    fun validateBioPAXFile(file: File) {
        try {
            val owlResource = org.springframework.core.io.FileSystemResource(file)
            val result = Validation(BiopaxIdentifier(), owlResource.description, false, null, 0, "notstrict")

            validator?.let { validator ->
                validator.importModel(result, owlResource.inputStream)
                validator.validate(result)

                // Save validation results
                PrintWriter(file.path + "_validation.xml").use { writer ->
                    ValidatorUtils.write(result, writer, null)
                }

                println("Validation completed for: ${file.name}")

                // Cleanup
                validator.results.remove(result)
            }
        } catch (e: IOException) {
            System.err.println("BioPAX validation failed: ${e.message}")
        }
    }
    
    /**
     * Set the database version number.
     *
     * @param version  Integer the ReactomeDB version number being used.
     */
    fun setDBVersion(version: Int) {
        dbVersion = version
    }

    ///////////////////////////////////////////////////////////////////////////////////

    // functions to output resulting document

    /**
     * Write the Biopax Model to std output.
     */
    fun toStdOut() {
        val os = ByteArrayOutputStream()
        val out = SimpleIOHandler()
        out.convertToOWL(thisModel, os)
        val output = try {
            String(os.toByteArray(), Charsets.UTF_8)
        } catch (e: Exception) {
            "failed to write"
        }
        println(output)
    }

    /**
     * Write the BioPAX Model to a file.
     *
     * @param output File to use.
     */
    fun toFile(output: File) {
        val os = ByteArrayOutputStream()
        val out = SimpleIOHandler()
        out.convertToOWL(thisModel, os)
        try {
            if (!output.exists()) {
                output.createNewFile()
            }
            FileOutputStream(output).use { fop ->
                fop.write(os.toByteArray())
                fop.flush()
            }
        } catch (e: Exception) {
            println("failed to write ${output.name}")
        }
    }

    /**
     * Write the BioPAX Model to a String.
     *
     * @return String representing the BioPAX Model.
     */
    override fun toString(): String {
        val os = ByteArrayOutputStream()
        val out = SimpleIOHandler()
        out.convertToOWL(thisModel, os)
        return try {
            String(os.toByteArray(), Charsets.UTF_8)
        } catch (e: Exception) {
            "failed to write"
        }
    }

    //////////////////////////////////////////////////////////////////////////////////

    // functions to facilitate testing

    /**
     * Gets the BioPAX model created
     *
     * @return thisModel BioPAX Model created
     */
    fun getModel(): Model? = thisModel
}
