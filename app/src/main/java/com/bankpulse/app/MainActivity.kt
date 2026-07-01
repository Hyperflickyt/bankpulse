package com.bankpulse.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bankpulse.app.data.Prefs
import com.bankpulse.app.ui.screens.*
import com.bankpulse.app.ui.theme.*
import com.bankpulse.app.vm.FinanceViewModel

class MainActivity : ComponentActivity() {

    private val vm: FinanceViewModel by viewModels()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* nothing to backfill; the receiver handles new SMS from here on */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.installTime(this)          // stamp the start-of-tracking moment
        requestPermissionsIfNeeded()
        setContent { BankPulseTheme { Root(vm) } }
    }

    private fun requestPermissionsIfNeeded() {
        val perms = mutableListOf(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)

        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) permLauncher.launch(perms.toTypedArray())
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Txns("Txns", Icons.Filled.ReceiptLong),
    Add("Add", Icons.Filled.AddCircle),
    Reports("Reports", Icons.Filled.BarChart),
    Insights("Insights", Icons.Filled.Lightbulb)
}

@Composable
private fun Root(vm: FinanceViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    if (showSplash) {
        SplashScreen(onDone = { showSplash = false })
        return
    }

    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(Tab.Home) }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, t.label) },
                        label = { Text(t.label, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Gold,
                            selectedTextColor = Gold,
                            indicatorColor = Surface2,
                            unselectedIconColor = TextLo,
                            unselectedTextColor = TextLo
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Home     -> DashboardScreen(state, onClearAll = vm::clearAll)
                Tab.Txns     -> TransactionsScreen(state, onDelete = vm::delete)
                Tab.Add      -> AddScreen(onSave = vm::addManual, onDone = { tab = Tab.Txns })
                Tab.Reports  -> ReportsScreen(state)
                Tab.Insights -> InsightsScreen(state)
            }
        }
    }
}
