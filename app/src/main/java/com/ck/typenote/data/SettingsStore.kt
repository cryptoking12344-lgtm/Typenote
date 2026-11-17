package com.ck.typenote.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class InkStyle(val label: String) { Fresh("Fresh"), Faded("Faded"), Smudged("Smudged"), Crisp("Crisp") }
enum class Margins(val label: String, val dp: Int) { Narrow("Narrow", 18), Normal("Normal", 28), Wide("Wide", 42) }

data class Settings(
    val curved: Boolean = false,
    val paper: Int = 0,
    val ink: InkStyle = InkStyle.Fresh,
    val typeSize: Int = 17,
    val margins: Margins = Margins.Normal,
    val strikeThrough: Boolean = true,
    val keySounds: Boolean = true,
    val bell: Boolean = true,
    val haptics: Boolean = true,
    val lamp: Boolean = false,
    val warmth: Int = 65,
    val brightness: Int = 80,
    val beam: Int = 55,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("typenote_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings

    fun update(change: (Settings) -> Settings) {
        val s = change(_settings.value)
        _settings.value = s
        prefs.edit()
            .putBoolean("curved", s.curved)
            .putInt("paper", s.paper)
            .putString("ink", s.ink.name)
            .putInt("typeSize", s.typeSize)
            .putString("margins", s.margins.name)
            .putBoolean("strike", s.strikeThrough)
            .putBoolean("keySounds", s.keySounds)
            .putBoolean("bell", s.bell)
            .putBoolean("haptics", s.haptics)
            .putBoolean("lamp", s.lamp)
            .putInt("warmth", s.warmth)
            .putInt("brightness", s.brightness)
            .putInt("beam", s.beam)
            .apply()
    }

    private fun load(): Settings {
        val d = Settings()
        return Settings(
            curved = prefs.getBoolean("curved", d.curved),
            paper = prefs.getInt("paper", d.paper),
            ink = runCatching { InkStyle.valueOf(prefs.getString("ink", d.ink.name)!!) }.getOrDefault(d.ink),
            typeSize = prefs.getInt("typeSize", d.typeSize),
            margins = runCatching { Margins.valueOf(prefs.getString("margins", d.margins.name)!!) }.getOrDefault(d.margins),
            strikeThrough = prefs.getBoolean("strike", d.strikeThrough),
            keySounds = prefs.getBoolean("keySounds", d.keySounds),
            bell = prefs.getBoolean("bell", d.bell),
            haptics = prefs.getBoolean("haptics", d.haptics),
            lamp = prefs.getBoolean("lamp", d.lamp),
            warmth = prefs.getInt("warmth", d.warmth),
            brightness = prefs.getInt("brightness", d.brightness),
            beam = prefs.getInt("beam", d.beam),
        )
    }
}
