package com.arnix.ide

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arnix.ide.ui.screens.ArnixMainScreen
import com.arnix.ide.ui.theme.ArnixTheme
import com.arnix.ide.ui.theme.IdeBackground
import com.arnix.ide.ui.viewmodel.IdeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: IdeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val missing = perms.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
            }
        } else {
            // Android 11+: Check and request All Files Access if missing
            try {
                if (!android.os.Environment.isExternalStorageManager()) {
                    com.arnix.ide.data.repository.StorageAccessHelper.requestAllFilesPermission(this)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
