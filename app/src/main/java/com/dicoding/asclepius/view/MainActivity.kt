package com.dicoding.asclepius.view


import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest

import androidx.activity.result.contract.ActivityResultContracts
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.yalantis.ucrop.UCrop
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null
    private var croppedImageUri: Uri? = null

    companion object {
        const val TAG = "ImageSelect"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setOnMenuItemClickListener{ menuItem->
            when(menuItem.itemId){
                R.id.menu1->{
                    startActivity(Intent(this, HistoryAvticity::class.java))
                    true
                }
                else -> false
            }

        }
        binding.galleryButton.setOnClickListener { startGallery() }
        binding.analyzeButton.setOnClickListener {
            currentImageUri?.let {
                analyzeImage()
            } ?: run {
                showToast(getString(R.string.empty_image_warning))
            }
        }
    }

    private fun startGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        val select = Intent.createChooser(intent, "Select a Picture")
        launcherGallery.launch(select)
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImg = result.data?.data
            selectedImg?.let { uri ->
                currentImageUri = uri
                showImage()
                startUCrop(uri)
            } ?: showToast("Failed to get image URI")
        }
    }

    private fun startUCrop(sourceUri: Uri) {
        val fileImage = "cropped_image_${System.currentTimeMillis()}.jpg"
        val destination = Uri.fromFile(File(cacheDir, fileImage))
        UCrop.of(sourceUri, destination)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000)
            .start(this)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let {
                showCroppedImage(resultUri)
            } ?: showToast("Failed to crop image")
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            showToast("Crop error: ${cropError?.message}")
        }
    }
    private fun showCroppedImage(uri: Uri) {
        binding.previewImageView.setImageURI(uri)
        croppedImageUri = uri
    }
    private fun analyzeImage() {
        Log.d(TAG, "Moving to ResultActivity")
        val intent = Intent(this, ResultActivity::class.java)
        croppedImageUri?.let { uri ->
            intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, uri.toString())
            startActivity(intent)
        } ?: showToast(getString(R.string.image_classifier_failed))
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}