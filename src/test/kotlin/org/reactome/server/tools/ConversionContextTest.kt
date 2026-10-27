package org.reactome.server.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConversionContextTest {
    @Test
    fun `numbers each class's ids from 1, on the xml base`() {
        val context = ConversionContext("http://www.reactome.org/biopax/97/1#")
        assertEquals("http://www.reactome.org/biopax/97/1#Pathway1", context.nextId("Pathway"))
        assertEquals("http://www.reactome.org/biopax/97/1#Protein1", context.nextId("Protein"))
        assertEquals("http://www.reactome.org/biopax/97/1#Pathway2", context.nextId("Pathway"))
        assertEquals("http://www.reactome.org/biopax/97/1#Provenance1", context.id("Provenance1"))
        assertEquals("http://www.reactome.org/biopax/97/1#", context.model.xmlBase)
    }

    @Test
    fun `a conversion's ids are its own`() {
        val one = ConversionContext("a#")
        val other = ConversionContext("b#")
        one.nextId("Pathway")
        one.nextId("Pathway")
        assertEquals("b#Pathway1", other.nextId("Pathway"))
        assertEquals("a#Pathway3", one.nextId("Pathway"))
    }
}
