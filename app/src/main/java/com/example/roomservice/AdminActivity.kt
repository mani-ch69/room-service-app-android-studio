package com.example.roomservice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.roomservice.ui.auth.*
import com.example.roomservice.ui.splash.*
import com.example.roomservice.ui.waiter.*
import com.example.roomservice.ui.profile.EditProfileScreen
import com.example.roomservice.ui.settings.*
import com.example.roomservice.ui.theme.RoomServiceTheme
import com.example.roomservice.util.SecurityManager

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoomServiceTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AdminNavigation()
                }
            }
        }
    }
}

@Composable
fun AdminNavigation() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val securityManager = remember { SecurityManager(context) }
    
    val initialData = securityManager.getUserData()
    val navController = rememberNavController()

    // Initialize session if already logged in
    LaunchedEffect(Unit) {
        if (securityManager.isLoggedIn()) {
            val hId = securityManager.getHotelId()
            if (hId != null) {
                com.example.roomservice.data.HotelSession.setHotelId(hId)
            } else {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    com.example.roomservice.data.HotelSession.setHotelId(uid)
                }
            }
        }
    }
    
    var loggedId by remember { 
        val email = initialData["email"] ?: ""
        val phone = initialData["phone"] ?: ""
        mutableStateOf(if (email.isNotEmpty()) email else phone)
    }
    var loggedName by remember { mutableStateOf(initialData["name"] ?: "Admin Profile") }
    var loggedPhoto by remember { mutableStateOf(initialData["photo"]) }
    
    var tempPhone by remember { mutableStateOf("") }
    var tempVerificationId by remember { mutableStateOf("") }
    
    var adminName by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }

    // Helper to mask email (e.g. admin@hotel.com -> a***n@hotel.com)
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val name = parts[0]
        val domain = parts[1]
        if (name.length <= 2) return email
        return "${name[0]}***${name.last()}@$domain"
    }

    NavHost(
        navController = navController, 
        startDestination = "splash",
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
    ) {
        composable("splash") {
            SplashScreen(
                onAnimationFinished = {
                    val next = when {
                        !securityManager.isLoggedIn() -> "login"
                        !securityManager.hasSeenSecuritySetup() -> "security_setup"
                        securityManager.isLockEnabled() -> "unlock"
                        else -> "admin_menu"
                    }
                    navController.navigate(next) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("onboarding") {
            OnboardingScreen(
                onLoginClick = { navController.navigate("login") },
                onStaffLoginClick = { /* Disabled */ }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = { id, name, photo ->
                    loggedId = id
                    loggedName = name
                    loggedPhoto = photo
                    
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    securityManager.setLoggedIn(true, id, name, photo, null, "ADMIN", uid)
                    com.example.roomservice.data.HotelSession.setHotelId(uid)

                    navController.navigate("permissions_request") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onSignUpClick = { navController.navigate("signup") }
            )
        }
        composable("otp_verification") {
            OtpVerificationScreen(
                mobileNumber = tempPhone,
                verificationId = tempVerificationId,
                onBackClick = { navController.popBackStack() },
                onVerifySuccess = { phone ->
                    com.example.roomservice.data.AuthRepository.syncUserDataByPhone(
                        phone = phone,
                        onSuccess = { data ->
                            val name = data["name"]?.toString() ?: "User"
                            val email = data["email"]?.toString() ?: ""
                            val hotelId = data["hotelId"]?.toString() ?: (data["id"]?.toString() ?: "")
                            
                            loggedId = if (email.isNotEmpty()) email else phone
                            loggedName = name
                            
                            securityManager.setLoggedIn(true, email, name, null, phone, "ADMIN", hotelId)
                            com.example.roomservice.data.HotelSession.setHotelId(hotelId)
                            
                            navController.navigate("permissions_request") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onFailure = { error ->
                            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
        }
        composable("permissions_request") {
            PermissionsRequestScreen(
                onPermissionsGranted = {
                    navController.navigate("security_setup") {
                        popUpTo("permissions_request") { inclusive = true }
                    }
                }
            )
        }
        composable("signup") {
            SignUpScreen(
                onSignUpSuccess = { name, email, phone, pass ->
                    adminName = name
                    emailAddress = email
                    mobileNumber = phone
                    password = pass

                    // Firebase Phone Auth Trigger
                    val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(com.google.firebase.auth.FirebaseAuth.getInstance())
                        .setPhoneNumber("+91$phone")
                        .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                        .setActivity(context.findActivity()!!)
                        .setCallbacks(object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                                // AUTO CAPTURED OTP
                                com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            com.example.roomservice.data.AuthRepository.signUpAdmin(
                                                name = name,
                                                email = email,
                                                phone = phone,
                                                pass = pass,
                                                onSuccess = {
                                                    loggedId = email
                                                    loggedName = name
                                                    securityManager.setLoggedIn(true, email, name, null, phone, "ADMIN")
                                                    navController.navigate("permissions_request") {
                                                        popUpTo("signup") { inclusive = true }
                                                    }
                                                },
                                                onFailure = { /* Handle error */ }
                                            )
                                        }
                                    }
                            }

                            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                                android.widget.Toast.makeText(context, e.message, android.widget.Toast.LENGTH_LONG).show()
                            }

                            override fun onCodeSent(id: String, token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken) {
                                tempPhone = phone
                                tempVerificationId = id
                                navController.navigate("otp_verification")
                            }
                        })
                        .build()
                    com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable("security_setup") {
            SecuritySetupScreen(
                onComplete = { pin, bio ->
                    securityManager.setAppLock(pin, bio)
                    navController.navigate("admin_menu") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("unlock") {
            UnlockScreen(
                userName = loggedName,
                userEmail = maskEmail(loggedId),
                savedPin = securityManager.getPin(),
                useBiometric = securityManager.useBiometric(),
                onUnlockSuccess = {
                    navController.navigate("admin_menu") {
                        popUpTo("unlock") { inclusive = true }
                    }
                },
                onLogout = {
                    securityManager.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("admin_menu") {
            AdminMenuScreen(
                staffIdLabel = loggedId,
                staffName = loggedName,
                staffPhoto = loggedPhoto,
                onProfileClick = { navController.navigate("profile") },
                onLogoutClick = {
                    securityManager.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onGeneralSettingsClick = { navController.navigate("general_settings") },
                onSecuritySettingsClick = { navController.navigate("security_settings") }
            )
        }
        composable("business_details") {
            BusinessDetailsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("general_settings") {
            GeneralSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onEditProfileClick = { navController.navigate("profile") }
            )
        }
        composable("security_settings") {
            SecuritySettingsScreen(
                onBackClick = { navController.popBackStack() },
                onAppLockClick = { navController.navigate("app_lock_settings") }
            )
        }
        composable("app_lock_settings") {
            AppLockSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("profile") {
            EditProfileScreen(
                initialName = loggedName,
                initialEmail = loggedId,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { 
                    navController.popBackStack()
                }
            )
        }
        composable("room_management") {
            RoomManagementScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
