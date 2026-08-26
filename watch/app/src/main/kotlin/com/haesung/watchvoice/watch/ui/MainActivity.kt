package com.haesung.watchvoice.watch.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haesung.watchvoice.watch.watchContainer
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val voiceViewModel: VoiceViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val container = watchContainer
                return VoiceViewModel(
                    container.commandTransport,
                    container.intentParser,
                ) as T
            }
        }
    }

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val transcript = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            voiceViewModel.onTranscript(transcript)
        } else {
            voiceViewModel.onCancelled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = watchContainer
        setContent {
            val connectionViewModel: ConnectionViewModel = viewModel {
                ConnectionViewModel(container.commandTransport)
            }
            val connectionState by connectionViewModel.state.collectAsStateWithLifecycle()
            val voiceState by voiceViewModel.state.collectAsStateWithLifecycle()
            ConnectionScreen(
                state = connectionState,
                onCheck = connectionViewModel::check,
                voiceState = voiceState,
                onListen = ::startSpeechRecognition,
            )
        }
    }

    private fun startSpeechRecognition() {
        voiceViewModel.beginListening()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            voiceViewModel.recognizerUnavailable()
        }
    }
}
