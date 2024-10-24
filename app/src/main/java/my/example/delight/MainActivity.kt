package my.example.delight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import my.example.delight.data.DelightModel
import my.example.delight.ui.Users
import my.example.delight.ui.theme.DelightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val model by viewModels<DelightModel>()
        setContent {
            DelightTheme {
                val snack = remember { SnackbarHostState() }
                LaunchedEffect(model.error) {
                    model.error?.let {
                        snack.showSnackbar(it.substringBefore('['), withDismissAction = true)
                        model.error = null
                    }
                }

                Scaffold(
                    Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snack) }
                ) { innerPadding ->
                    var tab by rememberSaveable { mutableIntStateOf(0) }
                    val api = remember(tab) {
                        when (tab) {
                            0 -> model.dbUsers
                            1 -> model.apiUsers
                            else -> model.nopUsers
                        }
                    }
                    BackHandler {
                        finish()
                        tab = 2
                    }
                    Column(Modifier.padding(innerPadding)) {
                        TabRow(tab) {
                            Tab(tab == 0, { tab = 0 }) {
                                Text("SQLDelight: database.db")
                            }
                            Tab(tab == 1, { tab = 1 }) {
                                Text("Ktor API: http://10.0.2.2")
                            }
                        }
                        Users(api)
                    }
                }
            }
        }
    }
}

