package edu.stanford.spezi.account

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import edu.stanford.spezi.core.dependency
import edu.stanford.spezi.resources.Strings
import edu.stanford.spezi.ui.ImageResource
import edu.stanford.spezi.ui.StringResource
import edu.stanford.spezi.ui.validation.ValidationRule
import edu.stanford.spezi.ui.validation.intercepting
import edu.stanford.spezi.ui.validation.minimalEmail
import edu.stanford.spezi.ui.validation.minimalPassword
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * An in-memory implementation of the [AccountService] that can be used for testing or as a simple default implementation.
 *
 * This service stores credentials in memory and is not persisted across app restarts.
 * It is intended to be used for testing and demo purposes, such as in sample apps or test screens.
 *
 * ## Example:
 *
 * ```kotlin
 * class MyApplication : Application(), SpeziApplication {
 *
 *     override val configuration = Configuration {
 *         accountConfiguration(
 *             service = InMemoryAccountService(),
 *         )
 *     }
 * }
 * ```
 */
class InMemoryAccountService(
    override val configuration: AccountServiceConfiguration = defaultConfiguration,
) : AccountService {

    private val account by dependency<Account>()
    private val externalAccountStorage by dependency<ExternalAccountStorage>()

    /** Keys this service does not store itself and which are delegated to the storage provider. */
    private val unsupportedKeys: AccountKeyCollection by lazy {
        account.configuration.allKeys - supportedAccountKeys
    }

    override suspend fun signUp(signupDetails: AccountDetails): Result<Unit> {
        val accountId = signupDetails.getOrNull(AccountKeys.accountId::class)
            ?: signupDetails.getOrNull(AccountKeys.userId::class)

        val externalDetails = signupDetails.copy().apply { removeAll(supportedAccountKeys) }
        if (externalDetails.isNotEmpty && accountId != null) {
            externalAccountStorage.requestExternalStorage(accountId = accountId, details = externalDetails)
        }

        supplyDetails(signupDetails.copy().apply { removeAll(unsupportedKeys) })
        return Result.success(Unit)
    }

    override suspend fun login(credential: UserIdPasswordCredential): Result<Unit> {
        val details = AccountDetails().apply {
            this[AccountKeys.accountId::class] = credential.userId
            this[AccountKeys.userId::class] = credential.userId
        }
        supplyDetails(details)
        return Result.success(Unit)
    }

    override suspend fun logout(): Result<Unit> {
        delay(1.seconds) // Simulate some delay for logout operations
        val accountId = account.details.value?.getOrNull(AccountKeys.accountId::class)
        account.removeUserDetails()
        accountId?.let { externalAccountStorage.userDidDisassociate(it) }
        return Result.success(Unit)
    }

    override suspend fun delete(): Result<Unit> {
        val accountId = account.details.value?.getOrNull(AccountKeys.accountId::class)
        account.removeUserDetails()
        accountId?.let { externalAccountStorage.deleteAccount(it) }
        return Result.success(Unit)
    }

    override suspend fun updateAccountDetails(modifications: AccountModifications): Result<Unit> {
        val current = account.details.value ?: return Result.failure(IllegalStateException("No user is currently signed in"))
        val accountId = current.getOrNull(AccountKeys.accountId::class)

        val externalModifications = modifications.copy().removeModifications(supportedAccountKeys)
        if (!externalModifications.isEmpty && accountId != null) {
            externalAccountStorage.updateExternalStorage(accountId = accountId, modifications = externalModifications)
        }

        current.addContents(modifications.modifiedDetails)
        current.removeAll(modifications.removedAccountKeys)
        account.supplyUserDetails(current)
        return Result.success(Unit)
    }

    override suspend fun resetPassword(userId: String): Result<Unit> {
        // No-op for in-memory: there is no external reset mechanism
        return Result.success(Unit)
    }

    override suspend fun signIn(provider: AuthProvider): Result<Unit> {
        return if (provider is InMemoryProvider) {
            val email = "leland@stanford.edu"
            val accountDetails = AccountDetails().apply {
                this[AccountKeys.accountId::class] = email
                this[AccountKeys.userId::class] = email
                this[AccountKeys.name::class] = PersonName(givenName = "Leland", familyName = "Stanford")
                this[AccountKeys.email::class] = email
            }
            supplyDetails(accountDetails)
            Result.success(Unit)
        } else {
            Result.failure(UnsupportedOperationException("Provider-based sign-in is not supported by InMemoryAccountService"))
        }
    }

    /**
     * Supplies the given service-supported [details] to the [account], merging in any externally
     * stored values for the same account id retrieved from the [externalAccountStorage].
     */
    private suspend fun supplyDetails(details: AccountDetails) {
        val accountId = details.getOrNull(AccountKeys.accountId::class)
        if (accountId == null || unsupportedKeys.isEmpty()) {
            account.supplyUserDetails(details)
            return
        }
        val externalDetails = externalAccountStorage
            .retrieveExternalStorage(accountId = accountId, keys = unsupportedKeys.keys())
            .getOrElse { AccountDetails() }
        details.addContents(externalDetails)
        account.supplyUserDetails(details)
        delay(1.seconds) // Simulate some delay for account operations
    }

    private companion object {
        val supportedAccountKeys = accountKeyCollection(
            AccountKeys.accountId::class,
            AccountKeys.userId::class,
            AccountKeys.email::class,
            AccountKeys.name::class,
            AccountKeys.password::class,
            AccountKeys.dateOfBirth::class,
            AccountKeys.genderIdentity::class,
        )

        val defaultConfiguration = accountServiceConfiguration(
            supportedAccountKeys = SupportedAccountKeys.Exactly(supportedAccountKeys),
        ) {
            add(UserIdConfiguration(idType = UserIdType.Email))
            requiredKeys(AccountKeys.accountId::class, AccountKeys.userId::class, AccountKeys.name::class)
            validationRule(keyType = AccountKeys.userId::class, ValidationRule.minimalEmail.intercepting)
            validationRule(keyType = AccountKeys.email::class, ValidationRule.minimalEmail.intercepting)
            validationRule(keyType = AccountKeys.password::class, ValidationRule.minimalPassword)
            authProvider(InMemoryProvider())
        }
    }

    private class InMemoryProvider : AuthProvider {
        override val actionName = StringResource(Strings.account_in_memory_sign_up_action)
        override val icon: ImageResource = ImageResource(Icons.Outlined.School)
    }
}
