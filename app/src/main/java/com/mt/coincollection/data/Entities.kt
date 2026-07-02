package com.mt.coincollection.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cities")
data class City(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = ""
)

@Entity(tableName = "coins")
data class Coin(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cityId: Int, // Связь с городом
    val name: String, // Например: "10 рублей"
    val imagePath: String // Путь к файлу картинки внутри приложения
)