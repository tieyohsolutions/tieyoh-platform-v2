package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandNavyLight2
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal

// Step right after "Tell us a bit about yourself" in profile setup.
// Matches the "Tieyoh is better with friends!" reference image: fonts,
// colors, and sizes follow the same tokens as the rest of the redesign.
//
// The four-avatar illustration is the actual reference artwork (its white
// background removed so it sits directly on the screen), not a Compose
// approximation -- see find_friends_avatars.png in res/drawable.
// Both buttons currently just advance to the next step: actual
// contacts-based friend finding (permission request + matching against the
// backend's user search) isn't built yet, so "Find My Friends" doesn't yet
// do anything different from "Maybe Later" under the hood -- kept as two
// separate callbacks so that can be wired up later without reshaping this
// screen.
@Composable
fun FindFriendsScreen(
    onBack: () -> Unit,
    onFindFriends: () -> Unit,
    onMaybeLater: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Same decorative wave as the other post-signup screens, pushed a
        // little further down than usual so it stays clear of the buttons.
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
                    text = "Profile Setup",
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

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Tieyoh is better with friends!",
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = TieyohBrandDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Find people you already know, connect with friends, and make new memories together.",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = TieyohBrandNavyLight2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.find_friends_avatars),
                contentDescription = "Connect with friends",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(270f / 186f)
            )

            Spacer(Modifier.weight(1f))

            val pillShape = RoundedCornerShape(26.dp)
            val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(brandGradient)
                    .clickable { onFindFriends() },
                contentAlignment = Alignment.Center
            ) {
                Text("Find My Friends", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(Color.White)
                    .border(width = 1.5.dp, color = TieyohBrandBlue, shape = pillShape)
                    .clickable { onMaybeLater() },
                contentAlignment = Alignment.Center
            ) {
                Text("Maybe Later", color = TieyohBrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
