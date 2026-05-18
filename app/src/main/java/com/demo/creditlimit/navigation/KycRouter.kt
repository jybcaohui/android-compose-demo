package com.demo.creditlimit.navigation

/**
 * KYC navigation router based on user/profile bitmask.
 *
 * Bitmask positions (1-indexed):
 *   1=Aadhaar正面OCR, 2=Aadhaar反面, 3=Pan正面OCR, 4=Aadhaar/Pan一致性,
 *   5=基本信息, 6=其他信息, 7=人脸识别, 8=绑卡, 9=Aadhaar反面地址,
 *   10=身份信息, 11=紧急联系人
 *
 * Navigation order: 6 → 11 → 5 → 1/10 → 7 → 8
 */
object KycRouter {

    private const val POS_OCR = 1
    private const val POS_BASIC_INFO = 5
    private const val POS_SUPPLEMENTARY = 6
    private const val POS_FACE = 7
    private const val POS_BIND_CARD = 8
    private const val POS_IDENTITY = 10
    private const val POS_EMERGENCY = 11

    /** Returns 1 if the bit at [position] is set, 0 otherwise. */
    fun getStatus(profile: Int, position: Int): Int = (profile ushr (position - 1)) and 1

    /**
     * Resolves the next KYC screen the user should visit based on their [profile] bitmask.
     * Returns null if all KYC steps are complete (proceed to credit result or home).
     */
    fun resolveNextScreen(profile: Int): Screen? = when {
        getStatus(profile, POS_SUPPLEMENTARY) == 0 -> Screen.KycSupplementaryInfo
        getStatus(profile, POS_EMERGENCY) == 0 -> Screen.KycEmergencyContact
        getStatus(profile, POS_BASIC_INFO) == 0 -> Screen.KycBasicInfo
        getStatus(profile, POS_OCR) == 0 || getStatus(profile, POS_IDENTITY) == 0 -> Screen.KycOcr
        getStatus(profile, POS_FACE) == 0 -> Screen.KycFace
        getStatus(profile, POS_BIND_CARD) == 0 -> Screen.KycBindCard
        else -> null  // KYC complete
    }
}
