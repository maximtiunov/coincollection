package com.mt.coincollection

import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mt.coincollection.data.City
import com.mt.coincollection.data.Coin
import com.mt.coincollection.ui.theme.MainViewModel
import com.mt.coincollection.ui.theme.CoinCollectionTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoinCollectionTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

// --- ГЛАВНАЯ НАВИГАЦИЯ ---
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    // Простая навигация через состояние
    var currentCityId by remember { mutableStateOf<Int?>(null) }

    if (currentCityId == null) {
        // Экран 1: Список городов
        CityListScreen(viewModel) { cityId ->
            currentCityId = cityId
            viewModel.selectedCityId = cityId
        }
    } else {
        // Экран 2: Монеты в городе
        CoinListScreen(viewModel, currentCityId!!) {
            currentCityId = null
            viewModel.selectedCityId = null
        }
    }
}

// --- ЭКРАН 1: ГОРОДА ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityListScreen(viewModel: MainViewModel, onCityClick: (Int) -> Unit) {
    val cities by viewModel.cities.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var cityNameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Сашины монетки") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить город")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(cities) { city ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onCityClick(city.id) }
                ) {
                    Text(
                        text = city.name,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Диалог добавления города
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Новый город") },
            text = {
                OutlinedTextField(
                    value = cityNameInput,
                    onValueChange = { cityNameInput = it },
                    label = { Text("Название города") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (cityNameInput.isNotBlank()) {
                        viewModel.addCity(cityNameInput)
                        cityNameInput = ""
                        showDialog = false
                    }
                }) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Отмена") }
            }
        )
    }
}

// --- ЭКРАН 2: МОНЕТЫ ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinListScreen(viewModel: MainViewModel, cityId: Int, onBackClick: () -> Unit) {
    val coins by viewModel.getCoinsForCity(cityId).collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Лаунчер для выбора фото из галереи (современный Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Если фото выбрано, сразу добавляем монету (для упрощения UI)
            viewModel.addCoin(cityId, "Монета", uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Монеты") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) { Text("Назад") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить монету")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(coins) { coin ->
                CoinItem(coin = coin)
            }
        }
    }

    // Диалог добавления монеты
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новая монета") },
            text = { Text("Сейчас откроется галерея для выбора фото монеты.") },
            confirmButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) { Text("Выбрать фото") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Отмена") }
            }
        )
    }
}

// --- КАРТОЧКА МОНЕТЫ ---
@Composable
fun CoinItem(coin: Coin) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            // Загрузка картинки из файла
            AsyncImage(
                model = File(coin.imagePath),
                contentDescription = coin.name,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = coin.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Путь: ${coin.imagePath.takeLast(20)}...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}