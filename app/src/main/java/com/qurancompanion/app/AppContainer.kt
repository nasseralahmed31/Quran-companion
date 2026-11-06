package com.qurancompanion.app

import com.qurancompanion.app.data.repository.LocalProgressRepository
import com.qurancompanion.app.domain.repository.ProgressRepository

/** Simple manual dependency container. Replaced/extended with Room-backed repositories in phase 2. */
class AppContainer {
    val progressRepository: ProgressRepository = LocalProgressRepository()
}
