package com.mt.coincollection.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageHelper {
    // Копирует картинку из галереи во внутреннее хранилище приложения
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            // Создаем уникальное имя файла
            val fileName = "coin_${UUID.randomUUID()}.jpg"
            val directory = File(context.filesDir, "coin_images")
            if (!directory.exists()) directory.mkdirs()

            val file = File(directory, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            file.absolutePath // Возвращаем полный путь к файлу
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}