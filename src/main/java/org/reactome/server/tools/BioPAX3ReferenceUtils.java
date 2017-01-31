package org.reactome.server.tools;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Pathway;
import org.reactome.server.graph.domain.model.*;
import org.reactome.server.graph.domain.model.Event;


import java.io.*;
import java.util.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class BioPAX3ReferenceUtils {

    static String getComment(List <org.reactome.server.graph.domain.model.Summation> summations) {
        String comment = "";
        if (summations != null) {
            for (Summation s : summations) {
                if (comment.length() == 0) {
                    comment += removeTags(s.getText());
                } else {
                    comment += System.getProperty("line.separator");
                    comment += removeTags(s.getText());
                }
            }
        }
        return comment;
    }

    private static String removeTags(String notes) {
        notes = notes.replaceAll("\\p{Cntrl}+", " ");
        notes = notes.replaceAll("</*[a-zA-Z][^>]*>", " ");
        notes = notes.replaceAll("<>", " interconverts to ");
        notes = notes.replaceAll("<", " ");
        notes = notes.replaceAll("\n+", "  ");
        notes = notes.replaceAll("&+", "  ");
        return notes;
    }

 }
