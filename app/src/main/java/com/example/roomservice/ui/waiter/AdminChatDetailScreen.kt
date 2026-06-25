package com.example.roomservice.ui.waiter

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.roomservice.data.ChatRepository
import com.example.roomservice.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import androidx.core.content.FileProvider
import android.media.MediaRecorder
import android.media.MediaPlayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.delay
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatDetailScreen(
    roomNumber: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val messages by ChatRepository.messages.collectAsState()
    val filteredMessages = remember(messages, roomNumber) {
        messages.filter { it.roomNumber == roomNumber }.sortedBy { it.timestamp }
    }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showAttachmentMenu by remember { mutableStateOf(false) }

    // Voice Recording State
    var isRecording by remember { mutableStateOf(false) }
    var recordStartTime by remember { mutableLongStateOf(0L) }
    var recordingTimer by remember { mutableStateOf("0:00") }
    val mediaRecorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val audioFile = remember { mutableStateOf<File?>(null) }

    // Image Launchers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { ChatRepository.sendMessage(roomNumber, "", "admin", it.toString()) }
    }

    val cameraImageUri = remember {
        val file = File(context.cacheDir, "chat_temp_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            ChatRepository.sendMessage(roomNumber, "", "admin", cameraImageUri.toString())
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(context, "Mic permission required", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Timer Logic for Recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordStartTime = System.currentTimeMillis()
            while (isRecording) {
                val elapsed = (System.currentTimeMillis() - recordStartTime) / 1000
                val mins = elapsed / 60
                val secs = elapsed % 60
                recordingTimer = String.format("%d:%02d", mins, secs)
                delay(1000)
            }
        }
    }

    fun startRecording() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.mp3")
            audioFile.value = file
            mediaRecorder.value = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording(send: Boolean) {
        try {
            mediaRecorder.value?.apply {
                stop()
                release()
            }
            mediaRecorder.value = null
            isRecording = false
            
            if (send && audioFile.value != null) {
                ChatRepository.sendMessage(roomNumber, "", "admin", null, Uri.fromFile(audioFile.value).toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
        }
    }

    // Auto-scroll to bottom
    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            listState.animateScrollToItem(filteredMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color.LightGray
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(roomNumber.take(1), fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Room $roomNumber", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Online", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE5DDD5)) // WhatsApp background color
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredMessages) { message ->
                        WhatsAppChatBubble(message = message)
                    }
                }

                // WhatsApp Style Input Bar
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isRecording) {
                            // Text Input Container
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(25.dp),
                                color = Color.White
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    IconButton(onClick = {}) {
                                        Icon(Icons.Default.TagFaces, null, tint = Color.Gray)
                                    }
                                    TextField(
                                        value = textInput,
                                        onValueChange = { textInput = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Type a message") },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        )
                                    )
                                    IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                                        Icon(Icons.Default.Add, null, tint = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            // Recording Indicator Container
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(25.dp),
                                color = Color.White
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Mic, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(recordingTimer, color = Color.Black, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.weight(1f))
                                    Text("Slide to cancel", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Send / Mic Button
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) Color.Red else Color(0xFF075E54))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            if (textInput.isBlank()) {
                                                startRecording()
                                                try {
                                                    awaitRelease()
                                                } finally {
                                                    stopRecording(true)
                                                }
                                            }
                                        },
                                        onTap = {
                                            if (textInput.isNotBlank()) {
                                                ChatRepository.sendMessage(roomNumber, textInput, "admin")
                                                textInput = ""
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (textInput.isBlank()) Icons.Default.Mic else Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // ATTACHMENT MENU
            if (showAttachmentMenu) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 70.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AttachmentOption(
                            icon = Icons.Default.Photo,
                            label = "Gallery",
                            color = Color(0xFF9C27B0),
                            onClick = { galleryLauncher.launch("image/*"); showAttachmentMenu = false }
                        )
                        AttachmentOption(
                            icon = Icons.Default.CameraAlt,
                            label = "Camera",
                            color = Color(0xFFE91E63),
                            onClick = { cameraLauncher.launch(cameraImageUri); showAttachmentMenu = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(50.dp)) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun WhatsAppChatBubble(message: ChatMessage) {
    val context = LocalContext.current
    val isAdmin = message.senderId == "admin"
    val alignment = if (isAdmin) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isAdmin) Color(0xFFDCF8C6) else Color.White
    
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer() }

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isAdmin) 12.dp else 0.dp,
                bottomEnd = if (isAdmin) 0.dp else 12.dp
            ),
            shadowElevation = 0.5.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(max = 280.dp)) {
                if (!message.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (message.text.isNotEmpty()) Spacer(Modifier.height(4.dp))
                }

                if (!message.voiceUrl.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                        IconButton(onClick = {
                            if (isPlaying) {
                                mediaPlayer.stop()
                                isPlaying = false
                            } else {
                                try {
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(context, Uri.parse(message.voiceUrl))
                                    mediaPlayer.prepare()
                                    mediaPlayer.start()
                                    isPlaying = true
                                    mediaPlayer.setOnCompletionListener { isPlaying = false }
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color(0xFF1976D2))
                        }
                        Spacer(Modifier.width(8.dp))
                        // Voice Waveform Placeholder
                        LinearProgressIndicator(
                            progress = if (isPlaying) 0.5f else 0f, 
                            modifier = Modifier.weight(1f).height(4.dp),
                            color = Color(0xFF1976D2),
                            trackColor = Color.LightGray
                        )
                    }
                }
                
                if (message.text.isNotEmpty()) {
                    Text(
                        text = message.text,
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                }
                Text(
                    text = time,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                )
            }
        }
    }
}
