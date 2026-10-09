package ro.daydreamstalgia.duelmastersinventory.features.transactions.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Rational
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import ro.daydreamstalgia.duelmastersinventory.BuildConfig
import ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model.CardPrintWithPrototype
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.cards.CardPrintImage
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.AmountText
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.AppConfirmDialog
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.core.ConditionChip
import ro.daydreamstalgia.duelmastersinventory.shared.ui.components.utils.CivilizationIcon
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.Conditions
import ro.daydreamstalgia.duelmastersinventory.shared.utils.constants.conditionColor
import ro.daydreamstalgia.duelmastersinventory.shared.ui.theme.RaceLabelStyle

/**
 * Camera scan for the inbound "Add cards" tab (spec `2b`). Capture is
 * matched against a precomputed ORB local-feature index
 * ([ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository.CardImageMatchRepository])
 * - fully offline, no ML model. Matching physical trading-card photos this
 * way is inherently approximate, so every step keeps a manual way out
 * ("Not it", search-and-add on a miss).
 */
/** Fraction of the preview width the on-screen guide box occupies, and its card aspect ratio (w/h). Shared between the guide overlay and the post-capture crop so the two always agree on the framed region. */
private const val GUIDE_WIDTH_FRACTION = 0.62f
private const val GUIDE_ASPECT = 0.7f

/** Full condition names for the confirm-sheet's condition selector captions (spec `card-scan.html`). */
private val ConditionFullNames = mapOf(
    "NM" to "Near mint",
    "LP" to "Light play",
    "MP" to "Moderate",
    "HP" to "Heavy play",
    "D" to "Damaged",
)

@Composable
fun CardScanScreen(
    navController: NavController,
    viewModel: CardScanScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    // Debug-only: opening a picked photo to run through the same matching pipeline as a real
    // capture, so the scanner can be exercised without a physical card or camera - e.g. against
    // the phone-photo test set in DuelMastersFoilReconstruct/data/raw. Unlike a live capture, the
    // whole photo is kept (not auto-cropped) - the user positions the crop themselves below.
    var pickedImage by remember { mutableStateOf<Bitmap?>(null) }
    // The purple guide border never moves - it's always centered at (0,0) additional offset from
    // the container's own center. These track how far the *photo* has been dragged/scaled/rotated
    // out from its initial fit-to-container placement, so (Offset.Zero, 1f, 0f) always means "whole
    // photo visible, centered under the border" regardless of container size.
    var pickedImageOffset by remember { mutableStateOf(Offset.Zero) }
    var pickedImageScale by remember { mutableFloatStateOf(1f) }
    var pickedImageRotation by remember { mutableFloatStateOf(0f) }
    var pickedEditorSize by remember { mutableStateOf(IntSize.Zero) }

    val fileOpenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val bitmap = loadFullBitmap(context, uri)
                if (bitmap != null) {
                    pickedImage = bitmap
                    pickedImageOffset = Offset.Zero
                    pickedImageScale = 1f
                    pickedImageRotation = 0f
                }
            }
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val manageStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)

        // Best-effort - lets scanned captures be written to storage/emulated/0/ddnostalgia/... .
        // Debug-only screen, so a denial just means that audit trail is skipped, nothing breaks.
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    manageStorageLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                    )
                } catch (_: Exception) {
                    try {
                        manageStorageLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    } catch (_: Exception) {
                    }
                }
            }
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val queue by viewModel.queue.collectAsState()
    val candidate by viewModel.currentCandidate.collectAsState()
    val matching by viewModel.matching.collectAsState()
    val hasMoreCandidates by viewModel.hasMoreCandidates.collectAsState()
    val matchVisualization by viewModel.matchVisualization.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val searchActive by viewModel.searchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val availableSets by viewModel.availableSets.collectAsState()
    val selectedSetFilter by viewModel.selectedSetFilter.collectAsState()
    var showSetFilterSheet by remember { mutableStateOf(false) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    // "Blitz" = the phone's camera flash/torch, not a capture mode - toggled independently of
    // capture so it can be switched on before shooting in a dim room.
    var torchOn by remember { mutableStateOf(false) }
    // Condition is only ever picked once a card has been identified - on the confirm sheet after
    // a confident match, or per-row in the no-match search slideout. There is no camera-screen
    // preset any more.
    var confirmCondition by remember { mutableStateOf("NM") }
    var capturing by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    // Every way off this screen (✕, system back, "Search instead") goes through here so a
    // non-empty queue always gets a chance to be kept - none of them save the queue, so leaving
    // silently would otherwise drop already-scanned copies with no way back.
    fun requestExit() {
        if (queue.isNotEmpty()) showExitConfirm = true else navController.navigateUp()
    }

    BackHandler(enabled = queue.isNotEmpty()) { showExitConfirm = true }

    // Captures exactly one frame (see decisions/0009: burst capture was dropped, matching every
    // frame of a 4-shot burst against the whole index made a scan press feel slow).
    suspend fun performCapture() {
        val capture = imageCapture ?: return
        capturing = true
        val frame = capture.captureCroppedBitmap(context)
        capturing = false
        if (frame != null) {
            viewModel.onCaptured(frame)
        }
    }

    // Crops whatever part of the picked photo currently sits under the fixed guide border and
    // runs it through the same match pipeline as a live capture.
    suspend fun performAnalyze() {
        val bitmap = pickedImage ?: return
        val size = pickedEditorSize
        if (size.width == 0 || size.height == 0) return
        capturing = true
        val cropped = withContext(kotlinx.coroutines.Dispatchers.Default) {
            cropUnderFixedBorder(bitmap, size, pickedImageOffset, pickedImageScale, pickedImageRotation)
        }
        capturing = false
        viewModel.onCaptured(cropped)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0D0E))) {
        if (pickedImage != null) {
            PickedImageEditor(
                bitmap = pickedImage!!,
                imageOffset = pickedImageOffset,
                imageScale = pickedImageScale,
                imageRotation = pickedImageRotation,
                onSizeChanged = { pickedEditorSize = it },
                onTransform = { newOffset, newScale, newRotation ->
                    pickedImageOffset = newOffset
                    pickedImageScale = newScale
                    pickedImageRotation = newRotation
                },
            )
        } else if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    // Bind after layout so previewView.width/height (needed for the ViewPort) are known.
                    previewView.post {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder().build()
                            imageCapture = capture

                            // Ties the captured JPEG's crop to exactly what the preview shows on
                            // screen (same visible region CameraX uses for the PreviewView's own
                            // FILL_CENTER scaling) - without this, ImageCapture returns the full,
                            // uncropped sensor frame, which has a different aspect ratio than the
                            // on-screen preview and makes the guide-box overlay purely decorative.
                            val viewPort = ViewPort.Builder(
                                Rational(previewView.width, previewView.height),
                                previewView.display.rotation
                            ).build()
                            val useCaseGroup = UseCaseGroup.Builder()
                                .addUseCase(preview)
                                .addUseCase(capture)
                                .setViewPort(viewPort)
                                .build()

                            try {
                                cameraProvider.unbindAll()
                                boundCamera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    useCaseGroup
                                )
                            } catch (_: Exception) {
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Card-shaped guide rect - must match GUIDE_WIDTH_FRACTION/GUIDE_ASPECT exactly, both
            // in size AND in being exactly screen-centered (cropToGuideBox() below assumes a
            // centered fraction-of-bitmap crop, and the preview fills this same Box edge-to-edge -
            // see the ViewPort comment above). BoxWithConstraints only computes the same centered
            // position explicitly, so a caption can be anchored below it without nesting the guide
            // box itself inside a Column (which would shift its center off-screen-center and
            // silently desync the visible frame from what actually gets cropped/matched).
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val guideWidth = maxWidth * GUIDE_WIDTH_FRACTION
                val guideHeight = guideWidth / GUIDE_ASPECT

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(guideWidth)
                        .height(guideHeight)
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, MaterialTheme.colorScheme.primary, Color.Transparent)
                                )
                            )
                    )
                }

                Text(
                    "Hold the card inside the frame",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = guideHeight / 2 + 10.dp)
                        .fillMaxWidth(GUIDE_WIDTH_FRACTION + 0.1f)
                        .padding(horizontal = 8.dp),
                )
            }
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Camera permission is needed to scan cards.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Search instead",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clickable { requestExit() },
                )
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { requestExit() }) {
                Text("✕", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Scan into #${viewModel.transactionId}", color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${queue.sumOf { it.count }} queued · nothing saved yet",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (pickedImage != null) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.09f))
                        // Same guard as the file-open button below - leaving the editor recycles
                        // pickedImage (see PickedImageEditor's DisposableEffect), so this waits
                        // out any in-flight Analyze crop reading that same bitmap.
                        .clickable(enabled = !capturing) { pickedImage = null }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Back to camera", tint = Color.White.copy(alpha = if (capturing) 0.3f else 0.85f), modifier = Modifier.size(18.dp))
                }
            }

            if (hasCameraPermission && pickedImage == null) {
                // Blitz: toggles the phone's camera flash/torch for scanning in dim light.
                // Independent of capture - flips instantly, no matching involved.
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (torchOn) Color(0xFFE3A94F).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                        .border(1.5.dp, if (torchOn) Color(0xFFE3A94F) else Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable {
                            val next = !torchOn
                            boundCamera?.cameraControl?.enableTorch(next)
                            torchOn = next
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⚡", style = MaterialTheme.typography.titleMedium)
                }

                // Limits scan matching to the sets checked in the slideout below (spec 0008) -
                // active (non-empty selection) gets the same on-state treatment as the torch.
                val filterActive = selectedSetFilter.isNotEmpty()
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (filterActive) Color(0xFFE3A94F).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                        .border(1.5.dp, if (filterActive) Color(0xFFE3A94F) else Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { showSetFilterSheet = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.FilterList,
                        contentDescription = "Limit scan to sets",
                        tint = if (filterActive) Color(0xFFE3A94F) else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (BuildConfig.DEBUG) {
                // Disabled mid-capture/analyze: picking a new photo here recycles the current
                // pickedImage bitmap once PickedImageEditor swaps to the new one (see its
                // DisposableEffect) - blocking this while an Analyze crop might still be reading
                // that same bitmap on a background dispatcher avoids racing a recycled-bitmap crash.
                IconButton(onClick = { fileOpenLauncher.launch("image/*") }, enabled = !capturing) {
                    Icon(Icons.Filled.FileOpen, contentDescription = "Open photo to scan", tint = Color.White.copy(alpha = if (capturing) 0.3f else 0.85f))
                }
            }
        }

        // Bottom controls: queue + capture + commit
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC0B0D0E))
                .padding(14.dp),
        ) {
            if (queue.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Queued for #${viewModel.transactionId}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), Color.Transparent)
                                )
                            ),
                    )
                }

                LazyColumn(modifier = Modifier.fillMaxWidth().height((queue.size.coerceAtMost(3) * 58).dp)) {
                    items(queue, key = { "${it.cardPrintId}-${it.condition}" }) { item ->
                        val print = viewModel.printFor(item.cardPrintId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(11.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            print?.print?.let {
                                CardPrintImage(
                                    it,
                                    modifier = Modifier
                                        .width(30.dp)
                                        .aspectRatio(40f / 60f)
                                        .clip(RoundedCornerShape(3.dp)),
                                )
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
                                Text(
                                    print?.prototype?.name ?: "#${item.cardPrintId}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                )
                                print?.print?.displayId()?.let {
                                    Text(
                                        it,
                                        color = Color.White.copy(alpha = 0.45f),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                            ConditionChip(item.condition, modifier = Modifier.padding(end = 8.dp))
                            AmountText("x${item.count}", color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // Capture button is a real Row cell (its own 64.dp-wide slot), not an overlay centered
            // on the full row - the old overlay approach centered the button against the row's
            // total width while the two pills split that width with *asymmetric* weights (1f vs
            // 1.25f, since "Add N to transaction" needs more room than "Search instead"), so the
            // wider pill's edge crept under the button. Giving the button its own cell makes that
            // impossible regardless of how long either pill's label gets (see
            // sources/design/scan-screens-redesign.html for the before/after).
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Search instead",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(15.dp))
                        .clickable { requestExit() }
                        .padding(vertical = 14.dp),
                )

                Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    if (hasCameraPermission || pickedImage != null) {
                        val busy = matching || capturing
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (busy) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary)
                                .clickable(enabled = !busy) {
                                    coroutineScope.launch {
                                        if (pickedImage != null) performAnalyze() else performCapture()
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, Color(0xFF3A0092), CircleShape))
                        }
                    }
                }

                Text(
                    "Add ${queue.sumOf { it.count }} to transaction",
                    color = Color(0xFF3A0092),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(15.dp))
                        .clickable(enabled = queue.isNotEmpty()) {
                            coroutineScope.launch {
                                viewModel.commitQueue()
                                navController.navigateUp()
                            }
                        }
                        .padding(vertical = 14.dp),
                )
            }
        }

        // Confirm-match panel - also the "Matching…" spinner's home while a capture is being
        // scored, and (on a miss) the home for the no-match search-and-add slideout below, so the
        // sheet slides up on every shutter press and its content just swaps in once a result
        // exists, instead of a separate spinner floating over the camera preview.
        val showSheet = matching || candidate != null || searchActive
        if (showSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.86f)
                    .background(Color(0xFF101416), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(width = 34.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.28f)),
                )

                val detail = candidate
                if (matching) {
                    Box(modifier = Modifier.fillMaxWidth().height(320.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                            )
                            Text(
                                "Matching…",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 14.dp),
                            )
                        }
                    }
                } else if (detail != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp, bottom = 14.dp)) {
                    Box(modifier = Modifier.size(7.dp).background(Color(0xFF7FD69B), CircleShape))
                    Text(
                        "Matched with ${detail.match.confidencePercent}% confidence",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Row {
                    detail.printWithPrototype?.print?.let {
                        CardPrintImage(
                            it,
                            modifier = Modifier
                                .width(104.dp)
                                .aspectRatio(0.7f)
                                .shadow(6.dp, RoundedCornerShape(7.dp)),
                        )
                    }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            detail.printWithPrototype?.prototype?.name ?: "Unknown card",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        detail.printWithPrototype?.prototype?.races?.let { races ->
                            Text(
                                races.uppercase(),
                                style = RaceLabelStyle,
                                color = Color(0xFFB5A6CF),
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        val prototype = detail.printWithPrototype?.prototype
                        if (prototype != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 9.dp)) {
                                CivilizationIcon(prototype.civilization, modifier = Modifier.size(13.dp), color = Color.White.copy(alpha = 0.6f))
                                Text(
                                    listOfNotNull(prototype.civilization, prototype.type, prototype.mana?.let { "$it mana" })
                                        .joinToString(" · "),
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                        Text(
                            detail.printWithPrototype?.print?.displayId() ?: "",
                            color = Color.White.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                // ORB keypoint-match render (query frame vs. reference art, lines connecting the
                // matched keypoints) - see CardImageMatchRepository.visualizeMatch. Null while
                // it's still rendering for this candidate (paging via "Not it" triggers a redraw).
                Text(
                    "MATCHED KEYPOINTS",
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    val viz = matchVisualization
                    if (viz != null) {
                        Image(
                            bitmap = viz.asImageBitmap(),
                            contentDescription = "Matched keypoints between the capture and the reference art",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                    } else {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 28.dp).size(20.dp),
                            color = Color.White.copy(alpha = 0.5f),
                            strokeWidth = 2.dp,
                        )
                    }
                }

                Text(
                    "CONDITION",
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Conditions.forEach { cond ->
                        val selected = cond == confirmCondition
                        val tint = conditionColor(cond)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) tint.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(11.dp)
                                )
                                .border(1.dp, if (selected) tint.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(11.dp))
                                .clickable { confirmCondition = cond }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(cond, color = if (selected) tint else Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                            Text(
                                ConditionFullNames[cond] ?: "",
                                color = if (selected) tint else Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Not it",
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .width(88.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .clickable { viewModel.rejectCandidate() }
                            .padding(vertical = 14.dp),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .clickable { viewModel.queueCandidate(detail.match.cardPrintId, confirmCondition) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Queue this copy", color = Color(0xFF3A0092), style = MaterialTheme.typography.labelLarge)
                        if (hasMoreCandidates) {
                            Text("· scan next", color = Color(0xFF3A0092).copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                } else if (searchActive) {
                    NoMatchSearchContent(
                        capturedBitmap = capturedBitmap,
                        query = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        results = searchResults,
                        onAdd = { printId, condition -> viewModel.addSearchResult(printId, condition) },
                        onSkip = { viewModel.skipSearch() },
                    )
                }
            }
        }

        if (showExitConfirm) {
            AppConfirmDialog(
                title = "Discard ${queue.sumOf { it.count }} scanned card${if (queue.sumOf { it.count } == 1) "" else "s"}?",
                message = "They haven't been added to transaction #${viewModel.transactionId} yet. Leaving now clears the queue.",
                confirmText = "Discard",
                onConfirm = {
                    showExitConfirm = false
                    navController.navigateUp()
                },
                onDismiss = { showExitConfirm = false },
                dismissText = "Keep scanning",
            )
        }

        if (showSetFilterSheet) {
            ScanSetFilterSheet(
                availableSets = availableSets,
                initialSelection = selectedSetFilter,
                onApply = {
                    viewModel.setSetFilter(it)
                    showSetFilterSheet = false
                },
                onDismiss = { showSetFilterSheet = false },
            )
        }
    }
}

/**
 * Slideout for spec 0008 - searchable, multiselect checkbox list of every loaded `(language,
 * set)` pair, narrowing subsequent scan matching to just the checked sets. Selection is staged
 * locally and only committed to the view model on "Apply"; dismissing without applying discards
 * the in-progress edit and leaves the previously-active filter untouched.
 */
@Composable
private fun ScanSetFilterSheet(
    availableSets: List<CardScanScreenViewModel.ScanSetOption>,
    initialSelection: Set<Pair<String, String>>,
    onApply: (Set<Pair<String, String>>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selection by remember { mutableStateOf(initialSelection) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(availableSets, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            availableSets
        } else {
            availableSets.filter {
                (it.displayName?.lowercase()?.contains(q) == true) || it.setCode.lowercase().contains(q)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() }
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(0.86f)
            .background(Color(0xFF101416), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 18.dp, vertical = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .align(Alignment.CenterHorizontally)
                .size(width = 34.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.28f)),
        )

        Text(
            "Limit scan to sets",
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Only checked sets are searched for a match. Leave nothing checked to search every loaded set.",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Search sets…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Select all",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable {
                    selection = selection + filtered.map { it.language to it.setCode }
                },
            )
            Text(
                "Clear",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable {
                    val filteredKeys = filtered.map { it.language to it.setCode }.toSet()
                    selection = selection - filteredKeys
                },
            )
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
            items(filtered, key = { "${it.language}-${it.setCode}" }) { option ->
                val key = option.language to option.setCode
                val checked = key in selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selection = if (checked) selection - key else selection + key }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { selection = if (it) selection + key else selection - key },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    )
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            option.displayName ?: option.setCode,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            "${option.language} · ${option.setCode}",
                            color = Color.White.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No sets match \"$query\"",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Cancel",
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .clickable { onDismiss() }
                    .padding(vertical = 14.dp),
            )
            Text(
                "Apply",
                color = Color(0xFF3A0092),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .clickable { onApply(selection) }
                    .padding(vertical = 14.dp),
            )
        }
    }
    }
}

/**
 * Slideout shown instead of a toast when a capture comes back with no confident ORB match: the
 * user can see what was captured, search the catalog by name/set, pick a condition (including
 * "no condition" - a physical copy doesn't always get graded on the spot), and add it straight to
 * the queue, or skip back to the camera/picked-image editor.
 */
@Composable
private fun NoMatchSearchContent(
    capturedBitmap: Bitmap?,
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<CardPrintWithPrototype>,
    onAdd: (Int, String?) -> Unit,
    onSkip: () -> Unit,
) {
    Text(
        "No confident match",
        color = Color.White,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        "Search for the card below, or skip and try scanning again.",
        color = Color.White.copy(alpha = 0.6f),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
    )

    if (capturedBitmap != null) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = "Captured card",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(90.dp)
                    .aspectRatio(GUIDE_ASPECT)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        "Search by name or set…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
                inner()
            },
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        if (query.isNotBlank() && results.isEmpty()) {
            Text(
                "No cards match \"$query\"",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        results.forEach { item ->
            // Keyed on the print's own id - the result list is rebuilt on every keystroke (see
            // decisions on LazyImage's IO dispatch), and without a key Compose would match rows
            // to slots by position instead of identity, letting a row's thumbnail lag behind a
            // different print's now-stale in-flight decode while its text has already moved on.
            key(item.print.id) {
            var condition by remember(item.print.id) { mutableStateOf<String?>(null) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(13.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardPrintImage(
                    item.print,
                    modifier = Modifier
                        .width(34.dp)
                        .aspectRatio(40f / 60f)
                        .clip(RoundedCornerShape(3.dp)),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(item.prototype.name ?: "", color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    Text(
                        item.print.displayId(),
                        color = Color.White.copy(alpha = 0.45f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                SearchConditionDropdown(condition, onSelect = { condition = it })
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onAdd(item.print.id, condition) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color(0xFF3A0092), style = MaterialTheme.typography.titleMedium)
                }
            }
            }
        }
    }

    Text(
        "Skip · back to camera",
        color = Color.White,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 18.dp)
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable { onSkip() }
            .padding(vertical = 14.dp),
    )
}

/** Per-row "NM ▾"/"- ▾" condition selector for the no-match search results - unlike the confirm sheet's chips, "-" (no condition) is a valid pick here since a card just found by search isn't necessarily being graded right now. */
@Composable
private fun SearchConditionDropdown(condition: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val color = conditionColor(condition)
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(condition ?: "-", style = MaterialTheme.typography.labelMedium, color = color)
            Text("▾", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("-") }, onClick = { onSelect(null); expanded = false })
            Conditions.forEach { cond ->
                DropdownMenuItem(
                    text = { Text(cond) },
                    onClick = {
                        onSelect(cond)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Full-screen picked-photo view with a *fixed* purple card-shaped guide border - it never moves.
 * Instead the photo itself is draggable/resizable/rotatable underneath it (one-finger drag pans,
 * two-finger pinch/twist scales and rotates - [detectTransformGestures] handles both), the same
 * way a phone's own photo-crop tool works. [onTransform] reports the photo's current offset from
 * its initial fit-to-container placement, scale relative to that same fit, and rotation in
 * degrees, so the caller can work out exactly what falls under the border on "Analyze".
 */
@Composable
private fun PickedImageEditor(
    bitmap: Bitmap,
    imageOffset: Offset,
    imageScale: Float,
    imageRotation: Float,
    onSizeChanged: (IntSize) -> Unit,
    onTransform: (Offset, Float, Float) -> Unit,
) {
    // pointerInput(Unit) below installs its gesture-detecting coroutine exactly once, so it must
    // read the current offset/scale/rotation through rememberUpdatedState rather than closing
    // over the plain parameters directly - otherwise every gesture after the first would compute
    // deltas against the stale values captured when the coroutine started, not the latest
    // committed transform.
    val currentOffset = androidx.compose.runtime.rememberUpdatedState(imageOffset)
    val currentScale = androidx.compose.runtime.rememberUpdatedState(imageScale)
    val currentRotation = androidx.compose.runtime.rememberUpdatedState(imageRotation)

    // DCIM/gallery photos decode to multi-megapixel bitmaps (tens of MB), and picking a new one
    // or leaving the editor otherwise just drops the old Bitmap object for the GC to catch up to
    // eventually. Recycling it explicitly frees that native memory immediately instead. Keyed on
    // (and closing over) `bitmap` itself, not the outer pickedImage state, so this only fires once
    // Compose has actually stopped drawing with this exact bitmap - either because [bitmap] changed
    // (a new photo was picked) or this composable left composition (editor closed) - never while a
    // frame might still be mid-draw with it, which is what makes explicit recycle() safe here.
    androidx.compose.runtime.DisposableEffect(bitmap) {
        onDispose { bitmap.recycle() }
    }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                containerSize = it
                onSizeChanged(it)
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rot ->
                    onTransform(
                        currentOffset.value + pan,
                        (currentScale.value * zoom).coerceIn(0.2f, 6f),
                        currentRotation.value + rot,
                    )
                }
            },
    ) {
        // contentScale = Fit centers the whole photo within this fillMaxSize box by default (no
        // transform applied yet) - graphicsLayer's default pivot (its own layout bounds' center,
        // which equals the container's center since the Image fills it) then pans/scales/rotates
        // from exactly that centered starting point, matching what cropUnderFixedBorder assumes.
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Picked photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = imageOffset.x
                    translationY = imageOffset.y
                    scaleX = imageScale
                    scaleY = imageScale
                    rotationZ = imageRotation
                },
        )

        if (containerSize.width > 0) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val baseWidthPx = containerSize.width * GUIDE_WIDTH_FRACTION
            val baseHeightPx = baseWidthPx / GUIDE_ASPECT

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(
                        width = with(density) { baseWidthPx.toDp() },
                        height = with(density) { baseHeightPx.toDp() },
                    )
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
            )
        }
        Text(
            "Drag to move · pinch to resize/rotate",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .fillMaxWidth(0.8f),
        )
    }
}

/** One shot from this [ImageCapture], cropped to the guide box. Suspends until the shutter callback fires. */
private suspend fun ImageCapture.captureCroppedBitmap(context: android.content.Context): Bitmap? =
    suspendCancellableCoroutine { cont ->
        takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    cont.resume(bitmap?.let { cropToGuideBox(it) })
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    cont.resume(null)
                }
            }
        )
    }

/**
 * JPEG-format capture from [ImageCapture.OnImageCapturedCallback], cropped to [ImageProxy.getCropRect]
 * and rotated per EXIF/sensor metadata. The decoded JPEG bytes are the full, uncropped sensor frame -
 * the [ViewPort] binding only makes CameraX *compute* the crop rect matching what the preview shows,
 * it doesn't bake that crop into the compressed JPEG data for the in-memory `OnImageCapturedCallback`
 * path. Skipping this step is what let [cropToGuideBox]'s fixed-fraction math silently operate on the
 * wrong (full sensor) aspect ratio instead of the viewport-matched one it assumes, exactly the failure
 * mode decisions/0005 fixed for the preview/guide-box side but missed here on the capture side.
 * `cropRect` is defined in the image's own, pre-rotation coordinate space, so this crop happens before
 * the rotation below, not after.
 */
private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val full = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

    val crop = image.cropRect
    val bitmap = if (crop.width() != full.width || crop.height() != full.height) {
        Bitmap.createBitmap(full, crop.left, crop.top, crop.width(), crop.height()).also { full.recycle() }
    } else {
        full
    }

    val rotation = image.imageInfo.rotationDegrees
    if (rotation == 0) return bitmap

    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { bitmap.recycle() }
}

/**
 * Crops exactly the on-screen guide-box region (see [GUIDE_WIDTH_FRACTION]/[GUIDE_ASPECT]) out
 * of the capture. Only valid because the ImageCapture use case is bound with a [ViewPort] shared
 * with the Preview, so [bitmap]'s aspect ratio already matches what's shown on screen - otherwise
 * this fraction-of-bitmap math wouldn't line up with the fraction-of-screen the guide box draws.
 */
private fun cropToGuideBox(bitmap: Bitmap): Bitmap {
    val cropWidth = (bitmap.width * GUIDE_WIDTH_FRACTION).toInt().coerceAtLeast(1)
    val cropHeight = (cropWidth / GUIDE_ASPECT).toInt().coerceIn(1, bitmap.height)
    val left = (bitmap.width - cropWidth) / 2
    val top = ((bitmap.height - cropHeight) / 2).coerceIn(0, bitmap.height - cropHeight)
    return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
}

/** Decodes [uri] at full resolution, uncropped - the picked-image editor lets the user position the crop themselves (drag/resize/rotate), unlike the old auto-centered debug crop. */
private suspend fun loadFullBitmap(context: android.content.Context, uri: Uri): Bitmap? =
    withContext(kotlinx.coroutines.Dispatchers.IO) {
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: return@withContext null

        // Gallery/DCIM photos (especially landscape ones) commonly store their real orientation
        // as EXIF metadata rather than in the pixel data itself - decodeStream above ignores that
        // entirely, which is what showed a landscape photo sideways in the editor. A second
        // stream read is needed since ExifInterface and BitmapFactory can't share one.
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use { androidx.exifinterface.media.ExifInterface(it).rotationDegrees }
        } catch (_: Exception) {
            null
        } ?: 0

        if (orientation == 0) decoded else {
            val matrix = Matrix().apply { postRotate(orientation.toFloat()) }
            // The with-matrix overload always allocates a new bitmap (never aliases decoded), so
            // decoded is safe to recycle immediately - it's a local, never handed to Compose.
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also { decoded.recycle() }
        }
    }

/**
 * Crops whatever part of [source] currently sits under [PickedImageEditor]'s *fixed*,
 * screen-centered guide border, given how far the photo has been panned/scaled/rotated
 * ([imageOffset]/[imageScale]/[imageRotationDegrees]) from its initial `ContentScale.Fit`
 * placement. Since the border never moves, this is the inverse of the old box-moves-over-a-static-
 * image math: work out which point of [source] is currently under the (fixed) border center, by
 * undoing the photo's own translate/scale/rotate. Draws directly into a target-sized canvas
 * (rather than rotating the whole source bitmap first) so the result is already axis-aligned and
 * exactly the crop size, regardless of how large [source] itself is.
 */
private fun cropUnderFixedBorder(
    source: Bitmap,
    containerSize: IntSize,
    imageOffset: Offset,
    imageScale: Float,
    imageRotationDegrees: Float,
): Bitmap {
    val srcAspect = source.width.toFloat() / source.height.toFloat()
    val containerAspect = containerSize.width.toFloat() / containerSize.height.toFloat()
    val fitScale = if (srcAspect > containerAspect) {
        containerSize.width / source.width.toFloat()
    } else {
        containerSize.height / source.height.toFloat()
    }
    val effectiveScale = fitScale * imageScale

    // The border's fixed center is the container's center, which is also where the untransformed
    // (offset=0, scale=1, rotation=0) photo is centered - so undoing the applied offset/rotation
    // directly gives the bitmap-space point now sitting under the border.
    val undone = rotatePoint(
        imageOffset.x / effectiveScale,
        imageOffset.y / effectiveScale,
        -imageRotationDegrees,
    )
    val bitmapCenterX = source.width / 2f - undone.x
    val bitmapCenterY = source.height / 2f - undone.y

    val baseWidthPx = containerSize.width * GUIDE_WIDTH_FRACTION
    val baseHeightPx = baseWidthPx / GUIDE_ASPECT
    val targetWidth = (baseWidthPx / effectiveScale).roundToInt().coerceAtLeast(1)
    val targetHeight = (baseHeightPx / effectiveScale).roundToInt().coerceAtLeast(1)

    val out = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val matrix = Matrix().apply {
        postTranslate(-bitmapCenterX, -bitmapCenterY)
        postRotate(-imageRotationDegrees)
        postTranslate(targetWidth / 2f, targetHeight / 2f)
    }
    canvas.drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
    return out
}

/** Rotates vector ([x], [y]) by [degrees], matching [Matrix.postRotate]'s clockwise-positive screen-space convention. */
private fun rotatePoint(x: Float, y: Float, degrees: Float): Offset {
    val rad = Math.toRadians(degrees.toDouble())
    val cos = kotlin.math.cos(rad).toFloat()
    val sin = kotlin.math.sin(rad).toFloat()
    return Offset(x * cos - y * sin, x * sin + y * cos)
}
