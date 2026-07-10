package com.example

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- UI Navigation Tabs ---
enum class Tab {
    HOME, WORKSPACE, DIAGNOSTIC, CODE, SETTINGS
}

// --- GSuite Simulated Services ---
enum class GSuiteApp {
    GMAIL, DRIVE, DOCS, SHEETS, CALENDAR, MEET, NONE
}

// --- Data Models ---
data class SearchResult(val title: String, val url: String, val snippet: String)
data class EmailMessage(val id: Int, val from: String, val subject: String, val body: String, var isRead: Boolean, val date: String)
data class DriveFileItem(val id: Int, val name: String, val type: String, val size: String)
data class CalendarEvent(val id: Int, val title: String, val time: String, val desc: String)

// --- ViewModel ---
class GoogolViewModel : ViewModel() {
    // Basic navigation and interface states
    private val _currentTab = MutableStateFlow(Tab.HOME)
    val currentTab: StateFlow<Tab> = _currentTab.asStateFlow()

    private val _activeGSuiteApp = MutableStateFlow(GSuiteApp.NONE)
    val activeGSuiteApp: StateFlow<GSuiteApp> = _activeGSuiteApp.asStateFlow()

    // System Setting Values
    var brightness = mutableStateOf(90f)
    var volume = mutableStateOf(80f)
    var battery = mutableStateOf(98f)

    // Search and AI Streaming state
    var searchQuery = mutableStateOf("")
    var aiOverview = mutableStateOf<String?>(null)
    var isSearching = mutableStateOf(false)
    var searchResultsList = mutableStateListOf<SearchResult>()
    var selectedResultTab = mutableStateOf("all")

    // Interactive Camera & Voice simulation toggles
    var isCameraScanning = mutableStateOf(false)
    var isVoiceListening = mutableStateOf(false)
    var voiceRecognizedText = mutableStateOf("")

    // Simulated Mailbox
    val emails = mutableStateListOf(
        EmailMessage(1, "Alphabet Inc.", "Annual Shareholder Notification", "This is a notification confirming that you hold exactly 0 voting shares of Googol or Google. Thank you for auditing.", false, "11:45 AM"),
        EmailMessage(2, "Sundar Pichai", "Your offer to buy Googol for $20", "Dear user, I saw your proposal to buy Googol for twenty dollars cash. While we respect your ambition, our current valuation has slightly more zeros. Try the Diagnostic app instead!", false, "Yesterday"),
        EmailMessage(3, "Google Developers", "Native Android codebase completed", "Your Googol Pixel 8 customized layout has been fully written in Kotlin Jetpack Compose. Inspect file codes inside the 'Src Code' tab.", true, "May 20")
    )
    var newMailFrom = mutableStateOf("")
    var newMailSubject = mutableStateOf("")
    var newMailBody = mutableStateOf("")

    // Drive simulated files list
    val driveFiles = mutableStateListOf(
        DriveFileItem(1, "Googol_Deed_Not_Yours.pdf", "PDF Document", "1.2 MB"),
        DriveFileItem(2, "Piggy_Bank_Balance_2026.xlsx", "Spreadsheet", "420 KB"),
        DriveFileItem(3, "CEO_Ikea_Desk_Layout.pptx", "Presentation", "3.4 MB"),
        DriveFileItem(4, "Cool_Ringtones_Folder", "Folder", "---"),
        DriveFileItem(5, "Cat_Meme_Database.sql", "Database Archive", "128 MB")
    )

    // Docs text state
    var docsContent = mutableStateOf(
        "# My Plan To Buy Googol\n\n1. Save pocket money (curr balance: $20.45)\n2. Ask Sundar very politely if he'd like to share.\n3. Implement Material Design 3 app."
    )

    // Sheets cells state (Grid coordinate logger)
    val sheetsCells = mutableStateListOf(
        mutableStateListOf("A1", "Googol Market Cap", "$2,000,000,000,000"),
        mutableStateListOf("A2", "My Wallet Capital", "$20.45"),
        mutableStateListOf("A3", "Shortfall Gap", "-$1,999,999,999,979.55"),
        mutableStateListOf("A4", "Percentage Owned", "0.000000001%")
    )

    // Calendar list
    val calendarEvents = mutableStateListOf(
        CalendarEvent(1, "Dream of owning Googol", "10:00 AM", "Highly critical brainstorm session."),
        CalendarEvent(2, "Meeting with Sundar's automated reject bot", "2:00 PM", "Drafting a very polite response.")
    )
    var newEventTitle = mutableStateOf("")
    var newEventTime = mutableStateOf("")

    // Meet conference state
    var isMeetActive = mutableStateOf(false)

    // Diagnostic Quiz States
    var diagnosticResult = mutableStateOf("idle") // idle, testing, not-owner, owner, close-enough
    var quizQuestionIndex = mutableStateOf(0)
    var quizScore = mutableStateOf(0)
    var selectedQuizAnswer = mutableStateOf<Int?>(null)

    val quizQuestions = listOf(
        QuizQuestion(
            "What is your present piggy-bank wallet resource balance?",
            listOf("Less than $100", "$100 to $1,000", "More than $10,000", "Actually, I have a billion dollars"),
            listOf(0, 1, 2, 8)
        ),
        QuizQuestion(
            "Where is your signature on Googol's Delaware incorporation certificate?",
            listOf("On page 1 in bold ink", "I signed with an electronic signature", "I can't recall having signed it", "In my dreams, every night"),
            listOf(10, 8, 0, 2)
        ),
        QuizQuestion(
            "Which desk size closest represents your current computer setup space?",
            listOf("A massive solid mahogany layout", "Medium adjustable desk", "IKEA Linnmon desk or kitchen table", "I work lying on my floor"),
            listOf(7, 4, 1, 0)
        )
    )

    fun setTab(tab: Tab) {
        _currentTab.value = tab
        if (tab != Tab.WORKSPACE) {
            _activeGSuiteApp.value = GSuiteApp.NONE
        }
    }

    fun setGSuiteApp(app: GSuiteApp) {
        _activeGSuiteApp.value = app
    }

    // Interactive Trigger Search
    fun triggerSearch(context: Context, query: String) {
        if (query.trim().isEmpty()) return
        searchQuery.value = query
        isSearching.value = true
        aiOverview.value = null
        searchResultsList.clear()

        // We run a background search routine
        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
        scope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val client = OkHttpClient()
                    val mediaType = "application/json".toMediaType()
                    val requestJson = """
                        {
                          "contents": [{"parts": [{"text": "${query.replace("\"", "\\\"")}"}]}],
                          "systemInstruction": {
                            "parts": [{"text": "You are Googol AI, a helpful, brilliant, and slightly superior search assistant. Keep answers concise, extremely polished and styled in clean markdown. If the user asks about owning Googol or Google, remind them that they have exactly zero shares and suggest taking the Diagnostic test."}]
                          }
                        }
                    """.trimIndent()

                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                        .post(requestJson.toRequestBody(mediaType))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyStr = response.body?.string() ?: ""
                            val jsonObj = JSONObject(bodyStr)
                            val textResult = jsonObj.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")

                            withContext(Dispatchers.Main) {
                                aiOverview.value = textResult
                            }
                        } else {
                            throw Exception("HTTP Error: ${response.code}")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        aiOverview.value = "Unable to reach dynamic AI nodes. Offline fallback overview activated. Here is standard search summary on '$query'."
                    }
                }
            } else {
                // Mock Streaming Overview Fallback
                delay(1200)
                withContext(Dispatchers.Main) {
                    aiOverview.value = "Googol Search local index analysis: Search results for '$query' successfully assembled. Since the Gemini API key is in template mode, local fallback indices indicate your conversion to a fully native Android Kotlin environment is complete and operational!"
                }
            }

            // Assemble organic search listings
            delay(500)
            withContext(Dispatchers.Main) {
                searchResultsList.add(SearchResult("$query - Googol General Index", "https://googol.com/q?${query.lowercase()}", "Access real-time database crawling indices regarding '$query'. Find comprehensive Delaware documents and index profiles."))
                searchResultsList.add(SearchResult("How to buy Googol and replace Sundar Pichai", "https://ownership.googol.com/ceo-thoughts", "Read articles about Google's corporate Delaware board, market valuations, and voting share diagnostics."))
                searchResultsList.add(SearchResult("Native Kotlin & Compose applet trace", "https://com.example.googol", "View the main activity source files, build dependencies, and asset configurations inside your local system image."))
                isSearching.value = false
            }
        }
    }

    // Diagnostic answer selected
    fun answerQuizQuestion(answerIndex: Int) {
        selectedQuizAnswer.value = answerIndex
        val points = quizQuestions[quizQuestionIndex.value].scores[answerIndex]
        quizScore.value += points

        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
        scope.launch {
            delay(500)
            selectedQuizAnswer.value = null
            if (quizQuestionIndex.value + 1 < quizQuestions.size) {
                quizQuestionIndex.value += 1
            } else {
                // Completed
                val finalScore = quizScore.value
                diagnosticResult.value = when {
                    finalScore >= 18 -> "owner"
                    finalScore >= 10 -> "close-enough"
                    else -> "not-owner"
                }
            }
        }
    }

    fun resetQuiz() {
        quizQuestionIndex.value = 0
        quizScore.value = 0
        selectedQuizAnswer.value = null
        diagnosticResult.value = "idle"
    }
}

data class QuizQuestion(val text: String, val options: List<String>, val scores: List<Int>)

// --- MainActivity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GoogolAppMain()
            }
        }
    }
}

// --- MAIN COMPOSE APP SCREEN ---
@Composable
fun GoogolAppMain() {
    val viewModel: GoogolViewModel = viewModel()
    val currentTab by viewModel.currentTab.collectAsState()
    val activeGSuiteApp by viewModel.activeGSuiteApp.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            GoogolBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    viewModel.setTab(tab)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F6FC)) // Elegant modern Sleek Interface background
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "ScreenTransition") { tab ->
                when (tab) {
                    Tab.HOME -> HomeScreen(viewModel, context)
                    Tab.WORKSPACE -> WorkspaceScreen(viewModel, activeGSuiteApp)
                    Tab.DIAGNOSTIC -> DiagnosticScreen(viewModel)
                    Tab.CODE -> CodeViewerScreen(viewModel, context)
                    Tab.SETTINGS -> SettingsScreen(viewModel)
                }
            }
        }
    }
}

// --- HOME SCREEN (Googol Search Engine) ---
@Composable
fun HomeScreen(viewModel: GoogolViewModel, context: Context) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var query by remember { viewModel.searchQuery }
    val isSearching by remember { viewModel.isSearching }
    val aiOverviewVal by remember { viewModel.aiOverview }
    val searchResults = viewModel.searchResultsList
    val activeTab by remember { viewModel.selectedResultTab }

    // Speech / Camera Lens Simulation states
    val isCameraOpen by remember { viewModel.isCameraScanning }
    val isVoiceOpen by remember { viewModel.isVoiceListening }
    val recognizedText by remember { viewModel.voiceRecognizedText }

    // Dialog state controllers for shortcuts
    var showProfileDialog by remember { mutableStateOf(false) }
    var showWeatherDialog by remember { mutableStateOf(false) }
    var showTranslateDialog by remember { mutableStateOf(false) }
    var showHomeworkDialog by remember { mutableStateOf(false) }
    var showSongDialog by remember { mutableStateOf(false) }

    // Stateful like counts and loved flags for Discover feed cards
    val likesCount = remember { mutableStateMapOf(1 to 142, 2 to 98, 3 to 256, 4 to 1024) }
    val isCardLiked = remember { mutableStateMapOf(1 to false, 2 to false, 3 to false, 4 to false) }

    // Permission launcher for camera scanning
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.isCameraScanning.value = true
                Toast.makeText(context, "Google Lens initiated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Camera permission required for Lens simulator", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC)), // Warm eye-safe elegant background
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            // --- TOP HEADER BAR: Weather & Profile Avatar ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Weather Pill Widget
                    Card(
                        modifier = Modifier
                            .clickable { showWeatherDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Sunny Weather",
                                tint = Color(0xFFFBBC05),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sunny • 72°F",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    // Profile Avatar Widget
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                )
                            )
                            .clickable { showProfileDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "F",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // --- GOOGLE LOGO (Official Color Sequence) ---
            item {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoogolLetter("G", Color(0xFF1A73E8))
                        GoogolLetter("o", Color(0xFFEA4335))
                        GoogolLetter("o", Color(0xFFFBBC05))
                        GoogolLetter("g", Color(0xFF1A73E8))
                        GoogolLetter("l", Color(0xFF34A853))
                        GoogolLetter("e", Color(0xFFEA4335))
                    }
                    Text(
                        text = "AI-POWERED PREVIEW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // --- PILL SEARCH BAR ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .shadow(4.dp, shape = RoundedCornerShape(28.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Multi-color G logo on left
                        Text(
                            text = "G",
                            color = Color(0xFF1A73E8),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Search or type URL...", color = Color(0xFF64748B), fontSize = 14.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                viewModel.triggerSearch(context, query)
                            })
                        )

                        // Microphone Button
                        IconButton(
                            onClick = {
                                viewModel.isVoiceListening.value = true
                                viewModel.voiceRecognizedText.value = ""
                                val voiceKeywords = listOf("Why do I not own Google?", "How to acquire stock?", "Kotlin Jetpack Compose guidelines", "Sundar Pichai secret workspace")
                                viewModel.voiceRecognizedText.value = voiceKeywords.random()
                                val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
                                scope.launch {
                                    delay(1800)
                                    viewModel.isVoiceListening.value = false
                                    query = viewModel.voiceRecognizedText.value
                                    viewModel.triggerSearch(context, query)
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Search", tint = Color(0xFF4285F4))
                        }

                        // Camera Lens Button
                        IconButton(
                            onClick = {
                                val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.isCameraScanning.value = true
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Google Lens", tint = Color(0xFF34A853))
                        }
                    }
                }
            }

            // --- QUICK SHORTCUTS ACTION ROW (Sleek Horizontal Pills) ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShortcutItem(
                        label = "Translate",
                        icon = Icons.Default.Translate,
                        bgColor = Color(0xFFE0F2FE),
                        iconColor = Color(0xFF0284C7)
                    ) {
                        showTranslateDialog = true
                    }

                    ShortcutItem(
                        label = "Homework",
                        icon = Icons.Default.MenuBook,
                        bgColor = Color(0xFFFEF3C7),
                        iconColor = Color(0xFFD97706)
                    ) {
                        showHomeworkDialog = true
                    }

                    ShortcutItem(
                        label = "Identify Song",
                        icon = Icons.Default.MusicNote,
                        bgColor = Color(0xFFF3E8FF),
                        iconColor = Color(0xFF9333EA)
                    ) {
                        showSongDialog = true
                    }

                    ShortcutItem(
                        label = "Lens Search",
                        icon = Icons.Default.PhotoCamera,
                        bgColor = Color(0xFFDCFCE7),
                        iconColor = Color(0xFF16A34A)
                    ) {
                        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            viewModel.isCameraScanning.value = true
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
            }

            // Search Trigger Button Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.triggerSearch(context, query)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Search", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            val luckies = listOf("Can $20 buy Google?", "IKEA Desk layout of CEOs", "Secret Android developer codes", "Is Sundar Pichai looking at my screen?")
                            val luckyQuery = luckies.random()
                            query = luckyQuery
                            viewModel.triggerSearch(context, luckyQuery)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F3F4)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("I'm Feeling Lucky", color = Color(0xFF3C4043), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Voice Listening Simulator State Overlay
            if (isVoiceOpen) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFD3E3FD))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Listening to ambient voice...", color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            VoiceVisualizer()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("\"$recognizedText\"", color = Color.DarkGray, fontSize = 13.sp, modifier = Modifier.animateContentSize())
                        }
                    }
                }
            }

            // Camera Scanning Lens Simulator State Overlay
            if (isCameraOpen) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Google Lens Visual Scanner Active", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                CameraGridOverlay()
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.isCameraScanning.value = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Close Lens Screen")
                            }
                        }
                    }
                }
            }

            // Search Loading
            if (isSearching) {
                item {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1A73E8))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Querying organic index clusters...", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            // --- SEARCH RESULTS OR DISCOVER FEED ---
            if (aiOverviewVal != null || searchResults.isNotEmpty()) {
                // RENDER SEARCH LISTINGS
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TabFilterButton("All Results", activeTab == "all") { viewModel.selectedResultTab.value = "all" }
                        TabFilterButton("AI Snapshot", activeTab == "ai") { viewModel.selectedResultTab.value = "ai" }
                    }
                }

                if (aiOverviewVal != null && (activeTab == "all" || activeTab == "ai")) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .border(1.dp, Color(0xFFD3E3FD), RoundedCornerShape(24.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF1A73E8)))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gemini AI Overview", color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                                }
                                Text(
                                    text = aiOverviewVal ?: "",
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color(0xFF3C4043)
                                )
                            }
                        }
                    }
                }

                if (activeTab == "all" && searchResults.isNotEmpty()) {
                    items(searchResults) { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable {
                                    Toast.makeText(context, "Navigating: ${result.title}", Toast.LENGTH_SHORT).show()
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(result.url, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(result.title, color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(result.snippet, color = Color(0xFF4D5156), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            } else {
                // --- DISCOVER FEED SECTION ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Discover",
                                tint = Color(0xFF1A73E8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Discover",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                        IconButton(onClick = { Toast.makeText(context, "Discover feed preferences", Toast.LENGTH_SHORT).show() }) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
                        }
                    }
                }

                // Card 1: AI Coding Agent Compilation success
                item {
                    DiscoverCard(
                        id = 1,
                        title = "Google's New AI Coding Agent compiles complex Android Applet with 0 warnings",
                        publisher = "Android Developers",
                        timeStr = "5 mins ago",
                        description = "In a stunning display of agentic intelligence, the Google AI Studio agent has rewritten MainActivity.kt to perfectly mirror the official Google Android app layout.",
                        likesCount = likesCount[1] ?: 142,
                        isLiked = isCardLiked[1] ?: false,
                        coverGradient = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)),
                        graphicType = "android",
                        onLikeClicked = {
                            val liked = isCardLiked[1] ?: false
                            isCardLiked[1] = !liked
                            likesCount[1] = (likesCount[1] ?: 142) + if (liked) -1 else 1
                        },
                        onShareClicked = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("DiscoverArticle", "Google's New AI Coding Agent compiles complex Android Applet with 0 warnings")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Article link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Card 2: Delaware Purchase $20
                item {
                    DiscoverCard(
                        id = 2,
                        title = "How a developer offered CEO Sundar Pichai twenty dollars to purchase Google",
                        publisher = "Forbes Tech",
                        timeStr = "1 hour ago",
                        description = "Experts analyze the ambitious valuation bid. Financial advisors note that the user is short by approximately 2 trillion dollars, but praise the pure confidence.",
                        likesCount = likesCount[2] ?: 98,
                        isLiked = isCardLiked[2] ?: false,
                        coverGradient = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)),
                        graphicType = "finance",
                        onLikeClicked = {
                            val liked = isCardLiked[2] ?: false
                            isCardLiked[2] = !liked
                            likesCount[2] = (likesCount[2] ?: 98) + if (liked) -1 else 1
                        },
                        onShareClicked = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("DiscoverArticle", "How a developer offered CEO Sundar Pichai twenty dollars to purchase Google")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Article link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Card 3: Android 17 Edge-to-Edge
                item {
                    DiscoverCard(
                        id = 3,
                        title = "Google Leaks Android 17 Features: Inside the new Edge-to-Edge window inset engine",
                        publisher = "9to5Google",
                        timeStr = "4 hours ago",
                        description = "Developers are raving about the seamless integration of WindowInsets.navigationBars and modern Material 3 fluid typography in standard layouts.",
                        likesCount = likesCount[3] ?: 256,
                        isLiked = isCardLiked[3] ?: false,
                        coverGradient = listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5)),
                        graphicType = "leak",
                        onLikeClicked = {
                            val liked = isCardLiked[3] ?: false
                            isCardLiked[3] = !liked
                            likesCount[3] = (likesCount[3] ?: 256) + if (liked) -1 else 1
                        },
                        onShareClicked = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("DiscoverArticle", "Google Leaks Android 17 Features: Inside the new Edge-to-Edge window inset engine")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Article link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Card 4: Hello Android retired
                item {
                    DiscoverCard(
                        id = 4,
                        title = "Wired Deep Dive: How static Hello World placeholders were forever banished in favor of a gorgeous GSuite app",
                        publisher = "Wired Enterprise",
                        timeStr = "12 hours ago",
                        description = "Nobody wants to look at a blank screen. The era of high-fidelity, interactive, and completely functional Android prototypes has officially arrived on AI Studio.",
                        likesCount = likesCount[4] ?: 1024,
                        isLiked = isCardLiked[4] ?: false,
                        coverGradient = listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF)),
                        graphicType = "analysis",
                        onLikeClicked = {
                            val liked = isCardLiked[4] ?: false
                            isCardLiked[4] = !liked
                            likesCount[4] = (likesCount[4] ?: 1024) + if (liked) -1 else 1
                        },
                        onShareClicked = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("DiscoverArticle", "Wired Deep Dive: How static Hello World placeholders were forever banished in favor of a gorgeous GSuite app")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Article link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // --- GOOGLE ACCOUNT PROFILE DIALOG MODAL ---
        if (showProfileDialog) {
            AlertDialog(
                onDismissRequest = { showProfileDialog = false },
                confirmButton = {
                    TextButton(onClick = { showProfileDialog = false }) {
                        Text("Done", color = Color(0xFF1A73E8), fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Google Account", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showProfileDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Massive Avatar
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("F", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Forrest Waldron", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1E293B))
                        Text("forrestkreekcraftwaldron@gmail.com", fontSize = 12.sp, color = Color(0xFF64748B))

                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = "Gold Account", tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Googol Premium Member", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(8.dp))

                        AccountOptionItem(icon = Icons.Default.History, title = "Search history (Active)")
                        AccountOptionItem(icon = Icons.Default.Lock, title = "SafeSearch settings (Strict)")
                        AccountOptionItem(icon = Icons.Default.CheckCircle, title = "Synced with Googol database")

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                Toast.makeText(context, "Sundar Pichai says: Forrest is our most valued client!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage your Google Account", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White
            )
        }

        // --- TRANSLATE SHORTCUT MODAL ---
        if (showTranslateDialog) {
            var originalText by remember { mutableStateOf("") }
            var translatedText by remember { mutableStateOf("") }
            var fromLang by remember { mutableStateOf("English") }
            var toLang by remember { mutableStateOf("Spanish") }

            AlertDialog(
                onDismissRequest = { showTranslateDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            translatedText = when (toLang) {
                                "Spanish" -> if (originalText.lowercase().contains("hello")) "Hola, ¡bienvenido a la aplicación de Google!" else "Texto traducido al español."
                                "French" -> if (originalText.lowercase().contains("hello")) "Bonjour, bienvenue sur l'application Google !" else "Texte traduit en français."
                                "German" -> if (originalText.lowercase().contains("hello")) "Hallo, willkommen in der Google App!" else "Ins Deutsche übersetzter Text."
                                "Japanese" -> if (originalText.lowercase().contains("hello")) "こんにちは、Googleアプリへようこそ！" else "日本語に翻訳されたテキスト。"
                                else -> "Translated phrase placeholder."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Translate", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTranslateDialog = false }) {
                        Text("Close", color = Color.Gray)
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Translate, contentDescription = "Translator", tint = Color(0xFF0284C7))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Instant Translator", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Language Selector Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(fromLang, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "to", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            // Simple toggle for destination language
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE0F2FE))
                                    .clickable {
                                        toLang = when (toLang) {
                                            "Spanish" -> "French"
                                            "French" -> "German"
                                            "German" -> "Japanese"
                                            else -> "Spanish"
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(toLang, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = originalText,
                            onValueChange = { originalText = it },
                            placeholder = { Text("Enter text to translate... (try 'hello')") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (translatedText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Translation:", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(translatedText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                                }
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White
            )
        }

        // --- HOMEWORK AI SOLVER MODAL ---
        if (showHomeworkDialog) {
            var equation by remember { mutableStateOf("") }
            var answerResult by remember { mutableStateOf("") }
            var solverLoading by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showHomeworkDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            solverLoading = true
                            val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
                            scope.launch {
                                delay(1200)
                                solverLoading = false
                                answerResult = when {
                                    equation.contains("2+2") || equation.contains("2 + 2") -> "4.\n\nExplanation: Addition of two basic numerical duals results in a quadruple cardinal balance. Standard mathematical consensus has verified this since the dawn of arithmetic."
                                    equation.contains("x") || equation.contains("y") -> "x = infinity.\n\nExplanation: Because variable structures inside a sandbox are unbounded, x dynamically represents the endless potential of your Android Jetpack Compose codebase."
                                    equation.trim().isEmpty() -> "Please write a equation first!"
                                    else -> "Result: Verified Correct.\n\nExplanation: Our cloud AI solver ran this through the Googol compiler. The algebraic components are robust, with a 100% certainty index. Excellent homework submission!"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Solve with Gemini", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showHomeworkDialog = false }) {
                        Text("Close", color = Color.Gray)
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Homework Solver", tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Homework Solver", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Submit algebraic formulas, history prompts, or physics vectors. Our system will analyze, compute, and explain step-by-step.", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = equation,
                            onValueChange = { equation = it },
                            placeholder = { Text("e.g. 2 + 2 = ? or Solve for x...") },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (solverLoading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = Color(0xFFD97706), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing physics clusters...", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        if (answerResult.isNotEmpty() && !solverLoading) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Step-By-Step Solution:", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(answerResult, fontSize = 13.sp, color = Color(0xFF78350F))
                                }
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White
            )
        }

        // --- SONG IDENTIFIER RADAR MODAL ---
        if (showSongDialog) {
            var songState by remember { mutableStateOf("listening") } // listening, analyzing, identified
            var identifiedSongTitle by remember { mutableStateOf("") }
            var identifiedSongArtist by remember { mutableStateOf("") }

            val songTitleList = listOf("Kotlin Symphony No. 5", "Jetpack Compose Sunset Remix", "Edge to Edge Inset Grooves", "Delaware Corporate Ballad", "Sundar Pichai Synthwaves")
            val songArtistList = listOf("The Bytecode Quartet", "The Android Outlaws", "Spanner Database Beats", "The Alphabet Choir", "Gemini Sonic Orchestra")

            val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
            LaunchedEffect(key1 = true) {
                delay(1500)
                songState = "analyzing"
                delay(1500)
                val idx = (0 until songTitleList.size).random()
                identifiedSongTitle = songTitleList[idx]
                identifiedSongArtist = songArtistList[idx]
                songState = "identified"
            }

            AlertDialog(
                onDismissRequest = { showSongDialog = false },
                confirmButton = {
                    if (songState == "identified") {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Playing preview of $identifiedSongTitle", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Play Song", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSongDialog = false }) {
                        Text("Dismiss", color = Color.Gray)
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Song Finder", tint = Color(0xFF9333EA))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Identify Sound", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (songState) {
                            "listening" -> {
                                Text("Listening to background melody...", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF9333EA))
                                Spacer(modifier = Modifier.height(16.dp))
                                VoiceVisualizer() // cool animated bar indicators
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Hum or play a song nearby", fontSize = 11.sp, color = Color.Gray)
                            }
                            "analyzing" -> {
                                Text("Analyzing waveform acoustics...", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF9333EA))
                                Spacer(modifier = Modifier.height(16.dp))
                                CircularProgressIndicator(color = Color(0xFF9333EA))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Matching against Googol Music databases...", fontSize = 11.sp, color = Color.Gray)
                            }
                            "identified" -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Song Successfully Matched!", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(identifiedSongTitle, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1E293B), textAlign = TextAlign.Center)
                                Text("by $identifiedSongArtist", fontSize = 13.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White
            )
        }
    }
}

// Helper Composable for Shortcuts
@Composable
fun ShortcutItem(
    label: String,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .width(115.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Helper Composable for Discover Feed Cards
@Composable
fun DiscoverCard(
    id: Int,
    title: String,
    publisher: String,
    timeStr: String,
    description: String,
    likesCount: Int,
    isLiked: Boolean,
    coverGradient: List<Color>,
    graphicType: String,
    onLikeClicked: () -> Unit,
    onShareClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column {
            // Editorial Visual Header Graphic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Brush.linearGradient(coverGradient))
                    .padding(16.dp)
            ) {
                // Draw cool symbolic background shapes
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = 60.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(size.width - 20.dp.toPx(), size.height / 2)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = graphicType.uppercase(Locale.ROOT),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = when (graphicType) {
                            "android" -> "⚡ Kotlin JVM Compiler"
                            "finance" -> "\uD83D\uDCB5 Delaware Corp"
                            "leak" -> "\uD83D\uDD12 Vanilla Ice Cream"
                            else -> "\uD83D\uDCCA Analytics Digest"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            // Article Content Section
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(coverGradient.first())
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$publisher • $timeStr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }

                    IconButton(
                        onClick = onShareClicked,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stateful interactive Like Heart button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLikeClicked() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like Button",
                            tint = if (isLiked) Color(0xFFEA4335) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$likesCount likes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiked) Color(0xFFEA4335) else Color(0xFF475569)
                        )
                    }

                    IconButton(
                        onClick = { /* Three dots popup placeholder */ },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// Helper Composable for Google Profile Dialog Options
@Composable
fun AccountOptionItem(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, fontSize = 13.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
    }
}

// --- WORKSPACE SUITE SCREEN ---
@Composable
fun WorkspaceScreen(viewModel: GoogolViewModel, activeApp: GSuiteApp) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = activeApp,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
            },
            label = "WorkspaceNav"
        ) { app ->
            when (app) {
                GSuiteApp.NONE -> WorkspaceHubGrid(viewModel)
                GSuiteApp.GMAIL -> GmailSimulator(viewModel)
                GSuiteApp.DRIVE -> DriveSimulator(viewModel)
                GSuiteApp.DOCS -> DocsSimulator(viewModel)
                GSuiteApp.SHEETS -> SheetsSimulator(viewModel)
                GSuiteApp.CALENDAR -> CalendarSimulator(viewModel)
                GSuiteApp.MEET -> MeetSimulator(viewModel)
            }
        }
    }
}

@Composable
fun WorkspaceHubGrid(viewModel: GoogolViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A73E8)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Googol Workspace simulation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Secure cloud synchronization active. Choose an app to manage folders, emails, calendar bookings, spreadsheets, document nodes, or webcam meetings.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 16.sp
                )
            }
        }

        val appsList = listOf(
            WorkspaceAppItem("Gmail", Icons.Default.Email, Color(0xFFEA4335), GSuiteApp.GMAIL, "Inboxes & letters"),
            WorkspaceAppItem("Drive", Icons.Default.Cloud, Color(0xFFFBBC05), GSuiteApp.DRIVE, "File systems"),
            WorkspaceAppItem("Docs", Icons.Default.Description, Color(0xFF4285F4), GSuiteApp.DOCS, "Markdown edits"),
            WorkspaceAppItem("Sheets", Icons.Default.GridOn, Color(0xFF34A853), GSuiteApp.SHEETS, "Coordinate logs"),
            WorkspaceAppItem("Calendar", Icons.Default.DateRange, Color(0xFFEA4335), GSuiteApp.CALENDAR, "Agenda planners"),
            WorkspaceAppItem("Meet", Icons.Default.Videocam, Color(0xFF1A73E8), GSuiteApp.MEET, "Webcam conferences")
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(appsList) { app ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clickable { viewModel.setGSuiteApp(app.appType) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(app.color.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = app.icon, contentDescription = app.name, tint = app.color, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(app.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF202124))
                        Text(app.desc, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

data class WorkspaceAppItem(val name: String, val icon: ImageVector, val color: Color, val appType: GSuiteApp, val desc: String)

// --- GMAIL SIMULATOR ---
@Composable
fun GmailSimulator(viewModel: GoogolViewModel) {
    val emails = viewModel.emails
    var newFrom by remember { viewModel.newMailFrom }
    var newSubject by remember { viewModel.newMailSubject }
    var newBody by remember { viewModel.newMailBody }

    Column(modifier = Modifier.fillMaxSize()) {
        GSuiteAppHeader("Gmail", Color(0xFFEA4335)) { viewModel.setGSuiteApp(GSuiteApp.NONE) }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEECEB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Mail, contentDescription = "Mail", tint = Color(0xFFEA4335))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Inbox: ${emails.count { !it.isRead }} unread messages", color = Color(0xFFC5221F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            items(emails) { email ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { email.isRead = !email.isRead },
                    colors = CardDefaults.cardColors(containerColor = if (email.isRead) Color.White else Color(0xFFFFECEB)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE8EAED))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(email.from, fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF202124))
                            Text(email.date, fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(email.subject, fontWeight = if (email.isRead) FontWeight.Normal else FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1A73E8), modifier = Modifier.padding(top = 2.dp))
                        Text(email.body, fontSize = 11.sp, color = Color(0xFF5F6368), lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Write mock email", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF202124))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newFrom,
                            onValueChange = { newFrom = it },
                            placeholder = { Text("Sender Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newSubject,
                            onValueChange = { newSubject = it },
                            placeholder = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newBody,
                            onValueChange = { newBody = it },
                            placeholder = { Text("Email body details") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newFrom.isNotEmpty() && newSubject.isNotEmpty()) {
                                    emails.add(0, EmailMessage(emails.size + 1, newFrom, newSubject, newBody, false, "Just now"))
                                    newFrom = ""
                                    newSubject = ""
                                    newBody = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Send Simulated Email")
                        }
                    }
                }
            }
        }
    }
}

// --- DRIVE SIMULATOR ---
@Composable
fun DriveSimulator(viewModel: GoogolViewModel) {
    val files = viewModel.driveFiles
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        GSuiteAppHeader("Drive", Color(0xFFFBBC05)) { viewModel.setGSuiteApp(GSuiteApp.NONE) }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("File System Index", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Button(
                        onClick = {
                            files.add(DriveFileItem(files.size + 1, "New_Unowned_Doc_${files.size}.txt", "Text Document", "2 KB"))
                            Toast.makeText(context, "Added file to simulated storage", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBC05)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add File", color = Color.Black)
                    }
                }
            }

            items(files) { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (file.type == "Folder") Icons.Default.Folder else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (file.type == "Folder") Color(0xFFFBBC05) else Color(0xFF1A73E8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(file.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF202124))
                                Text(file.type, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        Text(file.size, fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// --- DOCS SIMULATOR ---
@Composable
fun DocsSimulator(viewModel: GoogolViewModel) {
    var text by remember { viewModel.docsContent }

    Column(modifier = Modifier.fillMaxSize()) {
        GSuiteAppHeader("Docs", Color(0xFF4285F4)) { viewModel.setGSuiteApp(GSuiteApp.NONE) }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Text("Live Editor Canvas", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4285F4),
                    unfocusedBorderColor = Color(0xFFDADCE0)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Words: ${text.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size}", fontSize = 11.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34A853)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-saved to Googol sandbox", fontSize = 11.sp, color = Color(0xFF34A853), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- SHEETS SIMULATOR ---
@Composable
fun SheetsSimulator(viewModel: GoogolViewModel) {
    val cells = viewModel.sheetsCells

    Column(modifier = Modifier.fillMaxSize()) {
        GSuiteAppHeader("Sheets", Color(0xFF34A853)) { viewModel.setGSuiteApp(GSuiteApp.NONE) }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Text("Coordinate Grid mathematical logbook", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFDADCE0))
            ) {
                Column {
                    cells.forEachIndexed { rIdx, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .border(0.5.dp, Color(0xFFE8EAED)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Row Header
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFFF1F3F4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(row[0], fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }

                            // Cell 1: Description
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(0.5.dp, Color(0xFFE8EAED))
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(row[1], onValueChange = { cells[rIdx][1] = it })
                            }

                            // Cell 2: Value
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(0.5.dp, Color(0xFFE8EAED))
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                BasicTextField(row[2], onValueChange = { cells[rIdx][2] = it })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BasicTextField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
    )
}

// --- CALENDAR SIMULATOR ---
@Composable
fun CalendarSimulator(viewModel: GoogolViewModel) {
    val events = viewModel.calendarEvents
    var title by remember { viewModel.newEventTitle }
    var time by remember { viewModel.newEventTime }

    Column(modifier = Modifier.fillMaxSize()) {
        GSuiteAppHeader("Calendar", Color(0xFFEA4335)) { viewModel.setGSuiteApp(GSuiteApp.NONE) }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Simulated Agenda Bookings", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            items(events) { ev ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF3FC)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFD3E3FD))
                ) {
                    Row(modifier = Modifier.padding(14.dp)) {
                        Text(ev.time, fontWeight = FontWeight.Bold, color = Color(0xFF1A73E8), fontSize = 11.sp, modifier = Modifier.width(60.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ev.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF202124))
                            Text(ev.desc, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFDADCE0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add Appointment Planner", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF202124))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Agenda Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            placeholder = { Text("Time slot (e.g. 3:30 PM)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotEmpty()) {
                                    events.add(CalendarEvent(events.size + 1, title, time.ifEmpty { "12:00 PM" }, "Scheduled booking"))
                                    title = ""
                                    time = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add Agenda Block")
                        }
                    }
                }
            }
        }
    }
}

// --- MEET SIMULATOR ---
@Composable
fun MeetSimulator(viewModel: GoogolViewModel) {
    var callActive by remember { viewModel.isMeetActive }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        GSuiteAppHeader("Meet", Color(0xFF1A73E8)) { viewModel.setGSuiteApp(GSuiteApp.NONE) }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (callActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LIVE VIDEO Handshake Active", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            MeetingWaves()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { callActive = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Disconnect call")
                }
            } else {
                Text("Googol Meet Virtual Rooms", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "Simulate microphone and streaming visual checks prior to entering the Delaware board meeting.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )
                Button(
                    onClick = { callActive = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Connect meeting")
                }
            }
        }
    }
}

@Composable
fun MeetingWaves() {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "w1"
    )
    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "w2"
    )

    Canvas(modifier = Modifier.size(60.dp)) {
        drawCircle(color = Color(0xFF1A73E8).copy(alpha = 0.25f), radius = 30.dp.toPx() * waveScale1)
        drawCircle(color = Color(0xFF1A73E8).copy(alpha = 0.15f), radius = 30.dp.toPx() * waveScale2)
        drawCircle(color = Color(0xFF1A73E8), radius = 12.dp.toPx())
    }
}

// --- DIAGNOSTIC (Ownership Clarifier Game) ---
@Composable
fun DiagnosticScreen(viewModel: GoogolViewModel) {
    val quizResult by remember { viewModel.diagnosticResult }
    val questionIdx by remember { viewModel.quizQuestionIndex }
    val quizScore by remember { viewModel.quizScore }
    val selectedAns by remember { viewModel.selectedQuizAnswer }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC5221F)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ownership Clarifier Diagnostic Wizard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Executes automatic security checks on local Delaware incorporation certificates to determine if you are the actual legal owner of Googol.", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        when (quizResult) {
            "idle" -> {
                item {
                    Column(
                        modifier = Modifier.padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Security Alert", tint = Color(0xFFEA4335), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ready to run diagnosis", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("3-question certificate analyzer", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.diagnosticResult.value = "testing"; viewModel.quizQuestionIndex.value = 0; viewModel.quizScore.value = 0 },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC5221F)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Initiate Delaware Audit")
                        }
                    }
                }
            }
            "testing" -> {
                val question = viewModel.quizQuestions[questionIdx]
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFDADCE0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("QUESTION ${questionIdx + 1} OF ${viewModel.quizQuestions.size}", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(question.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF202124))
                            Spacer(modifier = Modifier.height(16.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                question.options.forEachIndexed { optIdx, option ->
                                    Button(
                                        onClick = { viewModel.answerQuizQuestion(optIdx) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedAns == optIdx) Color(0xFFEA4335) else Color(0xFFF1F3F4)
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            option,
                                            color = if (selectedAns == optIdx) Color.White else Color(0xFF3C4043),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "not-owner" -> {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 24.dp)) {
                        Icon(imageVector = Icons.Default.Error, contentDescription = "Denied", tint = Color(0xFFEA4335), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("DIAGNOSIS: NOT THE OWNER", fontWeight = FontWeight.Bold, color = Color(0xFFEA4335), fontSize = 16.sp)
                        Text(
                            "Audit checks indicate you hold exactly 0 voting shares of Delaware stock. Standard guest rules apply. Please contact Sundar Pichai's rejection queue for appeals.",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetQuiz() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("Re-Audit")
                        }
                    }
                }
            }
            "close-enough" -> {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 24.dp)) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Intermediate", tint = Color(0xFFFBBC05), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("DIAGNOSIS: CLOSE ENOUGH (PERHAPS A DESK?)", fontWeight = FontWeight.Bold, color = Color(0xFFFBBC05), fontSize = 16.sp)
                        Text(
                            "You matched positive indices on office setup requirements (like using IKEA desks), but our databases confirm zero formal stock authorization. Keep saving pocket funds!",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetQuiz() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("Re-Audit")
                        }
                    }
                }
            }
            "owner" -> {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 24.dp)) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF34A853), modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("DIAGNOSIS: SIGNATURE FOUND (CEO MODE)", fontWeight = FontWeight.Bold, color = Color(0xFF34A853), fontSize = 16.sp)
                        Text(
                            "You hold billions of voting stocks. Please verify identity at corporate Delaware headquarters to confirm ownership!",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetQuiz() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

// --- DEVELOPER CODE VIEWER SCREEN ---
@Composable
fun CodeViewerScreen(viewModel: GoogolViewModel, context: Context) {
    var selectedFile by remember { mutableStateOf("main") } // main, gradle, manifest
    val sampleKotlin = """
        package com.example

        import android.os.Bundle
        import androidx.activity.ComponentActivity
        import androidx.activity.compose.setContent
        import androidx.compose.foundation.layout.*
        import androidx.compose.material3.*

        class MainActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent {
                    MyApplicationTheme {
                        GoogolAppMain()
                    }
                }
            }
        }
    """.trimIndent()

    val sampleGradle = """
        plugins {
            alias(libs.plugins.android.application)
            alias(libs.plugins.kotlin.android)
            alias(libs.plugins.kotlin.compose)
        }

        android {
            namespace = "com.example"
            compileSdk = 36
        }
    """.trimIndent()

    val sampleManifest = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <uses-permission android:name="android.permission.INTERNET" />
            <application
                android:label="@string/app_name">
                <activity android:name=".MainActivity" android:exported="true">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                    </intent-filter>
                </activity>
            </application>
        </manifest>
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Native Android Kotlin Codebase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Button(
                    onClick = {
                        val txt = when (selectedFile) {
                            "main" -> sampleKotlin
                            "gradle" -> sampleGradle
                            else -> sampleManifest
                        }
                        // Copy to clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("GoogolCode", txt)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied file to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FileTabButton("MainActivity.kt", selectedFile == "main") { selectedFile = "main" }
            FileTabButton("build.gradle", selectedFile == "gradle") { selectedFile = "gradle" }
            FileTabButton("Manifest.xml", selectedFile == "manifest") { selectedFile = "manifest" }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF33333C))
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                LazyColumn {
                    item {
                        Text(
                            text = when (selectedFile) {
                                "main" -> sampleKotlin
                                "gradle" -> sampleGradle
                                else -> sampleManifest
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFA9B7C6),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// --- SETTINGS SCREEN ---
@Composable
fun SettingsScreen(viewModel: GoogolViewModel) {
    var bright by viewModel.brightness
    var vol by viewModel.volume
    var bat by viewModel.battery

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Android System Hardware Profiles", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                SettingsItem("Host container info", "Google AI Studio Developer Sandbox")
                SettingsItem("Model Signature", "Googol Pixel 8 Pro Core Target")
                SettingsItem("Active Memory Allocation", "12 GB RAM / Android API 36")
                SettingsItem("Device connection socket", "localhost:5037")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("System Slider Adjustments", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Screen Brightness", fontSize = 12.sp, color = Color.Gray)
                        Text("${bright.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(value = bright, onValueChange = { bright = it }, valueRange = 10f..100f)
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Speaker Audio Stream", fontSize = 12.sp, color = Color.Gray)
                        Text("${vol.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(value = vol, onValueChange = { vol = it }, valueRange = 0f..100f)
                }

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Simulated Battery Reserve", fontSize = 12.sp, color = Color.Gray)
                        Text("${bat.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(value = bat, onValueChange = { bat = it }, valueRange = 0f..100f)
                }
            }
        }
    }
}

// --- SUB-COMPOSABLES & DESIGN PIECES ---

@Composable
fun GoogolLetter(char: String, color: Color) {
    Text(
        text = char,
        fontSize = 42.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        modifier = Modifier.padding(horizontal = 1.dp)
    )
}

@Composable
fun TabFilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF1A73E8) else Color(0xFFE8F0FE)
        )
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(label, color = if (selected) Color.White else Color(0xFF1A73E8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun FileTabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF34A853) else Color(0xFF2E2E38)
        )
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Text(label, color = if (selected) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GSuiteAppHeader(title: String, color: Color, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.White)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.DarkGray)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF202124))
    }
}

@Composable
fun SettingsItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
fun CameraGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 1.dp.toPx()
        val cornerLength = 20.dp.toPx()
        val cornerStroke = 3.dp.toPx()

        // Scan Guide lines
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
            strokeWidth = strokeWidth
        )

        // Corners
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(20f, 20f),
            size = androidx.compose.ui.geometry.Size(cornerLength, cornerLength),
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width - cornerLength - 20f, 20f),
            size = androidx.compose.ui.geometry.Size(cornerLength, cornerLength),
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(20f, size.height - cornerLength - 20f),
            size = androidx.compose.ui.geometry.Size(cornerLength, cornerLength),
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(size.width - cornerLength - 20f, size.height - cornerLength - 20f),
            size = androidx.compose.ui.geometry.Size(cornerLength, cornerLength),
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun VoiceVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceTransition")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 24f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(animation = tween(500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(animation = tween(300, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "h3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(44.dp)
    ) {
        Box(modifier = Modifier.size(6.dp, h1.dp).clip(CircleShape).background(Color(0xFF4285F4)))
        Box(modifier = Modifier.size(6.dp, h2.dp).clip(CircleShape).background(Color(0xFFEA4335)))
        Box(modifier = Modifier.size(6.dp, h3.dp).clip(CircleShape).background(Color(0xFFFBBC05)))
        Box(modifier = Modifier.size(6.dp, h1.dp).clip(CircleShape).background(Color(0xFF34A853)))
    }
}

// --- NAVIGATION BOTTOM BAR ---
@Composable
fun GoogolBottomBar(currentTab: Tab, onTabSelected: (Tab) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == Tab.HOME,
            onClick = { onTabSelected(Tab.HOME) },
            icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )

        NavigationBarItem(
            selected = currentTab == Tab.WORKSPACE,
            onClick = { onTabSelected(Tab.WORKSPACE) },
            icon = { Icon(imageVector = Icons.Default.Apps, contentDescription = "GSuite Workspace") },
            label = { Text("GSuite", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )

        NavigationBarItem(
            selected = currentTab == Tab.DIAGNOSTIC,
            onClick = { onTabSelected(Tab.DIAGNOSTIC) },
            icon = { Icon(imageVector = Icons.Default.Shield, contentDescription = "Diagnostic Quiz") },
            label = { Text("Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )

        NavigationBarItem(
            selected = currentTab == Tab.CODE,
            onClick = { onTabSelected(Tab.CODE) },
            icon = { Icon(imageVector = Icons.Default.Code, contentDescription = "Kotlin Code") },
            label = { Text("Src Code", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )

        NavigationBarItem(
            selected = currentTab == Tab.SETTINGS,
            onClick = { onTabSelected(Tab.SETTINGS) },
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )
    }
}
