package com.tieyoh.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun LoginScreen(
    onAuthenticated: (AuthResponse) -> Unit
) {
    var isSignupMode by remember { mutableStateOf(false) }

    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun submit() {
        errorMessage = null

        if (email.isBlank() || password.isBlank() || (isSignupMode && displayName.isBlank())) {
            errorMessage = "Please fill in all fields."
            return
        }

        coroutineScope.launch {
            isLoading = true
            try {
                val response = if (isSignupMode) {
                    RetrofitClient.authApi.signup(
                        SignupRequest(
                            email = email.trim(),
                            password = password,
                            displayName = displayName.trim()
                        )
                    )
                } else {
                    RetrofitClient.authApi.login(
                        LoginRequest(
                            email = email.trim(),
                            password = password
                        )
                    )
                }
                onAuthenticated(response)
            } catch (e: HttpException) {
                errorMessage = when (e.code()) {
                    401 -> "Incorrect email or password."
                    409 -> "An account with this email already exists — try logging in instead."
                    403 -> "This account isn't active yet."
                    else -> "Something went wrong (code ${e.code()}). Please try again."
                }
            } catch (e: Exception) {
                errorMessage = "Couldn't reach the server. Is tieyoh-api running?"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TieyohNavy)) { append("tie") }
                withStyle(SpanStyle(color = TieyohAccent)) { append("yoh") }
            },
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (isSignupMode) "Create your account" else "Welcome back",
            fontSize = 16.sp,
            color = TieyohMuted
        )

        Spacer(Modifier.height(36.dp))

        if (isSignupMode) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { submit() },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = TieyohAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White)
            } else {
                Text(if (isSignupMode) "Create account" else "Log in", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = if (isSignupMode) "Already have an account? Log in" else "New to Tieyoh? Create an account",
            color = TieyohAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable {
                    isSignupMode = !isSignupMode
                    errorMessage = null
                }
                .padding(bottom = 32.dp)
        )
    }
}
