package com.haesung.watchvoice.watch.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haesung.watchvoice.watch.watchContainer
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var voiceViewModel: VoiceViewModel
    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        voiceViewModel.handleRecognitionResult(result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = watchContainer
        setContent {
            val connectionViewModel: ConnectionViewModel = viewModel {
                ConnectionViewModel(container.commandTransport)
            }
            val voiceModel: VoiceViewModel = viewModel {
                VoiceViewModel(container.commandTransport, container.intentParser)
            }
            voiceViewModel = voiceModel
            val connectionState by connectionViewModel.state.collectAsStateWithLifecycle()
            val voiceState by voiceModel.state.collectAsStateWithLifecycle()
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
