package com.example.expenses

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expenses.ui.theme.ExpensesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpensesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BalanceScreen()
                }
            }
        }
    }
}

@Composable
fun BalanceScreen(balanceViewModel: BalanceViewModel = viewModel(factory = BalanceViewModel.Factory)) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State for the balance, collected from the ViewModel's Flow
    val balance by balanceViewModel.balance.collectAsState(initial = null)

    // State to manage if we have SMS permission
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasSmsPermission = isGranted
        }
    )

    // Effect to request permission on launch if not already granted
    LaunchedEffect(key1 = true) {
        if (!hasSmsPermission) {
            permissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Live Balance Tracker",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(40.dp))

        if (hasSmsPermission) {
            // Show balance if permission is granted
            Text(
                text = "Current Balance",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = balance?.let { String.format("₹%.2f", it) } ?: "Not Set",
                style = MaterialTheme.typography.displayLarge,
                color = if ((balance ?: 0.0) >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Show initial balance setup only if balance is not yet set
            if (balance == null) {
                InitialBalanceSetup { initialBalance ->
                    coroutineScope.launch {
                        balanceViewModel.setInitialBalance(initialBalance)
                    }
                }
            }
        } else {
            // Show a message if permission is denied
            Text(
                text = "This app requires SMS reading permission to function automatically. Please grant the permission.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) }) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
fun InitialBalanceSetup(onBalanceSet: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Set Initial Balance") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = {
        val initialBalance = text.toDoubleOrNull()
        if (initialBalance != null) {
            onBalanceSet(initialBalance)
        }
    }) {
        Text("Save Initial Balance")
    }
}