package com.example

import com.example.data.model.GroqModelItemDto
import com.example.data.model.GroqModelRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroqModelParsingTest {

    @Test
    fun `test fromDto converts raw Groq API model correctly`() {
        val dto = GroqModelItemDto(
            id = "llama-3.3-70b-versatile",
            objectType = "model",
            created = 1730000000L,
            ownedBy = "meta",
            active = true,
            contextWindow = 131072
        )

        val modelInfo = GroqModelRegistry.fromDto(dto)
        assertEquals("llama-3.3-70b-versatile", modelInfo.id)
        assertEquals("Meta", modelInfo.provider)
        assertEquals("128k", modelInfo.contextWindow)
        assertTrue(modelInfo.isRecommended)
    }

    @Test
    fun `test fallback models list is not empty`() {
        assertNotNull(GroqModelRegistry.DEFAULT_MODELS)
        assertTrue(GroqModelRegistry.DEFAULT_MODELS.isNotEmpty())
    }
}
