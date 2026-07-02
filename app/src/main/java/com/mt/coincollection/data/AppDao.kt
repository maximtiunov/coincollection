package com.mt.coincollection.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Города
    @Insert
    suspend fun insertCity(city: City)
    @Query("SELECT * FROM cities ORDER BY name ASC")
    fun getAllCities(): Flow<List<City>>

    // Монеты
    @Insert
    suspend fun insertCoin(coin: Coin)
    @Query("SELECT * FROM coins WHERE cityId = :cityId")
    fun getCoinsByCity(cityId: Int): Flow<List<Coin>>
}