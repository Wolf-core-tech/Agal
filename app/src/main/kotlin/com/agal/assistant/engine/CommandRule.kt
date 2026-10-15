package com.agal.assistant.engine

import android.content.Context
import com.agal.assistant.engine.models.CommandResult

interface CommandRule {
    val name: String
    fun matches(input: String): Boolean
    suspend fun execute(input: String, context: Context): CommandResult
}
