package com.haesung.watchvoice.watch.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haesung.watchvoice.watch.watchContainer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transport = watchContainer.commandTransport
        setContent {
            val viewModel: ConnectionViewModel = viewModel {
                ConnectionViewModel(transport)
            }
            val state by viewModel.state.collectAsStateWithLifecycle()
            ConnectionScreen(state = state, onCheck = viewModel::check)
        }
    }
}
