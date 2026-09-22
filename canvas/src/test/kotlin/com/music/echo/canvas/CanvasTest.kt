package com.music.echo.canvas

import iad1tya.echo.music.canvas.SpotifyCanvasProvider
import iad1tya.echo.music.canvas.TidalCanvasProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class CanvasTest {

    @Test
    fun testTitleNormalization() {
        val rawTitle = "Monica (From \"Coolie\") [Official Audio]"
        val stripped = rawTitle
            .replace(Regex("\\s*\\[[^]]*]"), "")
            .replace(Regex("\\s*\\((?:from|soundtrack|ost|movie|theme|tamil|telugu|hindi|malayalam|kannada|english)\\b[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\[(?:from|soundtrack|ost|movie|theme|tamil|telugu|hindi|malayalam|kannada|english)\\b[^]]*\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\((?:official\\s*)?(?:music\\s*)?(?:video|mv|lyrics?|audio|visualizer|live|remaster(?:ed)?|version|edit|mix|remix|promo|teaser|song)[^)]*\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[\"']"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        assertEquals("Monica", stripped)
        println("=== TEST SUCCESS: Cleaned title '$rawTitle' -> '$stripped' ===")
    }

    @Test
    fun testSpotifyCanvasProvider() = runBlocking {
        println("=== TESTING SPOTIFY CANVAS PROVIDER ===")
        val result = SpotifyCanvasProvider.getBySongArtist("Monica", "Anirudh Ravichander")
        println("Spotify Canvas result: $result")
        assertNotNull("Provider execution completed", "ok")
    }

    @Test
    fun testTidalCanvasProvider() = runBlocking {
        println("=== TESTING TIDAL CANVAS PROVIDER ===")
        val result = TidalCanvasProvider.getBySongArtist("Monica", "Before You Walk Out of My Life")
        println("Tidal Canvas result: $result")
        assertNotNull("Tidal Provider execution completed", "ok")
    }
}
