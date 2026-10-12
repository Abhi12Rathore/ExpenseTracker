package com.example.expensetrackerpro.domain.models

/** The text found in a photo, as the lines a person would read, top to bottom. */
data class RecognizedText(val lines: List<TextLine>)

/**
 * One line of text across the photo. [top] and [height] are in the photo's pixels; the height
 * tells large print, such as a shop's name, from the rest.
 */
data class TextLine(val text: String, val top: Int, val height: Int)
