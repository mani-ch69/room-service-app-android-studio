package com.example.roomservice

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.roomservice.ui.theme.RoomServiceTheme
import com.example.roomservice.ui.auth.LoginScreen
import com.example.roomservice.ui.auth.OtpVerificationScreen
import com.example.roomservice.ui.auth.SignUpScreen
import com.example.roomservice.ui.auth.StaffQRScannerScreen
import com.example.roomservice.ui.auth.SecuritySetupScreen
import com.example.roomservice.ui.auth.PermissionsRequestScreen
import com.example.roomservice.ui.auth.StaffInvitationScreen
import com.example.roomservice.ui.auth.UnlockScreen
import com.example.roomservice.ui.profile.EditProfileScreen
import com.example.roomservice.util.SecurityManager
import com.example.roomservice.ui.settings.AppLockSettingsScreen
import com.example.roomservice.ui.settings.BusinessDetailsScreen
import com.example.roomservice.ui.settings.GeneralSettingsScreen
import com.example.roomservice.ui.settings.SecuritySettingsScreen
import com.example.roomservice.ui.settings.SettingsScreen
import com.example.roomservice.ui.waiter.AdminChatDetailScreen
import com.example.roomservice.ui.waiter.AdminChatListScreen
import com.example.roomservice.ui.waiter.AdminMenuScreen
import com.example.roomservice.ui.waiter.StaffDashboardScreen
import com.example.roomservice.ui.waiter.AddStaffScreen
import com.example.roomservice.ui.waiter.RoomManagementScreen
import com.example.roomservice.ui.waiter.WaiterDashboardScreen
import com.example.roomservice.ui.splash.SplashScreen
import com.example.roomservice.ui.splash.OnboardingScreen
import com.example.roomservice.util.NotificationHelper

import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

import androidx.fragment.app.FragmentActivity

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

class AdminActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        
        setContent {
            RoomServiceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
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
                // Fallback for Admin if hotelId not in prefs yet
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
    var loggedRole by remember { mutableStateOf(initialData["role"] ?: "ADMIN") }
    
    var tempPhone by remember { mutableStateOf("") }
    var tempVerificationId by remember { mutableStateOf("") }

    // Helper to mask email (e.g. admin@hotel.com -> a***n@hotel.com)
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val name = parts[0]
        val domain = parts[1]
        if (name.length <= 2) return email
        return "${name[0]}***${name.last()}@$domain"
    }

    val startDest = "splash"

    NavHost(
        navController = navController, 
        startDestination = startDest,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) }
    ) {
        composable("splash") {
            SplashScreen(
                onAnimationFinished = {
                    val next = when {
                        !securityManager.isLoggedIn() -> "onboarding"
                        !securityManager.hasSeenSecuritySetup() -> "security_setup"
                        securityManager.isLockEnabled() -> "unlock"
                        else -> {
                            // Already logged in and lock explicitly disabled
                            if (loggedRole == "ADMIN") "admin_menu" else "staff_dashboard"
                        }
                    }
                    // Handle pre-navigation to specific dashboard if already unlocked
                    val dest = if (next == "unlock") {
                        // We will let unlock screen decide where to go next based on role
                        "unlock"
                    } else {
                        next
                    }

                    navController.navigate(dest) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("onboarding") {
            OnboardingScreen(
                onLoginClick = { navController.navigate("login") },
                onStaffLoginClick = { navController.navigate("staff_login") }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = { id, name, photo ->
                    loggedId = id
                    loggedName = name
                    loggedPhoto = photo
                    loggedRole = "ADMIN"
                    
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    securityManager.setLoggedIn(true, id, name, photo, null, "ADMIN", uid)
                    com.example.roomservice.data.HotelSession.setHotelId(uid)

                    navController.navigate("permissions_request") {
                        popUpTo("login") { inclusive = true }
                    }
                }
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
                            val role = data["role"]?.toString() ?: "ADMIN"
                            val name = data["name"]?.toString() ?: "User"
                            val email = data["email"]?.toString() ?: ""
                            val hotelId = data["hotelId"]?.toString() ?: (data["id"]?.toString() ?: "")
                            
                            loggedId = if (email.isNotEmpty()) email else phone
                            loggedName = name
                            loggedRole = role
                            
                            securityManager.setLoggedIn(true, email, name, null, phone, role, hotelId)
                            com.example.roomservice.data.HotelSession.setHotelId(hotelId)
                            
                            if (role == "ADMIN") {
                                navController.navigate("permissions_request") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                navController.navigate("staff_invitation") {
                                    popUpTo("login") { inclusive = true }
                                }
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
                    // Logic to check if user is staff
                    val phone = initialData["phone"] ?: ""
                    // In a real app, fetch from repository
                    val isStaff = false // placeholder
                    if (isStaff) {
                        navController.navigate("staff_invitation")
                    } else {
                        navController.navigate("security_setup") {
                            popUpTo("permissions_request") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("staff_invitation") {
            val businessDetails by com.example.roomservice.data.BusinessDetailsRepository.details.collectAsState()
            StaffInvitationScreen(
                hotelName = businessDetails.hotelName, 
                assignedRole = loggedRole,
                hotelAddress = businessDetails.address,
                onAccept = {
                    navController.navigate("staff_dashboard") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDecline = {
                    securityManager.logout()
                    navController.navigate("onboarding") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("staff_login") {
            StaffQRScannerScreen(
                onBackClick = { navController.popBackStack() },
                onQRScanned = { qrUrl ->
                    android.util.Log.d("StaffLogin", "Scanned QR: $qrUrl")
                    try {
                        val uri = android.net.Uri.parse(qrUrl)
                        val sId = uri.getQueryParameter("id") // The UUID
                        val hId = uri.getQueryParameter("hotelId")
                        val code = uri.getQueryParameter("code")
                        val name = uri.getQueryParameter("name") ?: "Staff Member"
                        
                        if (hId != null && sId != null) {
                            android.widget.Toast.makeText(context, "Welcome, $name", android.widget.Toast.LENGTH_SHORT).show()
                            
                            com.example.roomservice.data.HotelSession.setHotelId(hId)
                            val phone = uri.getQueryParameter("phone") ?: ""
                            val role = uri.getQueryParameter("role") ?: "STAFF"
                            
                            loggedId = sId // Use UUID for correct filtering
                            loggedName = name
                            loggedRole = role
                            securityManager.setLoggedIn(true, sId, name, null, phone, role, hId)
                            
                            navController.navigate("staff_dashboard") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        } else if (hId != null && code != null) {
                            // Fallback for old QR if any, though ID is preferred
                            android.widget.Toast.makeText(context, "Old QR detected, updating...", android.widget.Toast.LENGTH_SHORT).show()
                            com.example.roomservice.data.HotelSession.setHotelId(hId)
                            loggedId = code
                            loggedName = name
                            loggedRole = "STAFF"
                            securityManager.setLoggedIn(true, code, name, null, "", "STAFF", hId)
                            navController.navigate("staff_dashboard") { popUpTo("onboarding") { inclusive = true } }
                        } else {
                            android.widget.Toast.makeText(context, "Invalid QR: Missing ID or Code", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Scanning error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        android.util.Log.e("StaffLogin", "Error parsing QR", e)
                    }
                }
            )
        }
        composable("staff_dashboard") {
            StaffDashboardScreen(
                staffId = loggedId,
                staffName = loggedName,
                onLogout = {
                    securityManager.logout()
                    navController.navigate("onboarding") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("signup") {
            SignUpScreen(
                onSignUpSuccess = { name, email, phone, pass ->
                    com.example.roomservice.data.AuthRepository.signUpAdmin(
                        name = name,
                        email = email,
                        phone = phone,
                        pass = pass,
                        onSuccess = {
                            loggedId = email
                            loggedName = name
                            loggedPhoto = null
                            securityManager.setLoggedIn(true, email, name, null, phone, "ADMIN")
                            navController.navigate("permissions_request") {
                                popUpTo("signup") { inclusive = true }
                            }
                        },
                        onFailure = { error ->
                            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable("security_setup") {
            SecuritySetupScreen(
                onComplete = { pin, bio ->
                    securityManager.setAppLock(pin, bio)
                    val next = if (loggedRole == "ADMIN") "admin_menu" else "staff_dashboard"
                    navController.navigate(next) {
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
                    val next = if (loggedRole == "ADMIN") "admin_menu" else "staff_dashboard"
                    navController.navigate(next) {
                        popUpTo("unlock") { inclusive = true }
                    }
                },
                onLogout = {
                    securityManager.logout()
                    navController.navigate("onboarding") {
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
                onRoomManagementClick = { navController.navigate("room_management") },
                onWaiterDashboardClick = { navController.navigate("waiter_dashboard") },
                onChatWithRoomClick = { room -> navController.navigate("admin_chat_detail/$room") },
                onProfileClick = { navController.navigate("profile") },
                onSettingsClick = { navController.navigate("settings") },
                onLogoutClick = {
                    securityManager.logout()
                    navController.navigate("onboarding") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("add_staff") {
            AddStaffScreen(onBackClick = { navController.popBackStack() })
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
                onBackClick = { navController.popBackStack() },
                onPaymentsClick = { navController.navigate("payments") },
                onManageStaffClick = { navController.navigate("add_staff") }, // Or a list if available
                onMessagingPreferenceClick = { /* Add route if needed */ }
            )
        }
        composable("payments") {
            com.example.roomservice.ui.waiter.PaymentsManagementScreen()
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
        composable("waiter_dashboard") {
            WaiterDashboardScreen(
                onChatClick = { navController.navigate("admin_chat_list") }
            )
        }
        composable("admin_chat_list") {
            AdminChatListScreen(
                onChatClick = { room -> navController.navigate("admin_chat_detail/$room") },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("admin_chat_detail/{roomNumber}") { backStackEntry ->
            val room = backStackEntry.arguments?.getString("roomNumber") ?: ""
            AdminChatDetailScreen(
                roomNumber = room,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}