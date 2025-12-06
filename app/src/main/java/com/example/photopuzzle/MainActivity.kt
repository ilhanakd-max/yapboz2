package com.example.photopuzzle

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.media.AudioManager
import android.media.ImageDecoder
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.photopuzzle.databinding.ActivityMainBinding
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

    private var currentBitmap: Bitmap? = null
    private val pieces = mutableListOf<PuzzlePiece>()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            loadBitmapFromUri(uri)?.let { bitmap ->
                currentBitmap = bitmap
                preparePuzzle(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        ensureLanguage()
        updateTexts()

        binding.selectImageButton.setOnClickListener {
            requestImage()
        }

        binding.playAgainButton.setOnClickListener {
            currentBitmap?.let { preparePuzzle(it) }
        }
    }

    private fun ensureLanguage() {
        val prefs = getPreferences(Context.MODE_PRIVATE)
        val selected = prefs.getString(KEY_LANGUAGE, null)
        if (selected == null) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_language))
                .setCancelable(false)
                .setItems(arrayOf(getString(R.string.language_english), getString(R.string.language_turkish))) { _, which ->
                    val locale = if (which == 0) Locale.ENGLISH else Locale("tr")
                    saveLanguage(locale)
                    updateTexts()
                }
                .show()
        } else {
            val locale = if (selected == "en") Locale.ENGLISH else Locale("tr")
            applyLocale(locale)
        }
    }

    private fun saveLanguage(locale: Locale) {
        getPreferences(Context.MODE_PRIVATE).edit()
            .putString(KEY_LANGUAGE, if (locale.language == "tr") "tr" else "en")
            .apply()
        applyLocale(locale)
    }

    private fun applyLocale(locale: Locale) {
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun updateTexts() {
        binding.selectImageButton.text = getString(R.string.select_image)
        binding.playAgainButton.text = getString(R.string.play_again)
        binding.statusText.text = ""
    }

    private fun requestImage() {
        if (!hasStoragePermission()) {
            requestPermissions()
            return
        }
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissionLauncher.launch(permissions)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            requestImage()
        } else {
            Toast.makeText(this, getString(R.string.permission_needed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun preparePuzzle(bitmap: Bitmap) {
        binding.confettiView.stop()
        binding.confettiView.visibility = View.GONE
        binding.playAgainButton.isVisible = false
        binding.statusText.text = ""

        val resized = Bitmap.createScaledBitmap(bitmap, bitmap.width - bitmap.width % 3, bitmap.height - bitmap.height % 3, true)
        pieces.clear()
        val tileWidth = resized.width / 3
        val tileHeight = resized.height / 3
        var index = 0
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                val pieceBitmap = Bitmap.createBitmap(resized, col * tileWidth, row * tileHeight, tileWidth, tileHeight)
                pieces.add(PuzzlePiece(pieceBitmap, index))
                index++
            }
        }
        pieces.shuffle()
        renderGrid()
    }

    private fun renderGrid() {
        binding.puzzleGrid.removeAllViews()
        binding.puzzleGrid.rowCount = 3
        binding.puzzleGrid.columnCount = 3

        pieces.forEachIndexed { position, piece ->
            piece.currentIndex = position
            val imageView = ImageView(this).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(piece.bitmap)
                tag = piece
                setOnTouchListener { v, _ ->
                    startDragForPiece(v as ImageView)
                    true
                }
                setOnDragListener(dragListener)
            }
            binding.puzzleGrid.addView(imageView)
        }
    }

    private fun startDragForPiece(view: ImageView) {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP)
        val piece = view.tag as PuzzlePiece
        view.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        val shadow = View.DragShadowBuilder(view)
        val data = ClipData.newPlainText("piece", piece.currentIndex.toString())
        view.startDragAndDrop(data, shadow, view, 0)
    }

    private val dragListener = View.OnDragListener { target, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true
            DragEvent.ACTION_DRAG_ENTERED -> {
                target.alpha = 0.7f
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                target.alpha = 1f
                true
            }
            DragEvent.ACTION_DROP -> {
                target.alpha = 1f
                val draggedView = event.localState as ImageView
                val fromIndex = (draggedView.tag as PuzzlePiece).currentIndex
                val toIndex = (target.tag as PuzzlePiece).currentIndex
                swapPieces(fromIndex, toIndex)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK)
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                (target as? ImageView)?.colorFilter = null
                true
            }
            else -> false
        }
    }

    private fun swapPieces(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        pieces[fromIndex].currentIndex = toIndex
        pieces[toIndex].currentIndex = fromIndex
        pieces.swap(fromIndex, toIndex)
        renderGrid()
        checkVictory()
    }

    private fun checkVictory() {
        val solved = pieces.withIndex().all { (index, piece) -> piece.correctIndex == index }
        if (solved) {
            binding.statusText.text = getString(R.string.victory_message)
            binding.playAgainButton.isVisible = true
            binding.confettiView.start()
            playVictoryMelody()
        }
    }

    private fun playVictoryMelody() {
        val tones = listOf(
            ToneGenerator.TONE_PROP_ACK,
            ToneGenerator.TONE_PROP_BEEP,
            ToneGenerator.TONE_PROP_BEEP2,
            ToneGenerator.TONE_PROP_NACK,
            ToneGenerator.TONE_PROP_ACK
        )
        Thread {
            tones.forEach { tone ->
                toneGenerator.startTone(tone, 150)
                Thread.sleep(180)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator.release()
    }

    data class PuzzlePiece(val bitmap: Bitmap, val correctIndex: Int, var currentIndex: Int = correctIndex)

    companion object {
        private const val KEY_LANGUAGE = "key_language"
    }
}

private fun MutableList<MainActivity.PuzzlePiece>.swap(i: Int, j: Int) {
    val tmp = this[i]
    this[i] = this[j]
    this[j] = tmp
}
