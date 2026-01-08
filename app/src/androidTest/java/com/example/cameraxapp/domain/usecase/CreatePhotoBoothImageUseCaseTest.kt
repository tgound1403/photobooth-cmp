package com.example.cameraxapp.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class CreatePhotoBoothImageUseCaseTest {

    private val useCase = CreatePhotoBoothImageUseCase()
    private val context = ApplicationProvider.getApplicationContext<Context>()


    @Test
    fun execute_createsImage_GRID_2X2_when4PathsProvided() = runBlocking {
        // Arrange
        val imagePaths = mutableListOf<String>()
        val cacheDir = context.cacheDir
        
        // Create 4 dummy images
        for (i in 1..4) {
            val file = File(cacheDir, "test_image_$i.jpg")
            val outputStream = FileOutputStream(file)
             android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.rgb(i*50, 0, 0))
                compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            outputStream.close()
            imagePaths.add(file.absolutePath)
        }

        // Act
        val result = useCase.execute(context, imagePaths, com.example.cameraxapp.shared.domain.model.PhotoBoothLayout.GRID_2X2)

        // Assert
        assertTrue("Result should be success", result.isSuccess)
        val resultUri = result.getOrNull()
        assertTrue("Result path should not be null", resultUri != null)
        
        // Clean up
        imagePaths.forEach { File(it).delete() }
    }

    @Test
    fun execute_createsImage_STRIP_1X4_when4PathsProvided() = runBlocking {
         // Arrange
        val imagePaths = mutableListOf<String>()
        val cacheDir = context.cacheDir
        
        // Create 4 dummy images
        for (i in 1..4) {
            val file = File(cacheDir, "test_image_$i.jpg")
            val outputStream = FileOutputStream(file)
             android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.rgb(i*50, 0, 0))
                compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
            outputStream.close()
            imagePaths.add(file.absolutePath)
        }

        // Act
        val result = useCase.execute(
            context, 
            imagePaths, 
            com.example.cameraxapp.shared.domain.model.PhotoBoothLayout.STRIP_1X4
        )

        // Assert
        assertTrue("Result should be success", result.isSuccess)
        // We could decode the result URI and check dimensions if we wanted to be more thorough
    }

    @Test
    fun execute_fails_whenNot4Paths() = runBlocking {
         // Arrange
        val imagePaths = listOf("path/to/img1", "path/to/img2") // Only 2

        // Act
        val result = useCase.execute(context, imagePaths)

        // Assert
        assertTrue("Result should be failure", result.isFailure)
    }
}
