package io.github.clinejj.stop2end

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.clinejj.stop2end.filter.PhoneNumbers
import io.github.clinejj.stop2end.ui.Stop2EndTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = AppSettings(this)
        setContent {
            Stop2EndTheme {
                MainScreen(settings)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(settings: AppSettings) {
    val context = LocalContext.current

    var enabled by remember { mutableStateOf(settings.enabled) }
    var allowlist by remember { mutableStateOf(settings.allowlist.sorted()) }
    var dismissedCount by remember { mutableIntStateOf(settings.dismissedCount) }
    DisposableEffect(settings) {
        val unsubscribe = settings.observe {
            enabled = settings.enabled
            allowlist = settings.allowlist.sorted()
            dismissedCount = settings.dismissedCount
        }
        onDispose { unsubscribe() }
    }

    // Both can change in system settings while the app is in the background.
    var hasListenerAccess by remember { mutableStateOf(false) }
    var hasContactsAccess by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        hasListenerAccess = isListenerEnabled(context)
        hasContactsAccess = ContactsChecker(context).hasPermission()
        onPauseOrDispose { }
    }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasContactsAccess = it
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Stop2End") }) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = "Notification access") {
                    if (hasListenerAccess) {
                        Text("Granted. Stop2End can see and dismiss Google Messages and Google Voice notifications.")
                    } else {
                        Text("Required. Stop2End reads message notifications to find spam and dismiss them.")
                        Text(
                            "If the switch is greyed out, open App info, tap ⋮ › Allow restricted settings, then try again.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row {
                            Button(onClick = { openListenerSettings(context) }) { Text("Grant access") }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { openAppInfo(context) }) { Text("App info") }
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Filtering active", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (enabled) "Spam notifications are dismissed" else "Paused. All notifications are left alone",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = { settings.enabled = it })
                    }
                }
            }

            item {
                SectionCard(title = "Contacts") {
                    Text("Messages from your contacts are never dismissed.")
                    if (!hasContactsAccess) {
                        Text(
                            "Without contacts access, any sender shown by name instead of by number is treated as a contact.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(onClick = { contactsPermission.launch(Manifest.permission.READ_CONTACTS) }) {
                            Text("Allow contacts access")
                        }
                    }
                }
            }

            item {
                AllowlistCard(
                    entries = allowlist,
                    onAdd = settings::addToAllowlist,
                    onRemove = settings::removeFromAllowlist,
                )
            }

            item {
                SectionCard(title = "What gets dismissed") {
                    Text(
                        "Messages containing \"stop to end\", \"end to end\" and variations (stop2end, END2END, " +
                            "STOP=END, stop to quit, stop to opt out, stop to unsubscribe). Messages from " +
                            "${PhoneNumbers.SHORT_CODE_LENGTHS.first}–${PhoneNumbers.SHORT_CODE_LENGTHS.last}-digit " +
                            "short codes, contacts, the allowlist and group conversations are skipped.",
                    )
                    Text(
                        "Only the notification is dismissed. The message stays in your inbox.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dismissed so far: $dismissedCount", Modifier.weight(1f))
                        TextButton(onClick = settings::resetDismissedCount) { Text("Reset") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllowlistCard(entries: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val add = {
        onAdd(input)
        input = ""
    }
    SectionCard(title = "Allowlist") {
        Text("Phone numbers or sender names that should never be dismissed.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Number or name") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = add, enabled = input.isNotBlank()) { Text("Add") }
        }
        entries.forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry, Modifier.weight(1f))
                TextButton(onClick = { onRemove(entry) }) { Text("Remove") }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String? = null, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private fun isListenerEnabled(context: Context): Boolean =
    context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

private fun openListenerSettings(context: Context) {
    val component = ComponentName(context, SpamListenerService::class.java)
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
    } else {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}

private fun openAppInfo(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )
}
