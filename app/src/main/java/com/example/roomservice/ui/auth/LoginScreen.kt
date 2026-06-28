package com.example.roomservice.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.roomservice.data.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

import androidx.compose.ui.ExperimentalComposeUiApi

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofill(
    autofillTypes: List<AutofillType>,
    onFill: (String) -> Unit
) = composed {
    val autofill = LocalAutofill.current
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)
    LocalAutofillTree.current += autofillNode

    this.onGloballyPositioned {
        autofillNode.boundingBox = it.boundsInWindow()
    }.onFocusChanged { focusState ->
        autofill?.run {
            if (focusState.isFocused) {
                requestAutofillForNode(autofillNode)
            } else {
                cancelAutofillForNode(autofillNode)
            }
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String, String, String?) -> Unit,
    onSignUpClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.roomservice.R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember { 
        val activity = context.findActivity()
        if (activity != null) GoogleSignIn.getClient(activity, gso) else null 
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        val user = auth.currentUser
                        onLoginSuccess(user?.email ?: "", user?.displayName ?: "Google User", user?.photoUrl?.toString())
                    } else {
                        Toast.makeText(context, "Firebase Auth failed", Toast.LENGTH_SHORT).show()
                    }
                    isProcessing = false
                }
            } else {
                // Fallback or error
                if (account != null) {
                    onLoginSuccess(account.email ?: "", account.displayName ?: "Google User", account.photoUrl?.toString())
                }
                isProcessing = false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
            isProcessing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(Modifier.height(60.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Admin Login",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = "Please enter your admin email and password or sign in with Google",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
            
            Spacer(Modifier.height(32.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Admin Email", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        autoCorrect = false
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .autofill(
                            autofillTypes = listOf(AutofillType.EmailAddress, AutofillType.Username),
                            onFill = { email = it }
                        ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrect = false
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .autofill(
                            autofillTypes = listOf(AutofillType.Password),
                            onFill = { password = it }
                        ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, 
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (email.isNotBlank()) {
                                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, task.exception?.message ?: "Error", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(context, "Please enter your email to reset password", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildAnnotatedString {
                    append("By logging in, you agree to our ")
                    withStyle(style = SpanStyle(color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)) {
                        append("Terms and conditions")
                    }
                    append(" and ")
                    withStyle(style = SpanStyle(color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)) {
                        append("Privacy Policy")
                    }
                },
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    if (email.isNotBlank() && password.isNotBlank()) {
                        isProcessing = true
                        AuthRepository.loginAdmin(
                            email = email,
                            pass = password,
                            onSuccess = { data ->
                                isProcessing = false
                                onLoginSuccess(
                                    data["email"]?.toString() ?: "",
                                    data["name"]?.toString() ?: "Admin",
                                    null
                                )
                            },
                            onFailure = { error ->
                                isProcessing = false
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isProcessing && (email.isNotBlank() && password.isNotBlank()),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("CONTINUE", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}
