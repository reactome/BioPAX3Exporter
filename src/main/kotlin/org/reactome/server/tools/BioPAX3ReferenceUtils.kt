package org.reactome.server.tools;

import org.reactome.server.graph.domain.model.Summation;

import java.util.*;

/**
 * @author Sarah Keating <skeating@ebi.ac.uk>
 */

object BioPAX3ReferenceUtils {

    /**
     * Function to create a text comment from the Summation
     *
     * @param summations List of Reactome Summation
     *
     * @return text representation of the notes in the summations
     */
    fun getComment(summations: List<Summation>?): String {
        var comment = ""
        if (summations != null) {
            for (s in summations) {
                if (comment.isEmpty()) {
                    comment += removeTags(s.text)
                } else {
                    comment += System.getProperty("line.separator")
                    comment += removeTags(s.text)
                }
            }
        }
        return comment
    }

    /**
     * Removes any xhtml mark-up from the text
     *
     * @param notes text to clean up
     *
     * @return the text with any xhtml mark up removed
     */
    private fun removeTags(notes: String): String {
        var result = notes
            .replace(Regex("\\p{Cntrl}+"), " ")
            .replace(Regex("</*[a-zA-Z][^>]*>"), " ")
            .replace("<>", " interconverts to ")
            .replace("<", " ")
            .replace(Regex("\n+"), "  ")
            .replace(Regex("&+"), "  ")
        return result
    }
}
