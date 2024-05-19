package com.dicoding.asclepius.view

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.dicoding.asclepius.database.History
import com.dicoding.asclepius.database.HistoryDatabase
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File
import java.io.FileOutputStream

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: Menampilkan hasil gambar, prediksi, dan confidence score.
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            showImage(imageUri)

            val imageClassifierHelper = ImageClassifierHelper(
                context = this,
                classifierListener = object : ImageClassifierHelper.ClassifierListener {
                    override fun onError(errorMessage: String) {
                        Log.d(TAG, "Error: $errorMessage")
                    }

                    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                        results?.let { showResults(it) }
                    }
                }
            )
            imageClassifierHelper.classifyImage(imageUri)
        } else {
            Log.e(TAG, "No image URI provided")
            finish()
        }
        binding.saveButton.setOnClickListener {
            val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
            val result = binding.resultText.text.toString()

            if (imageUriString != null) {
                val imageUri = Uri.parse(imageUriString)
                showToast("Data saved")
                saveHistoryToDatabase(imageUri, result)
            } else {
                showToast("No image URI provided")
                finish()
            }
        }

    }
    private fun showImage(uri: Uri) {
        Log.d(TAG, "Displaying image: $uri")
        binding.resultImage.setImageURI(uri)
    }
    private fun showResults(results: List<Classifications>) {
        val topResult = results[0]
        val label = topResult.categories[0].label
        val score = topResult.categories[0].score

        fun Float.formatToString(): String {
            return String.format("%.2f%%", this * 100)
        }
        binding.resultText.text = "$label ${score.formatToString()}"
    }
    private fun saveHistoryToDatabase(imageUri: Uri, result: String) {
        if (result.isNotEmpty()) {
            val fileName = "cropped_image_${System.currentTimeMillis()}.jpg"
            val destinationUri = Uri.fromFile(File(cacheDir, fileName))
            contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(File(cacheDir, fileName)).use { output ->
                    input.copyTo(output)
                }
            }
            val history = History(imagePath = destinationUri.toString(), result = result)
            GlobalScope.launch(Dispatchers.IO) {
                val database = HistoryDatabase.getDatabase(applicationContext)
                try {
                    database.historyDao().insertHistory(history)
                    Log.d(TAG, "History saved successfully: $history")
                    val history = database.historyDao().getAllHistory()
                    Log.d(TAG, "All history after save: $history")
                    moveToHistory(destinationUri, result)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save : $history", e)
                }
            }
        } else {
            Log.e(TAG, "Result is empty.")
        }
    }
    private fun moveToHistory(imageUri: Uri, result: String) {
        val intent = Intent(this, HistoryAvticity::class.java)
        intent.putExtra(EXTRA_RESULT, result)
        intent.putExtra(EXTRA_IMAGE_URI, imageUri.toString())
        setResult(RESULT_OK, intent)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT = "extra_result"
        const val TAG = "imageSelect"
        const val REQUEST_HISTORY_UPDATE = 1    }
}