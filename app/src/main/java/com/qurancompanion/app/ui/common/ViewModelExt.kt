package com.qurancompanion.app.ui.common

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.qurancompanion.app.AppContainer
import com.qurancompanion.app.QuranCompanionApp

fun CreationExtras.appContainer(): AppContainer = (this[APPLICATION_KEY] as QuranCompanionApp).container

/** Converts digits to Arabic-Indic numerals (e.g. 12 -> ١٢). */
fun Int.toArabicIndic(): String = toString().map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("")

/** Converts every digit in the string to Arabic-Indic numerals. */
fun String.toArabicDigits(): String = map { if (it in '0'..'9') '٠' + (it - '0') else it }.joinToString("")

fun Double.oneDecimal(): String = String.format(java.util.Locale.US, "%.1f", this).toArabicDigits()
