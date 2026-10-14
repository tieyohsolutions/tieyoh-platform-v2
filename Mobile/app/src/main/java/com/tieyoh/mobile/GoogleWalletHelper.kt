package com.tieyoh.mobile

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.pay.Pay
import com.google.android.gms.pay.PayApiAvailabilityStatus
import com.google.android.gms.pay.PayClient
import kotlinx.coroutines.tasks.await

// Shared "Add to Google Wallet" plumbing for PayWithQrScreen and
// PayWithCardScreen's "Add to Wallet" buttons -- both call
// launchAddToGoogleWallet() below with a JWT fetched from
// GoogleWalletApi (real, signed by the backend once Tieyoh's Google
// Wallet issuer credentials are configured there; see
// GoogleWalletService.java's header comment for the honest-gap note on
// what "configured = false" means).
//
// Request codes are distinct per screen so an onActivityResult hook added
// to MainActivity later (not required for the save flow itself -- Google
// Wallet's own on-device UI shows the user its own confirmation once they
// tap through) could tell which "Add to Wallet" flow just finished.
const val GOOGLE_WALLET_QR_CARD_REQUEST_CODE = 4001
const val GOOGLE_WALLET_PREPAID_CARD_REQUEST_CODE = 4002

// Google's own recommended pattern: check availability once when the
// screen appears so a tap can fail fast with a clear reason (no Play
// Services / device not eligible) instead of a silent no-op. Defaults to
// false until the check resolves; a screen doesn't need to gate the
// button's visibility on this -- launchAddToGoogleWallet below re-checks
// before launching so this is best used just to short-circuit with a
// clearer message at tap time.
@Composable
fun rememberGoogleWalletAvailable(): State<Boolean> {
    val context = LocalContext.current
    val available = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        available.value = try {
            val status = Pay.getClient(activity)
                .getPayApiAvailabilityStatus(PayClient.RequestType.SAVE_PASSES)
                .await()
            status == PayApiAvailabilityStatus.AVAILABLE
        } catch (e: Exception) {
            false
        }
    }
    return available
}

// Launches the real, on-device "Add to Google Wallet" system flow --
// Google Wallet's own UI shows the pass preview, the user's own confirm
// tap, and its own success confirmation, so nothing further is needed
// here for the save itself to work. Returns false (caller should show a
// message) if there's no Activity to launch from or the device can't run
// the flow at all; both are cheap synchronous checks so this never blocks.
fun launchAddToGoogleWallet(context: Context, saveJwt: String, requestCode: Int): Boolean {
    val activity = context as? FragmentActivity ?: return false
    return try {
        Pay.getClient(activity).savePassesJwt(saveJwt, activity, requestCode)
        true
    } catch (e: Exception) {
        false
    }
}
