package com.example.legoclassifierapp

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.example.legoclassifierapp.ui.theme.LEGOClassifierAppTheme
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel

data class ClassificationEntry(
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("count") @set:PropertyName("count") var count: Int = 0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: ClassifierViewModel = viewModel()
            val interpreter = viewModel.interpreter
            val modelStatus = viewModel.modelStatus
            var classificationDisplay by remember { mutableStateOf<String?>(null) }
            var classLabel by remember { mutableStateOf<String?>(null) }
            var currentImageUri by remember { mutableStateOf<Uri?>(null) }

            LEGOClassifierAppTheme {
                val navController = rememberNavController()
                var checked by remember { mutableStateOf(false) }
                val context = LocalContext.current
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomNav(
                            modifier = Modifier.fillMaxWidth().height(84.dp),
                            navController = navController,
                            checked = checked,
                            onCheckedChange = { checked = it }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AddPhotoScreen,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None }
                    ) {
                        composable<AddPhotoScreen> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                ColPhotoButtons(
                                    modifier = Modifier.fillMaxSize(),
                                    context = context,
                                    onImageCaptured = { uri ->
                                        currentImageUri = uri
                                        if (interpreter != null) {
                                            classifyImage(uri, context = context, interpreter = interpreter) { label, display ->
                                                classLabel = label
                                                classificationDisplay = display
                                            }
                                        }
                                    },
                                    onImageSelected = { uri ->
                                        currentImageUri = uri
                                        if (uri != null && interpreter != null) {
                                            classifyImage(uri, context = context, interpreter = interpreter) { label, display ->
                                                classLabel = label
                                                classificationDisplay = display
                                            }
                                        }
                                    }
                                )

                                // Model Status Overlay
                                Column(
                                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = modelStatus, color = Color.White, style = MaterialTheme.typography.bodySmall)
                                            if (interpreter == null && !modelStatus.contains("Failed")) {
                                                CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp).size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                            }
                                        }
                                    }
                                }

                                // Classification Popup
                                classificationDisplay?.let { result ->
                                    CustomActionDialog(
                                        classification = result,
                                        imageUri = currentImageUri,
                                        onDatabaseWrite = {
                                            classLabel?.let { viewModel.incrementClassificationCount(it) }
                                            classificationDisplay = null
                                        },
                                        onWebSearch = {
                                            val query = classLabel ?: ""
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=LEGO+$query"))
                                            context.startActivity(intent)
                                        },
                                        onDismiss = { classificationDisplay = null }
                                    )
                                }
                            }
                        }
                        composable<InventoryScreen> {
                            InventoryList(
                                modifier = Modifier,
                                classifications = viewModel.classifications,
                                firestoreStatus = viewModel.firestoreStatus
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomActionDialog(
    classification: String,
    imageUri: Uri?,
    onDatabaseWrite: () -> Unit,
    onWebSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CLASSIFICATION RESULT",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color(0xFF003399)
                )

                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.padding(vertical = 16.dp).size(200.dp).clip(RoundedCornerShape(16.dp)).border(2.dp, Color.LightGray, RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = classification,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Column(modifier = Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDatabaseWrite,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003399)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add to Inventory", fontWeight = FontWeight.Bold)
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onWebSearch,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD500)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Web Search", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Dismiss", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColPhotoButtons(
    modifier: Modifier,
    onImageCaptured: (Uri) -> Unit,
    onImageSelected: (Uri?) -> Unit,
    context: Context
) {
    var tempCapturedUriString by rememberSaveable { mutableStateOf<String?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempCapturedUriString?.let { onImageCaptured(Uri.parse(it)) }
    }
    val selectImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        onImageSelected(uri)
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clickable {
                val uri = createImageUri(context)
                tempCapturedUriString = uri.toString()
                takePictureLauncher.launch(uri)
            }
        ) {
            AsyncImage(model = "file:///android_asset/images/lego_take_photo.png", contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            Text(text = "TAKE A PHOTO", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clickable { selectImageLauncher.launch("image/*") }
        ) {
            AsyncImage(model = "file:///android_asset/images/lego_select_photo.png", contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            Text(text = "CHOOSE A PHOTO", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun BottomNav(
    modifier: Modifier = Modifier,
    navController: NavController,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.background(Color.Yellow),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwitchNavigation(
            checked = checked,
            onCheckedChange = { newValue ->
                onCheckedChange(newValue)
                val destination = if (newValue) InventoryScreen else AddPhotoScreen
                navController.navigate(destination) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
fun SwitchNavigation(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.LightGray,
            checkedTrackColor = Color.White,
            uncheckedThumbColor = Color.DarkGray,
            uncheckedTrackColor = Color.White,
        ),
        modifier = Modifier.fillMaxSize().scale(2f),
        thumbContent = if (checked) {
            { Icon(painter = painterResource(R.drawable.storage), contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
        } else {
            { Icon(painter = painterResource(R.drawable.photo), contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
        }
    )
}

@Composable
fun InventoryList(
    modifier: Modifier,
    classifications: List<ClassificationEntry>,
    firestoreStatus: String
) {
    Column(modifier = modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(color = Color(0xFF003399)).fillMaxWidth().height(130.dp)
        ) {
            AsyncImage(
                model = "file:///android_asset/images/icon.jpg",
                contentDescription = null,
                modifier = Modifier.padding(start = 16.dp).size(88.dp).align(Alignment.CenterStart).clip(CircleShape).border(4.dp, Color(0xFF0077FF), CircleShape).border(2.dp, Color.White, CircleShape),
                contentScale = ContentScale.Crop
            )
            Text(text = "MY LEGO BRICKS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.align(Alignment.Center).padding(start = 64.dp))
        }

        if (classifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = firestoreStatus, color = Color.Gray, textAlign = TextAlign.Center)
                    if (firestoreStatus == "Fetching data...") {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp), color = Color(0xFF003399))
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(classifications) { entry -> ClassificationItem(entry) }
            }
        }
    }
}

@Composable
fun ClassificationItem(entry: ClassificationEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = entry.name.ifEmpty { "Unknown Brick" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Box(modifier = Modifier.background(Color(0xFF003399), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(text = "x${entry.count}", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

class ClassifierViewModel(application: Application) : AndroidViewModel(application) {
    var interpreter by mutableStateOf<Interpreter?>(null)
        private set
    var modelStatus by mutableStateOf("Initializing...")
        private set
    var firestoreStatus by mutableStateOf("Initializing...")
        private set
    var classifications by mutableStateOf<List<ClassificationEntry>>(emptyList())
        private set

    init {
        loadModel()
        fetchClassifications()
    }

    fun incrementClassificationCount(label: String) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document("5sSPz1C3gbjaE2JpFSWn").collection("classifications").document(label)
        val data = hashMapOf("name" to label, "count" to FieldValue.increment(1))
        docRef.set(data, SetOptions.merge())
    }

    private fun fetchClassifications() {
        firestoreStatus = "Fetching data..."
        FirebaseFirestore.getInstance().collection("users").document("5sSPz1C3gbjaE2JpFSWn").collection("classifications")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    firestoreStatus = "Error: ${error.message}"
                    return@addSnapshotListener
                }
                if (value != null) {
                    if (value.isEmpty) {
                        firestoreStatus = "Inventory is empty"
                        classifications = emptyList()
                    } else {
                        classifications = value.mapNotNull { it.toObject(ClassificationEntry::class.java) }
                        firestoreStatus = "Loaded ${classifications.size} items"
                    }
                }
            }
    }

    private fun loadModel() {
        modelStatus = "Checking Firebase..."
        val conditions = CustomModelDownloadConditions.Builder().requireWifi().build()
        FirebaseModelDownloader.getInstance().getModel("lego_classifier_v2", DownloadType.LOCAL_MODEL, conditions)
            .addOnSuccessListener { model ->
                model.file?.let { file ->
                    interpreter = Interpreter(file)
                    modelStatus = "Model loaded: Firebase"
                } ?: run { loadLocalModel() }
            }
            .addOnFailureListener { loadLocalModel() }
    }

    private fun loadLocalModel() {
        try {
            val assetManager = getApplication<Application>().assets
            val fileDescriptor = assetManager.openFd("models/model.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val buffer = inputStream.channel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
            interpreter = Interpreter(buffer)
            modelStatus = "Model loaded: Local Asset"
        } catch (e: Exception) {
            modelStatus = "Failed to load model"
        }
    }
}
