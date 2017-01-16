package org.reactome.server.tools;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Pathway;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.domain.model.Event;


import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.biopax.paxtools.model.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */
class BioPAX3Utils {

    static String getID(String id){
        return (WriteBioPAX3.xmlBase + id);
    }

//    static public String getReactomeId(DatabaseObject object){
//        return getReactomeId(object.getStId());
//
//    }

    static <T extends org.biopax.paxtools.model.BioPAXElement> T getObjectFromSet(Set<T> pathways, String id) {
        T p = null;
        Boolean found = false;
        for (T pathway : pathways) {
            p = pathway;
            if (p.getRDFId().equals(getID(id))) {
                found = true;
                break;
            }
        }
        if (found){
            return p;
        }
        else {
            return null;
        }
    }

}
