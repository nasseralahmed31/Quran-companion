package com.qurancompanion.app.domain.util

import com.qurancompanion.app.domain.model.MasteryLevel
import com.qurancompanion.app.domain.model.ReviewItem
import com.qurancompanion.app.domain.model.ReviewStatus

/**
 * Order of the review list. Passages that need review (weak or flagged) always come before
 * passages that are in good shape, even when the latter have been waiting longer.
 */
object ReviewOrdering {
    const val GROUP_NEEDS_REVIEW = 0
    const val GROUP_REGULAR = 1
    const val GROUP_UPCOMING = 2
    const val GROUP_DONE = 3
    const val GROUP_LATER = 4

    fun needsReview(mastery: MasteryLevel): Boolean =
        mastery == MasteryLevel.WEAK || mastery == MasteryLevel.NEEDS_REVIEW

    /** Lower groups are shown first. */
    fun group(item: ReviewItem): Int = when (item.status) {
        ReviewStatus.OVERDUE, ReviewStatus.DUE_TODAY ->
            if (needsReview(item.mastery)) GROUP_NEEDS_REVIEW else GROUP_REGULAR
        ReviewStatus.UPCOMING -> GROUP_UPCOMING
        ReviewStatus.DONE -> GROUP_DONE
        ReviewStatus.LATER -> GROUP_LATER
    }

    fun masteryRank(mastery: MasteryLevel): Int = when (mastery) {
        MasteryLevel.WEAK -> 0
        MasteryLevel.NEEDS_REVIEW -> 1
        MasteryLevel.GOOD -> 2
        MasteryLevel.MASTERED -> 3
        else -> 4
    }

    fun sectionLabel(group: Int): String = when (group) {
        GROUP_NEEDS_REVIEW -> "تحتاج مراجعة أولًا"
        GROUP_REGULAR -> "مراجعة اعتيادية"
        GROUP_UPCOMING -> "قريبة"
        GROUP_DONE -> "مكتملة"
        else -> "لاحقة"
    }

    val comparator: Comparator<ReviewItem> =
        compareBy<ReviewItem> { group(it) }
            .thenBy { masteryRank(it.mastery) }
            .thenByDescending { it.daysOverdue }
            .thenBy { it.nextReviewDay }
            .thenBy { it.entryId }
}
