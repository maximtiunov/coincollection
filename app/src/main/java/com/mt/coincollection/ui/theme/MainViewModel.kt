package com.mt.coincollection.ui.theme

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mt.coincollection.data.AppDatabase
import com.mt.coincollection.data.City
import com.mt.coincollection.data.Coin
import com.mt.coincollection.utils.ImageHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).appDao()

    // Получаем список всех городов
    val cities = dao.getAllCities().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Текущий выбранный город (если null - показываем список городов)
    var selectedCityId: Int? = null

    // Получаем монеты для конкретного города
    fun getCoinsForCity(cityId: Int) = dao.getCoinsByCity(cityId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addCity(name: String) {
        viewModelScope.launch {
            dao.insertCity(City(name = name))
        }
    }

    fun addCoin(cityId: Int, coinName: String, imageUri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            // Сохраняем картинку и получаем путь
            val imagePath = ImageHelper.saveImageToInternalStorage(context, imageUri)
            if (imagePath != null) {
                dao.insertCoin(Coin(cityId = cityId, name = coinName, imagePath = imagePath))
            }
        }
    }
}