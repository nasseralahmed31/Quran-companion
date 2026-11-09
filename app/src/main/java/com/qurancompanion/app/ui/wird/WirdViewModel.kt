package com.qurancompanion.app.ui.wird

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.qurancompanion.app.domain.model.KhatmState
import com.qurancompanion.app.domain.model.WirdToday
import com.qurancompanion.app.domain.repository.WirdRepository
import com.qurancompanion.app.ui.common.appContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WirdViewModel(private val wird: WirdRepository) : ViewModel() {
    val today: StateFlow<WirdToday?> = wird.observeToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val khatm: StateFlow<KhatmState?> = wird.observeKhatm()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun showMessage(text: String) {
        _message.value = text
    }

    fun clearMessage() {
        _message.value = null
    }

    fun setTargets(readPages: Int, memorizeAyahs: Int, reviewAyahs: Int) = run {
        wird.setTargets(readPages, memorizeAyahs, reviewAyahs)
    }

    fun startKhatm(days: Int) = run {
        wird.startKhatm(days)
        _message.value = "بدأت الختمة. بارك الله لك فيها."
    }

    fun cancelKhatm() = run { wird.cancelKhatm() }

    fun logPages(fromPage: Int, toPage: Int) = run {
        wird.logPages(fromPage, toPage)
        _message.value = "سُجّلت القراءة."
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                _message.value = e.message
            } catch (e: Exception) {
                _message.value = "تعذّر تنفيذ الطلب: ${e.message}"
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { WirdViewModel(appContainer().wirdRepository) }
        }
    }
}
