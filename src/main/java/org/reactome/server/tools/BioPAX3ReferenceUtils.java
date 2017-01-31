package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.Summation;

import java.util.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

class BioPAX3ReferenceUtils {

    /**
     * Function to create a text comment from the Summation
     *
     * @param summations List of Reactome Summation
     *
     * @return text representation of the notes in the summations
     */
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

    /**
     * Removes any xhtml mark-up from the text
     *
     * @param notes text to clean up
     *
     * @return the text with any xhtml mark up removed
     */
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
