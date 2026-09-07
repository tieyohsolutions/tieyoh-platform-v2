package com.tieyoh.mobile

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandNavyLight2
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy

// The one payment-method screen, reused in two places: right after merchant
// selection during signup (SessionState.NeedsPaymentMethod in
// MainActivity), and from the Profile tab's "Manage Payment Methods" link
// (TieyohScreen.PAYMENT_METHODS) -- both already point at this same
// composable, so no other call site needed to change for this redesign.
// Matches the "Add your preferred payment method" reference image: fonts,
// colors, sizes, and the central wallet illustration follow the same
// tokens as the rest of the redesign.
//
// doneButtonLabel distinguishes the two contexts: left null (onboarding)
// this shows "Continue" plus a secondary "Maybe Later" skip, matching the
// reference; passed a value (Profile passes "Done") this shows just that
// one button, since "maybe later" doesn't make sense once you're already
// past onboarding and back in Settings.
//
// onBack has no clean prior step to return to from the top-level
// onboarding session state this is reached from (same situation as
// MerchantSelectionScreen), so it defaults to onDone -- a flagged judgment
// call, not a confirmed decision. From the Profile entry point it's
// correct as-is: back and done both land you on Profile.
//
// Real bank/card linking (Plaid/Stripe or similar) is NOT built -- per
// instruction, these three rows are a placeholder for now. Tapping one
// still opens the existing on-device-only capture form (PaymentMethodStore
// / PaymentMethodFormScreen, built earlier -- see PaymentMethodStore.kt's
// own header comment): it saves nothing more than a masked last-4 locally,
// never a real account/card number, and nothing is sent to a processor.
// That old form also does not support real *editing* of a saved entry --
// tapping a row that already has something saved reopens a blank form to
// add another, it doesn't let you change the existing one. That's a
// genuine gap, not yet built, matching what was flagged when this was
// requested.
@Composable
fun PaymentMethodSetupScreen(
    onDone: () -> Unit,
    onBack: () -> Unit = onDone,
    doneButtonLabel: String? = null
) {
    val context = LocalContext.current
    var savedMethods by remember { mutableStateOf(PaymentMethodStore.load(context)) }
    var activeForm by remember { mutableStateOf<PaymentMethodType?>(null) }

    val currentForm = activeForm
    if (currentForm != null) {
        PaymentMethodFormScreen(
            type = currentForm,
            onCancel = { activeForm = null },
            onSaved = { method ->
                PaymentMethodStore.add(context, method)
                savedMethods = PaymentMethodStore.load(context)
                activeForm = null
            }
        )
        return
    }

    val isOnboarding = doneButtonLabel == null
    val pillShape = RoundedCornerShape(26.dp)
    val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))
    // Sampled from the reference image's small purple card icon -- not yet
    // one of the shared brand tokens in ui/theme/Color.kt, so it's kept
    // local to this screen rather than added there for a single use.
    val creditCardPurple = Color(0xFF6659D6)

    Box(modifier = Modifier.fillMaxSize()) {

        // Same decorative wave as the other redesigned screens.
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Only the onboarding entry point (no Scaffold/top bar around
            // it -- see the header comment) needs to reserve the status
            // bar/notch itself. The Profile > Manage Payment Methods entry
            // point is already inside TieyohApp's Scaffold, which reserves
            // that space via its own top bar -- adding it again here is
            // what pushed this screen's arrow noticeably lower than Edit
            // Profile/Privacy's, which never had this extra padding.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let {
                        if (isOnboarding) {
                            it.windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                                .padding(top = 12.dp)
                        } else {
                            it
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = TieyohBrandNavy
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Image(
                painter = painterResource(id = R.drawable.tieyoh_logo),
                contentDescription = "Tieyoh",
                modifier = Modifier.fillMaxWidth(0.5f)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Add your preferred\npayment method",
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = TieyohBrandDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Choose how you'd like to pay for purchases, subscriptions, or send and receive money on Tieyoh.",
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = TieyohBrandNavyLight2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // The actual reference artwork (wallet + bank/card icons),
            // background removed -- not a Compose approximation.
            Image(
                painter = painterResource(id = R.drawable.payment_wallet_icon),
                contentDescription = "Add a payment method",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .aspectRatio(287f / 145f)
            )

            Spacer(Modifier.height(18.dp))

            PaymentOptionRow(
                icon = Icons.Filled.AccountBalance,
                iconColor = TieyohBrandBlue,
                title = "Bank Account",
                subtitle = "Link your bank account (routing & account number)",
                savedLabel = savedMethods.firstOrNull { it.type == PaymentMethodType.BANK_ACCOUNT }
                    ?.let { "Added •••• ${it.last4}" },
                onClick = { activeForm = PaymentMethodType.BANK_ACCOUNT }
            )

            Spacer(Modifier.height(10.dp))

            PaymentOptionRow(
                icon = Icons.Filled.CreditCard,
                iconColor = TieyohBrandBlue,
                title = "Debit Card",
                subtitle = "Add your debit card for fast, secure payments",
                savedLabel = savedMethods.firstOrNull { it.type == PaymentMethodType.DEBIT_CARD }
                    ?.let { "Added •••• ${it.last4}" },
                onClick = { activeForm = PaymentMethodType.DEBIT_CARD }
            )

            Spacer(Modifier.height(10.dp))

            PaymentOptionRow(
                icon = Icons.Filled.CreditCard,
                iconColor = creditCardPurple,
                title = "Credit Card",
                subtitle = "Add your credit card for flexible payments",
                savedLabel = savedMethods.firstOrNull { it.type == PaymentMethodType.CREDIT_CARD }
                    ?.let { "Added •••• ${it.last4}" },
                onClick = { activeForm = PaymentMethodType.CREDIT_CARD }
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(brandGradient)
                    .clickable { onDone() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    doneButtonLabel ?: "Continue",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            if (isOnboarding) {
                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(pillShape)
                        .background(Color.White)
                        .border(width = 1.5.dp, color = TieyohBrandBlue, shape = pillShape)
                        .clickable { onDone() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Maybe Later", color = TieyohBrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaymentOptionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    savedLabel: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(width = 1.dp, color = TieyohMuted.copy(alpha = 0.18f), shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TieyohBrandNavy)
            Spacer(Modifier.height(2.dp))
            Text(
                text = savedLabel ?: subtitle,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                color = if (savedLabel != null) TieyohBrandBlue else TieyohBrandNavyLight2
            )
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TieyohMuted
        )
    }
}

@Composable
private fun PaymentMethodFormScreen(
    type: PaymentMethodType,
    onCancel: () -> Unit,
    onSaved: (SavedPaymentMethod) -> Unit
) {
    val context = LocalContext.current
    val isBank = type == PaymentMethodType.BANK_ACCOUNT

    var name by remember { mutableStateOf("") }
    var primaryNumber by remember { mutableStateOf("") }
    var secondary by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = when (type) {
                PaymentMethodType.BANK_ACCOUNT -> "Add a bank account"
                PaymentMethodType.DEBIT_CARD -> "Add a debit card"
                PaymentMethodType.CREDIT_CARD -> "Add a credit card"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(if (isBank) "Account holder name" else "Name on card") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = primaryNumber,
            onValueChange = { primaryNumber = it },
            label = { Text(if (isBank) "Account number" else "Card number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = secondary,
            onValueChange = { secondary = it },
            label = { Text(if (isBank) "Routing number" else "Expiry (MM/YY)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isBank) KeyboardType.Number else KeyboardType.Text
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (!isBank) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = cvv,
                onValueChange = { cvv = it },
                label = { Text("CVV") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "For this preview, only the last 4 digits are stored on this device -- nothing is sent to a payment processor yet.",
            color = TieyohMuted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Row {
            TieyohSecondaryButton(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            TieyohPrimaryButton(
                text = "Save",
                onClick = {
                    val valid = name.isNotBlank() && primaryNumber.length >= 4
                    if (!valid) {
                        Toast.makeText(context, "Please fill in the details", Toast.LENGTH_SHORT).show()
                        return@TieyohPrimaryButton
                    }
                    val label = when (type) {
                        PaymentMethodType.BANK_ACCOUNT -> "Bank account"
                        PaymentMethodType.DEBIT_CARD -> "Debit card"
                        PaymentMethodType.CREDIT_CARD -> "Credit card"
                    }
                    onSaved(
                        SavedPaymentMethod(
                            type = type,
                            label = label,
                            last4 = primaryNumber.takeLast(4)
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
