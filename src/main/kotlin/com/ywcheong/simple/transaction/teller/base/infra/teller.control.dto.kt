package com.ywcheong.simple.transaction.teller.base.infra

data class LookupTellerPermissionResponse(
    val permissions: List<String>
)

data class GrantTellerPermissionResponse(
    val permission: String,
    val hasPermissionBefore: Boolean,
)

data class RevokeTellerPermissionResponse(
    val permission: String,
    val hasPermissionBefore: Boolean,
)

data class CreateSigningKeyResponse(
    val tellerPrivateKey: String
)

data class VerifySignRequest(
    val signedMessage: String,
    val checkingPermission: String,
)

data class VerifySignResponse(
    val message: String
)