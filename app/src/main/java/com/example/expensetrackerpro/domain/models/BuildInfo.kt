package com.example.expensetrackerpro.domain.models

/** What kind of build is running, for features that must not reach users. */
data class BuildInfo(
    val isDebug: Boolean,
    // As shown under About, e.g. 1.0
    val versionName: String = ""
)
