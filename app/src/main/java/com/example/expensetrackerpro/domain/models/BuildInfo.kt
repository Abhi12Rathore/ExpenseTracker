package com.example.expensetrackerpro.domain.models

/** What kind of build is running, for features that must not reach users. */
data class BuildInfo(val isDebug: Boolean)
