package com.tieyoh.mobile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandNavyLight2
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal

private data class PermissionPoint(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)

private val permissionPoints = listOf(
    PermissionPoint(
        Icons.Filled.Group,
        "Find Friends",
        "See which of your contacts are already on Tieyoh."
    ),
    PermissionPoint(
        Icons.Filled.Favorite,
        "Build Your Community",
        "Connect with people you know and make new connections."
    ),
    PermissionPoint(
        Icons.Filled.Shield,
        "Your Privacy Matters",
        "We never store your contacts without your permission, and you can remove access anytime in your settings."
    )
)

// Step right after "Tieyoh is better with friends!" (Find My Friends
// leads here) -- matches the "Allow Tieyoh to access your contacts?"
// reference image: fonts, colors, and sizes follow the same tokens as the
// rest of the redesign. This is a deliberate in-app pre-prompt shown
// BEFORE the real OS permission dialog -- Allow Access is the only button
// that actually triggers READ_CONTACTS; Don't Allow and Maybe Later both
// skip past contacts entirely without ever showing the system dialog
// (asking the OS a second time after the person already said no in-app
// would just burn the one-shot "don't ask again" system prompt for
// nothing). onAllowed only fires once the OS has actually granted the
// permission -- if the person backs out of the system dialog instead,
// that's treated the same as Don't Allow/Maybe Later (onSkip), per the
// same "skip contacts altogether" behavior.
@Composable
fun ContactsPermissionScreen(
    onBack: () -> Unit,
    onAllowed: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onAllowed() else onSkip()
    }

    fun requestAccess() {
        val alreadyGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            onAllowed()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Same decorative wave as the other post-signup screens.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1.4f)
                .height(120.dp)
                .offset(y = 70.dp)
                .clip(RoundedCornerShape(topStart = 600.dp, topEnd = 600.dp))
                .background(TieyohBrandTeal.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1.1f)
                .height(90.dp)
                .offset(y = 55.dp)
                .clip(RoundedCornerShape(topStart = 600.dp, topEnd = 600.dp))
                .background(TieyohBrandBlue.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = TieyohNavy
                    )
                }
                Text(
                    text = "Find Friends",
                    color = TieyohNavy,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            Image(
                painter = painterResource(id = R.drawable.tieyoh_logo),
                contentDescription = "Tieyoh",
                modifier = Modifier.fillMaxWidth(0.5f)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Allow Tieyoh to access\nyour contacts?",
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = TieyohBrandDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "This helps us find your friends, suggest people you may know, and make it easier to connect on Tieyoh.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = TieyohBrandNavyLight2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // The actual reference artwork (contact book + four contact
            // circles), background removed -- not a Compose approximation.
            Image(
                painter = painterResource(id = R.drawable.contacts_permission_icon),
                contentDescription = "Access your contacts",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(302f / 118f)
            )

            Spacer(Modifier.height(20.dp))

            permissionPoints.forEachIndexed { index, point ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(TieyohBrandBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = point.icon,
                            contentDescription = null,
                            tint = TieyohBrandBlue,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = point.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TieyohBrandNavy
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = point.description,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = TieyohBrandNavyLight2
                        )
                    }
                }
                if (index != permissionPoints.lastIndex) {
                    Spacer(Modifier.height(14.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            val pillShape = RoundedCornerShape(26.dp)
            val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(brandGradient)
                    .clickable { requestAccess() },
                contentAlignment = Alignment.Center
            ) {
                Text("Allow Access", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(Color.White)
                    .border(width = 1.5.dp, color = TieyohBrandBlue, shape = pillShape)
                    .clickable { onSkip() },
                contentAlignment = Alignment.Center
            ) {
                Text("Don't Allow", color = TieyohBrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Maybe Later",
                color = TieyohBrandBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onSkip() }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
