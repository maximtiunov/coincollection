package com.mt.coincollection

import android.os.Bundle
import androidx.compose.foundation.gestures.rememberTransformableState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import android.net.Uri
import androidx.core.content.FileProvider
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
        topBar = { TopAppBar(title = { Text("Города") }) },
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
    var showSourceDialog by remember { mutableStateOf(false) }

    // Для создания временного файла камеры
    val context = LocalContext.current
    val tempPhotoUri = remember { mutableStateOf<Uri?>(null) }

    // Лаунчер для выбора фото из галереи
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addCoin(cityId, "Монета", uri)
        }
    }

    // Лаунчер для камеры
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri.value?.let { uri ->
                viewModel.addCoin(cityId, "Монета", uri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Коллекция") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) { Text("Назад") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSourceDialog = true }) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(coins) { coin ->
                CoinItem(coin = coin)
            }
        }
    }

    // Диалог выбора источника (камера или галерея)
    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Добавить монету") },
            text = { Text("Выберите способ добавления фото") },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        showSourceDialog = false
                        // Создаем временный файл для камеры
                        val tempFile = File(context.cacheDir, "camera_photos/temp_photo.jpg")
                        tempFile.parentFile?.mkdirs()
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )
                        tempPhotoUri.value = uri
                        cameraLauncher.launch(uri)
                    }) {
                        Text("📷 Сфотографировать")
                    }
                    TextButton(onClick = {
                        showSourceDialog = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Text("🖼️ Выбрать из галереи")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Отмена") }
            }
        )
    }
}

// --- КАРТОЧКА МОНЕТЫ ---
@Composable
fun CoinItem(coin: Coin) {
    // Состояние для открытия полноэкранного просмотра
    var showFullscreen by remember { mutableStateOf(false) }

    // Если нужно показать полный экран
    if (showFullscreen) {
        FullscreenImageDialog(imagePath = coin.imagePath) {
            showFullscreen = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { showFullscreen = true } // Делаем всю карточку кликабельной
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = File(coin.imagePath),
                contentDescription = coin.name,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = coin.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Путь: ${coin.imagePath.takeLast(20)}...",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun FullscreenImageDialog(imagePath: String, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Создаем состояние для трансформации
    val transformableState = rememberTransformableState { zoomChange, panChange, rotationChange ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = offset + panChange
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Кнопка закрытия (X)
            Text(
                text = "✕",
                color = Color.White,
                fontSize = 32.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clickable { onDismiss() }
            )

            // Изображение с зумом
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Монета на весь экран",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = transformableState),
                contentScale = ContentScale.Fit
            )
        }
    }
}