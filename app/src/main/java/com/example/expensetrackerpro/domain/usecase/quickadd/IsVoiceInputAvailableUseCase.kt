package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.speech.VoiceInputAvailability
import javax.inject.Inject

/** Whether the microphone is offered next to the quick-add bar. */
class IsVoiceInputAvailableUseCase @Inject constructor(
    private val availability: VoiceInputAvailability
) {
    operator fun invoke(): Boolean = availability.isAvailable()
}
