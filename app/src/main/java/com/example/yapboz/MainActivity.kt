package com.example.yapboz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.ToneGenerator
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import androidx.compose.ui.graphics.Color as ComposeColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YapbozTheme {
                GameApp()
            }
        }
    }
}

enum class Language(val code: String) { TR("tr"), EN("en") }

data class Category(val key: String, val emoji: String)

data class Tile(val correctIndex: Int, val image: ImageBitmap)

data class Translation(
    val title: String,
    val easy: String,
    val hard: String,
    val animals: String,
    val vehicles: String,
    val fruits: String,
    val takePhoto: String,
    val back: String,
    val playAgain: String,
    val win: String,
    val choose: String,
    val camera: String,
    val gallery: String
)

private val translations = mapOf(
    "tr" to Translation(
        title = "Emoji Yapboz",
        easy = "Kolay (2x2)",
        hard = "Zor (3x3)",
        animals = "Hayvanlar",
        vehicles = "Araçlar",
        fruits = "Meyveler",
        takePhoto = "Fotoğraf Çek",
        back = "Menüye Dön",
        playAgain = "Tekrar Oyna",
        win = "BRAVO!",
        choose = "Kaynak Seç",
        camera = "Kamera",
        gallery = "Galeri"
    ),
    "en" to Translation(
        title = "Emoji Puzzle",
        easy = "Easy (2x2)",
        hard = "Hard (3x3)",
        animals = "Animals",
        vehicles = "Vehicles",
        fruits = "Fruits",
        takePhoto = "Take Photo",
        back = "Back to Menu",
        playAgain = "Play Again",
        win = "AWESOME JOB!",
        choose = "Choose Source",
        camera = "Camera",
        gallery = "Gallery"
    )
)

private val animalEmojis = listOf("\uD83E\uDD81", "\uD83D\uDC35", "\uD83D\uDC3C", "\uD83D\uDC36")
private val vehicleEmojis = listOf("\uD83D\uDE97", "\uD83D\uDEB5", "\uD83D\uDE92", "\uD83D\uDE93")
private val fruitEmojis = listOf("\uD83C\uDF4E", "\uD83C\uDF4C", "\uD83C\uDF53", "\uD83C\uDF51")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameApp() {
    val context = LocalContext.current
    var language by remember { mutableStateOf(Language.TR) }
    val currentTranslation = translations[language.code] ?: translations.values.first()
    var currentScreen by remember { mutableStateOf("menu") }
    var gridSize by remember { mutableStateOf(2) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showChooser by remember { mutableStateOf(false) }
    val sounds = remember { PuzzleSounds() }

    DisposableEffect(Unit) {
        onDispose { sounds.release() }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bmp = loadBitmapFromUri(context, it)
            selectedBitmap = bmp
            currentScreen = "game"
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        bmp?.let {
            selectedBitmap = it
            currentScreen = "game"
        }
    }

    val categories = listOf(
        Category("animals", "\uD83E\uDD81"),
        Category("vehicles", "\uD83D\uDE97"),
        Category("fruits", "\uD83C\uDF4E"),
        Category("photo", "\uD83D\uDCF7")
    )

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = currentTranslation.title,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    IconButton(onClick = {
                        language = if (language == Language.TR) Language.EN else Language.TR
                    }) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text(
                        text = if (language == Language.TR) "TR" else "EN",
                        modifier = Modifier.padding(end = 12.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)) {
            if (currentScreen == "menu") {
                MenuScreen(
                    translation = currentTranslation,
                    gridSize = gridSize,
                    onGridSizeChange = { gridSize = it },
                    categories = categories,
                    onCategorySelected = { category ->
                        if (category.key == "photo") {
                            showChooser = true
                        } else {
                            val emojiList = when (category.key) {
                                "animals" -> animalEmojis
                                "vehicles" -> vehicleEmojis
                                else -> fruitEmojis
                            }
                            val emoji = emojiList.random()
                            selectedBitmap = createEmojiBitmap(emoji)
                            currentScreen = "game"
                        }
                    }
                )
            } else {
                selectedBitmap?.let { bmp ->
                    GameScreen(
                        translation = currentTranslation,
                        gridSize = gridSize,
                        bitmap = bmp,
                        onBack = { currentScreen = "menu" },
                        onPlayAgain = {
                            selectedBitmap = if (selectedBitmap == null) null else selectedBitmap
                        },
                        sounds = sounds
                    )
                }
            }
        }
    }

    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = { Text(currentTranslation.choose) },
            text = {
                Column {
                    TextButton(onClick = {
                        cameraLauncher.launch(null)
                        showChooser = false
                    }) { Text(currentTranslation.camera) }
                    TextButton(onClick = {
                        galleryLauncher.launch("image/*")
                        showChooser = false
                    }) { Text(currentTranslation.gallery) }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun MenuScreen(
    translation: Translation,
    gridSize: Int,
    onGridSizeChange: (Int) -> Unit,
    categories: List<Category>,
    onCategorySelected: (Category) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DifficultyButton(
                text = translation.easy,
                selected = gridSize == 2,
                onClick = { onGridSizeChange(2) }
            )
            DifficultyButton(
                text = translation.hard,
                selected = gridSize == 3,
                onClick = { onGridSizeChange(3) }
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(categories) { _, category ->
                CategoryCard(
                    translation = translation,
                    category = category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@Composable
fun DifficultyButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val textColor = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
        )
    ) {
        Text(text = text, color = textColor)
    }
}

@Composable
fun CategoryCard(translation: Translation, category: Category, onClick: () -> Unit) {
    val label = when (category.key) {
        "animals" -> translation.animals
        "vehicles" -> translation.vehicles
        "fruits" -> translation.fruits
        else -> translation.takePhoto
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = category.emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun GameScreen(
    translation: Translation,
    gridSize: Int,
    bitmap: Bitmap,
    onBack: () -> Unit,
    onPlayAgain: () -> Unit,
    sounds: PuzzleSounds
) {
    val tiles = remember { mutableStateListOf<Tile>() }
    val selectedIndex = remember { mutableStateOf<Int?>(null) }
    var solved by remember { mutableStateOf(false) }

    LaunchedEffect(bitmap, gridSize) {
        tiles.clear()
        tiles.addAll(generateTiles(bitmap, gridSize))
        shuffleTiles(tiles)
        solved = false
        selectedIndex.value = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PuzzleBoard(
            tiles = tiles,
            gridSize = gridSize,
            selectedIndex = selectedIndex,
            onSwap = { first, second ->
                tiles.swap(first, second)
                solved = tiles.indices.all { tiles[it].correctIndex == it }
            },
            sounds = sounds,
            enabled = !solved
        )
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text(translation.back)
        }
    }

    if (solved) {
        WinOverlay(translation = translation, onPlayAgain = {
            shuffleTiles(tiles)
            solved = false
            selectedIndex.value = null
            onPlayAgain()
        }, sounds = sounds)
    }
}

@Composable
fun PuzzleBoard(
    tiles: List<Tile>,
    gridSize: Int,
    selectedIndex: MutableState<Int?>,
    onSwap: (Int, Int) -> Unit,
    sounds: PuzzleSounds,
    enabled: Boolean
) {
    val size = 400.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridSize),
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) {
            itemsIndexed(tiles) { index, tile ->
                val isSelected = selectedIndex.value == index
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(MaterialTheme.colorScheme.background)
                        .clickable(enabled = enabled) {
                            if (selectedIndex.value == null) {
                                selectedIndex.value = index
                                sounds.pickup()
                            } else {
                                val first = selectedIndex.value!!
                                if (first != index) {
                                    onSwap(first, index)
                                    sounds.drop()
                                }
                                selectedIndex.value = null
                            }
                        }
                ) {
                    Image(
                        bitmap = tile.image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun WinOverlay(translation: Translation, onPlayAgain: () -> Unit, sounds: PuzzleSounds) {
    LaunchedEffect(Unit) { sounds.win() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Confetti()
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "⭐", fontSize = 48.sp)
                Text(text = translation.win, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onPlayAgain, shape = RoundedCornerShape(12.dp)) {
                    Text(translation.playAgain)
                }
            }
        }
    }
}

@Composable
fun Confetti() {
    val colors = listOf(ComposeColor.Yellow, ComposeColor.Magenta, ComposeColor.Green, ComposeColor.Cyan, ComposeColor.White)
    val transition = rememberInfiniteTransition()
    val offsets = List(30) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000 + it * 20, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        offsets.forEachIndexed { index, anim ->
            val x = (index * 37 % size.width.toInt()).toFloat()
            val y = size.height * anim.value
            drawCircle(color = colors[index % colors.size], radius = 6f, center = androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}

fun MutableList<Tile>.swap(i: Int, j: Int) {
    val temp = this[i]
    this[i] = this[j]
    this[j] = temp
}

fun shuffleTiles(list: MutableList<Tile>) {
    do {
        list.shuffle()
    } while (list.indices.all { list[it].correctIndex == it })
}

fun generateTiles(bitmap: Bitmap, grid: Int): List<Tile> {
    val size = min(bitmap.width, bitmap.height)
    val cropped = Bitmap.createBitmap(bitmap, (bitmap.width - size) / 2, (bitmap.height - size) / 2, size, size)
    val tileSize = size / grid
    val tiles = mutableListOf<Tile>()
    var index = 0
    for (y in 0 until grid) {
        for (x in 0 until grid) {
            val tileBmp = Bitmap.createBitmap(cropped, x * tileSize, y * tileSize, tileSize, tileSize)
            tiles.add(Tile(correctIndex = index, image = tileBmp.asImageBitmap()))
            index++
        }
    }
    return tiles
}

fun createEmojiBitmap(emoji: String): Bitmap {
    val size = 600
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val pastelColors = listOf("#FFECB3", "#B3E5FC", "#C8E6C9", "#FFE0B2", "#E1BEE7")
    val bgPaint = Paint().apply { color = Color.parseColor(pastelColors.random()) }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    val framePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 20f
        isAntiAlias = true
    }
    canvas.drawRect(30f, 30f, size - 30f, size - 30f, framePaint)

    val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 320f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val bounds = Rect()
    textPaint.getTextBounds(emoji, 0, emoji.length, bounds)
    canvas.drawText(emoji, size / 2f, size / 2f + bounds.height() / 2f, textPaint)
    return bmp
}

fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        null
    }
}

class PuzzleSounds {
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    fun pickup() { tone.startTone(ToneGenerator.TONE_PROP_BEEP, 80) }
    fun drop() { tone.startTone(ToneGenerator.TONE_PROP_ACK, 80) }
    fun win() { tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200) }
    fun release() { tone.release() }
}
