package com.demo.creditlimit.ui.webview

/**
 * H5 ↔ Android JS message key registry.
 * Change a value here to remap the corresponding interaction on both sides.
 */
object JsMsgKey {
    // H5 → Android
    const val CAMERA          = 1   // take photo with camera
    const val GALLERY         = 2   // pick photo from gallery
    const val CONTACTS        = 3   // pick a contact
    const val OPEN_LINK       = 4   // open URL (browser or in-app)
    const val GOOGLE_REVIEW   = 5   // trigger Google Play review
    const val DEVICE_INFO     = 6   // request permission + upload device info
    const val LOGOUT          = 11  // clear token and return to login
    const val LIVENESS        = 12  // launch liveness detection

    // Android → H5
    const val BACK            = 8   // notify H5 that back key was pressed
    const val APP_LIFECYCLE   = 14  // foreground=1, background=2
    const val PUSH_TOKEN      = 15  // upload push token to H5
    const val PUSH_NOTIFY     = 16  // forward incoming push payload to H5

    // Bidirectional (H5 requests, Android responds; Android also proactively sends)
    const val APP_INFO        = 7   // app id / gaid / version / adjust info
    const val TOKEN_INFO      = 10  // access token + phone number
    const val PERMISSION      = 13  // current permission status
}
