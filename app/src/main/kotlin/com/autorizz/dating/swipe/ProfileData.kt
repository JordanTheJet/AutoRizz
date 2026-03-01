package com.autorizz.dating.swipe

data class ProfileData(
    val name: String,
    val age: Int? = null,
    val bio: String? = null,
    val prompts: List<PromptAnswer> = emptyList(),
    val photoDescriptions: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val distance: String? = null,
    val location: String? = null
) {
    fun toPromptString(): String = buildString {
        appendLine("Name: $name")
        age?.let { appendLine("Age: $it") }
        distance?.let { appendLine("Distance: $it") }
        location?.let { appendLine("Location: $it") }
        bio?.let { appendLine("Bio: $it") }
        if (prompts.isNotEmpty()) {
            appendLine("Prompts:")
            prompts.forEach { appendLine("  ${it.prompt}: ${it.answer}") }
        }
        if (interests.isNotEmpty()) {
            appendLine("Interests: ${interests.joinToString(", ")}")
        }
        if (photoDescriptions.isNotEmpty()) {
            appendLine("Photos:")
            photoDescriptions.forEachIndexed { i, desc ->
                appendLine("  Photo ${i + 1}: $desc")
            }
        }
    }
}

data class PromptAnswer(
    val prompt: String,
    val answer: String
)
