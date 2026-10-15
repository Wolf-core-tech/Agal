package com.agal.assistant.engine.rules

import android.content.Context
import com.agal.assistant.engine.CommandRule
import com.agal.assistant.engine.models.CommandResult
import com.agal.assistant.engine.models.CommandType
import kotlin.random.Random

class JokeRule : CommandRule {
    override val name: String = "Joke Rule"

    private val jokes = listOf(
        "Why do programmers prefer dark mode? Because light attracts bugs!",
        "Why was the mobile phone wearing glasses? Because it lost its contacts!",
        "There are only 10 types of people in the world: those who understand binary, and those who don't.",
        "Why did the computer squeak? Someone stepped on its mouse!",
        "What is an astronaut's favorite key on a keyboard? The space bar!",
        "Why did the developer go broke? Because they used up all their cache!",
        "How do you comfort a JavaScript bug? You console it!"
    )

    override fun matches(input: String): Boolean {
        val lower = input.lowercase().trim()
        return lower.contains("joke") || lower.contains("make me laugh")
    }

    override suspend fun execute(input: String, context: Context): CommandResult {
        val joke = jokes[Random.nextInt(jokes.size)]
        return CommandResult(
            type = CommandType.TELL_JOKE,
            rawInput = input,
            spokenReply = joke,
            displayTitle = "Offline Joke",
            displayDescription = joke,
            isSuccess = true
        )
    }
}
