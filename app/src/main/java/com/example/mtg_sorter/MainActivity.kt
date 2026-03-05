package com.example.mtg_sorter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mtg_sorter.data.model.LoadedDeck
import com.example.mtg_sorter.data.model.ScryfallCard
import com.example.mtg_sorter.ui.CameraPreview
import com.example.mtg_sorter.ui.ScannerViewModel
import com.example.mtg_sorter.ui.theme.MTG_SorterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MTG_SorterTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: ScannerViewModel = viewModel()) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onTextDetected = { text ->
                        viewModel.onTextDetected(text)
                    }
                )

                // Guidance overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(top = 64.dp)
                            .background(Color.Transparent)
                            .padding(4.dp)
                    ) {
                        // Corner borders for the scan window
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                        
                        // Top-left
                        Box(modifier = Modifier.align(Alignment.TopStart).size(20.dp).background(Color.White))
                        // Top-right
                        Box(modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(Color.White))
                        // Bottom-left
                        Box(modifier = Modifier.align(Alignment.BottomStart).size(20.dp).background(Color.White))
                        // Bottom-right
                        Box(modifier = Modifier.align(Alignment.BottomEnd).size(20.dp).background(Color.White))

                        Text(
                            text = "Align Card Name Here",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                ScannerOverlay(viewModel)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Camera permission required to scan cards")
            }
        }
    }
}

@Composable
fun ScannerOverlay(viewModel: ScannerViewModel) {
    val detectedText by viewModel.detectedText.collectAsState()
    val scannedCard by viewModel.scannedCard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDeckDialog by remember { mutableStateOf(false) }
    var deckInput by remember { mutableStateOf("") }

    val decks by viewModel.decks.collectAsState()
    val matches = viewModel.computeMatchesFor(scannedCard?.name)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = "Detected: ${scannedCard?.name ?: "scanning"}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                if (decks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Active Decks: ${decks.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { showDeckDialog = true }) {
                Text("Add deck")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Overall Deck Progress
        if (decks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Deck Progress:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                decks.forEach { deck ->
                    val missing = deck.totalCards - deck.totalCollected
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${deck.name}: ${deck.totalCollected}/${deck.totalCards} (Missing: $missing)",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.removeDeck(deck.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove deck",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isLoading) {
            CircularProgressIndicator(color = Color.White)
        }

        if (scannedCard != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Put into:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (matches.isEmpty()) {
                    Text(
                        text = "No matching deck yet",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    matches.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${m.deckName}: ${m.collected}/${m.total}",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = { viewModel.sortCardIntoDeck(m.deckId, scannedCard!!.name) },
                                enabled = m.collected < m.total
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeckDialog) {
        AlertDialog(
            onDismissRequest = { showDeckDialog = false },
            title = { Text("Add Moxfield deck") },
            text = {
                Column {
                    Text("Enter deck ID or URL")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deckInput,
                        onValueChange = { deckInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Deck ID or URL") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addDeck(deckInput)
                    showDeckDialog = false
                    deckInput = ""
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeckDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CardInfo(card: ScryfallCard) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Text(
            text = card.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${card.setName} (${card.set})",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        AsyncImage(
            model = card.imageUris?.normal,
            contentDescription = card.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        card.oracleText?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}