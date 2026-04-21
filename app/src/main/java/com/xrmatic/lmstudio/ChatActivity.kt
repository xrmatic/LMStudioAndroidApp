package com.xrmatic.lmstudio

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.xrmatic.lmstudio.adapter.MessageAdapter
import com.xrmatic.lmstudio.databinding.ActivityChatBinding
import com.xrmatic.lmstudio.viewmodel.ChatViewModel

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val messageAdapter = MessageAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupInputArea()
        observeViewModel()

        viewModel.loadModels()
    }

    // ── Menu ───────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        R.id.action_clear -> {
            confirmClearConversation()
            true
        }
        R.id.action_refresh_models -> {
            viewModel.loadModels()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // ── Setup ──────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recyclerMessages.apply {
            this.layoutManager = layoutManager
            adapter = messageAdapter
        }
    }

    private fun setupInputArea() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessage.text?.clear()
            }
        }
    }

    // ── Observers ──────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messages ->
            messageAdapter.submitList(messages.toList())
            if (messages.isNotEmpty()) {
                binding.recyclerMessages.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSend.isEnabled = !loading
        }

        viewModel.error.observe(this) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.models.observe(this) { models ->
            val modelIds = models.map { it.id }
            val spinnerAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                modelIds
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerModel.adapter = spinnerAdapter

            // Select persisted model
            val selected = viewModel.selectedModel.value
            val idx = modelIds.indexOf(selected)
            if (idx >= 0) binding.spinnerModel.setSelection(idx)

            binding.spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    viewModel.selectModel(modelIds[position])
                }
                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }

            // Show/hide empty-state hint
            binding.tvNoModels.visibility = if (models.isEmpty()) View.VISIBLE else View.GONE
            binding.spinnerModel.visibility = if (models.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun confirmClearConversation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_conversation)
            .setMessage(R.string.clear_conversation_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.clearConversation() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
