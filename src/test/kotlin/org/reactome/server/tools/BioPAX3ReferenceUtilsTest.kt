package org.reactome.server.tools

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.reactome.server.graph.domain.model.Summation

class BioPAX3ReferenceUtilsTest {
    
    @Test
    fun `test getComment with null input returns empty string`() {
        val result = BioPAX3ReferenceUtils.getComment(null)
        assertEquals("", result)
    }
    
    @Test
    fun `test getComment with empty list returns empty string`() {
        val result = BioPAX3ReferenceUtils.getComment(emptyList())
        assertEquals("", result)
    }
    
    @Test
    fun `test getComment combines multiple summations with line separator`() {
        val summations = listOf(
            createSummation("First comment"),
            createSummation("Second comment")
        )
        
        val result = BioPAX3ReferenceUtils.getComment(summations)
        assertEquals("First comment\nSecond comment", result)
    }
    
    @Test
    fun `test removeTags removes HTML tags and special characters`() {
        val input = "<p>This is a <b>test</b> with &lt;special&gt; characters</p>"
        val expected = "This is a test with special characters"
        
        // Since removeTags is private, we'll test it through getComment
        val result = BioPAX3ReferenceUtils.getComment(listOf(createSummation(input)))
        assertEquals(expected, result.trim())
    }
    
    @Test
    fun `test removeTags handles interconverts to syntax`() {
        val input = "A<>B"
        val expected = "A interconverts to B"
        
        val result = BioPAX3ReferenceUtils.getComment(listOf(createSummation(input)))
        assertEquals(expected, result.trim())
    }
    
    @Test
    fun `test removeTags handles control characters`() {
        val input = "Text with\u0000control\u0001characters"
        val expected = "Text with control characters"
        
        val result = BioPAX3ReferenceUtils.getComment(listOf(createSummation(input)))
        assertEquals(expected, result.trim())
    }
    
    @Test
    fun `test removeTags handles multiple line breaks`() {
        val input = "First line\n\n\nSecond line"
        val expected = "First line  Second line"
        
        val result = BioPAX3ReferenceUtils.getComment(listOf(createSummation(input)))
        assertEquals(expected, result.trim())
    }
    
    @Test
    fun `test removeTags handles ampersands`() {
        val input = "Text with &&& ampersands"
        val expected = "Text with   ampersands"
        
        val result = BioPAX3ReferenceUtils.getComment(listOf(createSummation(input)))
        assertEquals(expected, result.trim())
    }
    
    @Test
    fun `test getComment handles complex HTML`() {
        val input = "<div class=\"test\"><p>This is a <b>complex</b> <i>HTML</i> <a href=\"http://example.com\">link</a></p></div>"
        val expected = "This is a complex HTML link"
        
        val result = BioPAX3ReferenceUtils.getComment(listOf(createSummation(input)))
        assertEquals(expected, result.trim())
    }
    
    private fun createSummation(text: String): Summation {
        return Summation().apply {
            this.text = text
        }
    }
} 