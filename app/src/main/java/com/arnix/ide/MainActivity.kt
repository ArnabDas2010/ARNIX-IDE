package com.arnix.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.arnix.ide.ui.screens.ArnixMainScreen
import com.arnix.ide.ui.theme.ArnixTheme
import com.arnix.ide.ui.theme.IdeBackground
import com.arnix.ide.ui.viewmodel.IdeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: IdeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArnixTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = IdeBackground
                ) {
                    ArnixMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
