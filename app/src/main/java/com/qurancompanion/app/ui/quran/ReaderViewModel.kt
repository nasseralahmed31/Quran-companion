package com.qurancompanion.app.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.Achievement
import com.qurancompanion.app.domain.model.Ayah
import com.qurancompanion.app.domain.model.AyahMark
import com.qurancompanion.app.domain.model.AyahMarker
import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.MemorizationState
import com.qurancompanion.app.domain.repository.MemorizationRepository
import com.qurancompanion.app.domain.repository.QuranRepository
import com.qurancompanion.app.domain.repository.SettingsRepository
import com.qurancompanion.app.domain.repository.SimilarityRepository
import com.qurancompanion.app.domain.util.EpochDay
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val quran: QuranRepository,
    private val memorization: MemorizationRepository,
    private val similarity: SimilarityRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val surahNames: StateFlow<Map<Int, String>> = quran.observeSurahs()
        .map { list -> list.associate { it.id to it.nameAr } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val bookmarks: StateFlow<Set<Int>> = settings.settings.map { it.bookmarks }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _markers = MutableStateFlow<Map<Int, Set<AyahMarker>>>(emptyMap())
    val markers: StateFlow<Map<Int, Set<AyahMarker>>> = _markers.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _markers.value = quran.markers()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Marks are decoration; the page still renders without them.
            }
        }
    }

    fun observePage(page: Int): Flow<List<Ayah>> = quran.observePage(page)

    fun observeMarks(page: Int): Flow<Map<Int, AyahMark>> = memorization.observePageMarks(page)

    fun saveLastRead(page: Int) {
        viewModelScope.launch { settings.setLastReadPage(page) }
    }

    fun toggleBookmark(page: Int) {
        viewModelScope.launch { settings.toggleBookmark(page) }
    }

    /** Id of the similar-ayah group that contains [ayahId], or null. */
    suspend fun similarGroupId(ayahId: Int): Int? =
        similarity.loadGroups().firstOrNull { ayahId in it.ayahIds }?.id

    /** Records one ayah straight from the page (a one-ayah record dated today). */
    fun quickMark(ayahId: Int, state: MemorizationState, mastery: MasteryLevel, note: String) {
        viewModelScope.launch {
            try {
                val result = memorization.saveRange(ayahId, ayahId, EpochDay.today(), state, mastery, note.trim())
                _achievements.value = result.newAchievements
                _message.value = "تم تسجيل الآية"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _message.value = "تعذّر التسجيل: ${e.message}"
            }
        }
    }

    fun clearAchievements() {
        _achievements.value = emptyList()
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                ReaderViewModel(c.quranRepository, c.memorizationRepository, c.similarityRepository, c.settingsRepository)
            }
        }
    }
}
