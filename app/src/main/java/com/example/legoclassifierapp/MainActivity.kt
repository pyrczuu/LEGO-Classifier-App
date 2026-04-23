package com.example.legoclassifierapp

import android.app.Application
import android.content.Context
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.legoclassifierapp.ui.theme.LEGOClassifierAppTheme
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: ClassifierViewModel = viewModel()
            val interpreter = viewModel.interpreter
            val modelStatus = viewModel.modelStatus
            var classificationResult by remember { mutableStateOf<String?>(null) }
            
            LEGOClassifierAppTheme {
                val navController = rememberNavController()
                var checked by remember { mutableStateOf(false) }
                val context = LocalContext.current
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize(),
                    bottomBar = {
                        BottomNav(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp),
                            navController = navController,
                            checked = checked,
                            onCheckedChange = { checked = it }
                        )
                    }) { innerPadding ->
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
                                    modifier = Modifier
                                        .padding(innerPadding),
                                    context = context,
                                    onImageCaptured = { uri ->
                                        if (interpreter != null) {
                                            classifyImage(
                                                uri,
                                                context = context,
                                                interpreter = interpreter,
                                                onResult = { classificationResult = it }
                                            )
                                        }
                                    },
                                    onImageSelected = { uri ->
                                        if (uri != null && interpreter != null) {
                                            classifyImage(
                                                uri,
                                                context = context,
                                                interpreter = interpreter,
                                                onResult = { classificationResult = it }
                                            )
                                        }
                                    }
                                )
                                
                                // Result and Status Overlay
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Status Indicator
                                    Column(
                                        modifier = Modifier
                                            .background(
                                                color = Color.Black.copy(alpha = 0.6f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = modelStatus,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (interpreter == null && !modelStatus.contains("Failed")) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp).padding(top = 4.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                    
                                    // Classification Result
                                    classificationResult?.let { result ->
                                        Text(
                                            text = "Prediction:\n$result",
                                            modifier = Modifier
                                                .padding(top = 16.dp)
                                                .background(
                                                    color = Color.Blue.copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(16.dp),
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                    }
                                }
                            }
                        }
                        composable<InventoryScreen> {
                            InventoryList(
                                modifier = Modifier.padding(innerPadding)
                            )
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

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCapturedUriString?.let { uriString ->
                onImageCaptured(Uri.parse(uriString))
            }
        }
    }

    val selectImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonTakePhoto(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                onClick = {
                    Log.d("BUTTON", "Take photo button working")
                    val uri = createImageUri(context)
                    tempCapturedUriString = uri.toString()
                    takePictureLauncher.launch(uri)
                },
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonChoosePhoto(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                onClick = {
                    Log.d("BUTTON", "Choose photo buttton working")
                    selectImageLauncher.launch("image/*")
                }
            )
        }
    }
}

@Composable
fun ButtonTakePhoto(
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = "Take a photo"
        )
    }
}

@Composable
fun ButtonChoosePhoto(
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,

        ) {
        Text(
            text = "Choose a photo"
        )
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

                if (newValue) {
                    navController.navigate(InventoryScreen)
                } else {
                    navController.navigate(AddPhotoScreen)
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
        modifier = Modifier
            .fillMaxSize()
            .scale(2f),
        thumbContent = if (checked) {
            {
                Icon(
                    painter = painterResource(R.drawable.storage),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            {
                Icon(
                    painter = painterResource(R.drawable.photo),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        }
    )
}

@Composable
fun InventoryList(
    modifier: Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(color = Color.Blue)
                .fillMaxWidth()
                .height(96.dp)
        ) {
            Text(
                text = "Placeholder",
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
            )
        }
    }
}

class ClassifierViewModel(application: Application) : AndroidViewModel(application) {
    var interpreter by mutableStateOf<Interpreter?>(null)
        private set
        
    var modelStatus by mutableStateOf("Initializing...")
        private set

    init {
        loadModel()
    }

    private fun loadModel() {
        modelStatus = "Checking Firebase for model..."
        val conditions = CustomModelDownloadConditions.Builder().requireWifi().build()
        FirebaseModelDownloader.getInstance()
            .getModel("lego_classifier_v1", DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND, conditions)
            .addOnSuccessListener { model ->
                modelStatus = "Model found on Firebase..."
                model.file?.let { file ->
                    interpreter = Interpreter(file)
                    modelStatus = "Model loaded: Firebase"
                    Log.d("INTERPRETER", "Model loaded from Firebase")
                } ?: run {
                    modelStatus = "Firebase model downloading, trying local..."
                    loadLocalModel()
                }
            }
            .addOnFailureListener {
                modelStatus = "Firebase failure, trying local..."
                Log.e("INTERPRETER", "Failed to load model from Firebase, trying local", it)
                loadLocalModel()
            }
    }

    private fun loadLocalModel() {
        try {
            val assetManager = getApplication<Application>().assets
            val fileDescriptor = assetManager.openFd("model.tflite")
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            interpreter = Interpreter(buffer)
            modelStatus = "Model loaded: Local Asset"
            Log.d("INTERPRETER", "Model loaded from assets")
        } catch (e: Exception) {
            modelStatus = "Failed to load any model"
            Log.e("INTERPRETER", "Failed to load local model. Ensure 'model.tflite' is in assets.", e)
        }
    }
}
