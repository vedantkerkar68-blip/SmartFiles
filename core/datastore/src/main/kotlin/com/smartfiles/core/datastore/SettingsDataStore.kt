package com.smartfiles.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "smartfiles_settings")

/**
 * Holds the application's Preferences DataStore instance. Value objects and the
 * domain-facing repository live in the data layer, keeping this module free of
 * domain dependencies.
 */
class SettingsDataStore(val context: Context) {
    val data: DataStore<Preferences> get() = context.settingsDataStore
}
