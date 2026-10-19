package com.downloader.app.ui.settings

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downloader.app.DownloaderApp
import com.downloader.app.R
import com.downloader.app.core.engine.EngineManager
import com.downloader.app.core.storage.PrefsRepository
import com.downloader.app.ui.theme.DownloaderColors

@Composable
fun DebugScreen(onClose: () -> Unit, onOpenLog: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val engineState by EngineManager.state.collectAsStateWithLifecycle()
    val turbo by PrefsRepository.turbo.collectAsStateWithLifecycle()

    val info = rememberDebugInfo(engineStateLabel(engineState), turbo)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = DownloaderApp.str(R.string.action_back),
                )
            }
            Text(
                text = DownloaderApp.str(R.string.settings_debug_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            info.forEach { (label, value) ->
                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = DownloaderColors.SurfaceElevated),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(info.joinToString("\n") { "${it.first}: ${it.second}" }))
                        Toast.makeText(
                            context,
                            DownloaderApp.str(R.string.debug_copied),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(DownloaderApp.str(R.string.debug_copy_info))
                }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = onOpenLog,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(DownloaderApp.str(R.string.debug_open_log))
                }
            }
        }
    }
}

private fun engineStateLabel(state: EngineManager.State): String = when (state) {
    is EngineManager.State.Ready -> DownloaderApp.str(R.string.engine_ready)
    is EngineManager.State.Preparing -> DownloaderApp.str(R.string.engine_preparing)
    is EngineManager.State.Failed -> DownloaderApp.str(R.string.engine_failed) + ": " + state.message
    else -> DownloaderApp.str(R.string.engine_preparing)
}

@Suppress("DEPRECATION")
private fun rememberDebugInfo(engine: String, turbo: Boolean): List<Pair<String, String>> {
    val context = DownloaderApp.ctx()
    val version = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "${info.versionName} (${info.versionCode})"
    } catch (_: PackageManager.NameNotFoundException) {
        "-"
    }
    return listOf(
        DownloaderApp.str(R.string.debug_app) to version,
        DownloaderApp.str(R.string.debug_device) to "${Build.MANUFACTURER} ${Build.MODEL}",
        DownloaderApp.str(R.string.debug_android) to "API ${Build.VERSION.SDK_INT} • ${Build.VERSION.RELEASE}",
        DownloaderApp.str(R.string.debug_engine) to engine,
        DownloaderApp.str(R.string.debug_folder) to (
            PrefsRepository.folderUriValue() ?: DownloaderApp.str(R.string.settings_folder_default)
            ),
        DownloaderApp.str(R.string.debug_turbo) to if (turbo) "ON" else "OFF",
    )
}
