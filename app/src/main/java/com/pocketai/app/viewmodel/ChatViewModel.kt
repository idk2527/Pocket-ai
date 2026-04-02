package com.pocketai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketai.app.data.repository.ExpenseRepository
import com.pocketai.app.services.LiteRTService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

data class ChatMessage(val role: String, val content: String)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val liteRTService: LiteRTService
) : ViewModel() {
    
    // Initial system greeting
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("assistant", "Hello! I am your PocketAI financial assistant. Ask me anything about your receipts, like 'What are my top 5 products?' or 'How much did I spend at Walmart?'"))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    val llmStatus = liteRTService.status
    
    fun sendMessage(userText: String) {
        if (_isGenerating.value || userText.isBlank()) return
        
        viewModelScope.launch {
            _isGenerating.value = true
            
            // Add user message
            val currentList = _messages.value.toMutableList()
            currentList.add(ChatMessage("user", userText))
            
            // Add empty assistant message that will stream
            currentList.add(ChatMessage("assistant", ""))
            _messages.value = currentList
            
            // Fetch receipts context
            val expenses = expenseRepository.getAllExpenses().first()
            val recentExpenses = expenses.sortedByDescending { it.date }.take(40) // Limit to ~40 to spare context window
            
            val sb = java.lang.StringBuilder()
            sb.append("| Date | Store | Amt | Items |\n")
            sb.append("|---|---|---|---|\n")
            
            recentExpenses.forEach { exp ->
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(exp.date))
                var itemsStr = "N/A"
                if (!exp.itemsJson.isNullOrBlank()) {
                    try {
                        val itemsArray = JSONArray(exp.itemsJson)
                        val names = mutableListOf<String>()
                        for (i in 0 until itemsArray.length()) {
                            names.add(itemsArray.getJSONObject(i).optString("name", ""))
                        }
                        itemsStr = names.filter { !it.isNullOrBlank() }.joinToString(", ").take(50) // Truncate long lists
                    } catch (e: Exception) {}
                }
                sb.append("| $dateStr | ${exp.storeName} | €${String.format("%.2f", exp.amount)} | $itemsStr |\n")
            }
            
            val mdTable = sb.toString()
            val systemPrompt = """<|im_start|>system
You are a highly intelligent, premium personal finance assistant AI running locally on the user's device. You know every single receipt the user has scanned.
Analyze the following markdown table containing the user's latest receipts to answer their questions accurately. Be friendly, deeply analytical, and helpful. Do NOT hallucinate data not in the table. Keep your responses concise and naturally conversational. Format responses cleanly using markdown when necessary.

Receipts Context:
$mdTable<|im_end|>"""

            // Build conversation history (maintain last 3 complete turns to preserve memory & token limits)
            val promptBuilder = java.lang.StringBuilder(systemPrompt)
            val historyToKeep = currentList.dropLast(1).takeLast(6) // Exclude the blank assistant message we just appended
            
            for (msg in historyToKeep) {
                // Roles are either "user" or "assistant"
                promptBuilder.append("\n<|im_start|>${msg.role}\n${msg.content}<|im_end|>")
            }
            
            // Append the final assistant instruction
            promptBuilder.append("\n<|im_start|>assistant\n")
            
            val prompt = promptBuilder.toString()
            
            var accumulatedText = ""
            
            val fullResponse = liteRTService.generateResponseWithImage(
                image = null,
                prompt = prompt,
                onToken = { token ->
                    accumulatedText += token
                    // Update latest assistant message natively
                    val list = _messages.value.toMutableList()
                    val lastMsg = list.last()
                    list[list.size - 1] = lastMsg.copy(content = accumulatedText)
                    _messages.value = list
                }
            )
            
            if (fullResponse == null) {
                // Handle error
                val list = _messages.value.toMutableList()
                val lastMsg = list.last()
                list[list.size - 1] = lastMsg.copy(content = accumulatedText + "\n[System: Model error.]")
                _messages.value = list
            }
            
            _isGenerating.value = false
        }
    }
}
