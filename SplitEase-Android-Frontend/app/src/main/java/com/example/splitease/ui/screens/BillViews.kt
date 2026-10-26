package com.example.splitease.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.splitease.R
import com.example.splitease.ui.RequestToasts
import com.example.splitease.viewmodel.AddExpenseUiState
import com.example.splitease.viewmodel.BillScanStatus
import java.io.File

/** Opens the camera or the photo picker and hands back the chosen bill photo's uri. */
class BillPhotoPickers(val takePhoto: () -> Unit, val choosePhoto: () -> Unit)

@Composable
fun rememberBillPhotoPickers(onPicked: (String) -> Unit): BillPhotoPickers {
    val context = LocalContext.current
    // Survives the activity being recreated while the camera app is open
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (saved && uri != null) onPicked(uri)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri.toString())
    }

    return remember(camera, gallery) {
        BillPhotoPickers(
            takePhoto = {
                val uri = newCameraFile(context)
                pendingCameraUri = uri.toString()
                try {
                    camera.launch(uri)
                } catch (e: ActivityNotFoundException) {
                    pendingCameraUri = null
                    RequestToasts.failure("No camera app found")
                }
            },
            choosePhoto = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }
}

private fun newCameraFile(context: Context): Uri {
    val dir = File(context.cacheDir, "bills").apply { mkdirs() }
    val file = File(dir, "bill_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Bill photo controls in the Add Expense dialog. */
@Composable
fun BillAttachmentSection(
    addExpenseUiState: AddExpenseUiState,
    onTakePhoto: () -> Unit,
    onChoosePhoto: () -> Unit,
    onRemove: () -> Unit,
    onUseDetectedAmount: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallPadding))
    ) {
        Text(
            text = "Bill photo (optional)",
            fontSize = 16.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        val billUri = addExpenseUiState.billUri
        if (billUri == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallPadding))) {
                BillButton(text = "Take photo", onClick = onTakePhoto, modifier = Modifier.weight(1f))
                BillButton(text = "Choose photo", onClick = onChoosePhoto, modifier = Modifier.weight(1f))
            }
            return@Column
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.mediumPadding))
        ) {
            AsyncImage(
                model = billUri,
                contentDescription = "Bill photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)))
                    .background(Color(0xFFF1F1F1))
            )
            Column(modifier = Modifier.weight(1f)) {
                when (addExpenseUiState.billScanStatus) {
                    BillScanStatus.READING -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Text(text = "Reading the bill…", color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                    }
                    BillScanStatus.FOUND_AMOUNT -> {
                        Text(text = "Found ₹${addExpenseUiState.detectedAmount} on the bill", color = Color.Black)
                        if (addExpenseUiState.amount != addExpenseUiState.detectedAmount) {
                            TextButton(onClick = onUseDetectedAmount, contentPadding = PaddingValues(0.dp)) {
                                Text(text = "Use this amount")
                            }
                        } else {
                            Text(text = "Check it matches the bill", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                    BillScanStatus.NO_AMOUNT, null -> Text(
                        text = "Couldn't find the total. Enter the amount yourself.",
                        color = Color.Gray
                    )
                }
            }
            TextButton(
                onClick = onRemove,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.cross_96),
                    contentDescription = "Remove bill photo",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun BillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray),
        modifier = modifier
    ) {
        Text(text = text, color = Color.Black)
    }
}

/**
 * The bill photo of an expense for its details page. The payer can attach, replace or remove it;
 * everyone else in the group can only view it.
 */
@Composable
fun BillPhotoCard(
    hasReceipt: Boolean,
    canEdit: Boolean,
    isBusy: Boolean,
    thumbnailUrl: String,
    fullUrl: String,
    cacheVersion: Int,
    onPicked: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Nothing to show to members who can't attach a bill
    if (!hasReceipt && !canEdit) return

    var showFullScreen by remember { mutableStateOf(false) }
    val pickers = rememberBillPhotoPickers(onPicked)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(width = 0.2.dp, color = Color.Gray),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largePadding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallPadding))
        ) {
            Text(text = "Bill", fontSize = 20.sp)
            if (hasReceipt) {
                Text(text = "Tap the photo to see it full size.", color = Color.Gray)
                BillImage(
                    url = thumbnailUrl,
                    cacheVersion = cacheVersion,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.mediumCornerRoundedness)))
                        .background(Color(0xFFF1F1F1))
                        .clickable { showFullScreen = true }
                )
            } else {
                Text(text = "No bill photo yet. Add one so everyone can see it.", color = Color.Gray)
            }
            if (canEdit) {
                if (isBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Text(text = "Updating bill photo…", color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.smallPadding))) {
                        BillButton(text = "Take photo", onClick = pickers.takePhoto, modifier = Modifier.weight(1f))
                        BillButton(text = "Choose photo", onClick = pickers.choosePhoto, modifier = Modifier.weight(1f))
                    }
                    if (hasReceipt) {
                        TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Remove bill photo", color = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showFullScreen) {
        FullScreenBillDialog(url = fullUrl, cacheVersion = cacheVersion, onDismiss = { showFullScreen = false })
    }
}

/** A bill photo from the server; [cacheVersion] changes when the photo is replaced. */
@Composable
fun BillImage(
    url: String,
    cacheVersion: Int,
    contentScale: ContentScale,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AsyncImage(
        model = remember(url, cacheVersion) {
            ImageRequest.Builder(context)
                .data(url)
                .memoryCacheKey("$url#$cacheVersion")
                .build()
        },
        contentDescription = "Bill photo",
        contentScale = contentScale,
        modifier = modifier
    )
}

/** Full screen bill photo with pinch to zoom. */
@Composable
fun FullScreenBillDialog(url: String, cacheVersion: Int, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
                }
        ) {
            BillImage(
                url = url,
                cacheVersion = cacheVersion,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.cross_96),
                    contentDescription = "Close",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
