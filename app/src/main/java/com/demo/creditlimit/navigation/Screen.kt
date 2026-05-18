package com.demo.creditlimit.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Login : Screen("login")
    object KycBasicInfo : Screen("kyc_basic_info")
    object KycEmergencyContact : Screen("kyc_emergency_contact")
    object KycSupplementaryInfo : Screen("kyc_supplementary_info")
    object KycBindCard : Screen("kyc_bind_card")
    object KycOcr : Screen("kyc_ocr")
    object KycFace : Screen("kyc_face")
    object CreditResult : Screen("credit_result")
}
