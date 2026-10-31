plugins {
    alias(libs.plugins.android.application) apply false
    // Not applied: AGP compiles Kotlin itself. Declaring it pins the Kotlin version AGP uses,
    // which must match the Compose compiler plugin.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
