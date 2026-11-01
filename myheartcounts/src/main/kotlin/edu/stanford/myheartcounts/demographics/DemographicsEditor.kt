//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package edu.stanford.myheartcounts.demographics

import edu.stanford.myheartcounts.account.didOptInToTrial
import edu.stanford.myheartcounts.model.StudyVariant
import edu.stanford.myheartcounts.study.StudyVariantProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.grovealliance.account.Account
import org.grovealliance.account.AccountDetails
import org.grovealliance.account.AccountService
import org.grovealliance.account.accountId
import org.grovealliance.core.Module
import kotlin.time.Duration.Companion.seconds

/**
 * Resolves the regional demographics form.
 */
interface DemographicsRegionSource {

    /**
     * The form of the region the participant's study runs in: the current one, then each change.
     */
    val regions: Flow<DemographicsRegion>

    /**
     * The form of the region the participant's study runs in.
     */
    fun region(): DemographicsRegion
}

/**
 * [DemographicsRegionSource] following the participant's [StudyVariant].
 */
class DemographicsRegionSourceImpl(
    private val variantProvider: StudyVariantProvider,
) : DemographicsRegionSource {

    override val regions: Flow<DemographicsRegion> = variantProvider.variant.map { regionOf(variant = it) }

    override fun region(): DemographicsRegion = regionOf(variant = variantProvider.currentVariant)

    private fun regionOf(variant: StudyVariant): DemographicsRegion = when (variant) {
        StudyVariant.Stanford -> DemographicsRegion.US
        StudyVariant.Imperial -> DemographicsRegion.UK
    }
}

/**
 * Holds the signed-in participant's demographic answers while they are edited, storing changed
 * answers a second after the last edit and whenever [flush] is called. Edits not stored yet survive
 * a refresh of the account's details; signing in as somebody else discards them.
 */
interface DemographicsEditor : Module {

    /**
     * The answers as currently edited.
     */
    val state: StateFlow<DemographicsState>

    /**
     * Whether each question of the participant's form is answered and needs to be.
     */
    val completion: StateFlow<Map<DemographicsField, DemographicsCompletion>>

    /**
     * Whether every question needing an answer has one.
     */
    val isComplete: StateFlow<Boolean>

    /**
     * Emits each time storing the answers fails; the answers stay pending until a later attempt
     * stores them.
     */
    val saveFailures: Flow<Unit>

    /**
     * Applies [change] to the answers, along with the answers it implies. Ignored while nobody is
     * signed in.
     */
    fun update(change: (DemographicsResponse) -> DemographicsResponse)

    /**
     * Stores every answer not stored yet, succeeding once none is left.
     */
    suspend fun flush(): Result<Unit>

    /**
     * Starts storing every answer not stored yet, without waiting for it.
     */
    fun flushSoon()
}

/**
 * Default [DemographicsEditor] implementation, confined to the main thread of [scope].
 */
class DemographicsEditorImpl(
    private val account: Account,
    private val accountService: AccountService,
    private val codec: DemographicsCodec,
    private val rules: DemographicsRules,
    private val regionSource: DemographicsRegionSource,
    private val scope: CoroutineScope,
) : DemographicsEditor {

    private val mutableState = MutableStateFlow(DemographicsState(region = regionSource.region()))
    override val state = mutableState.asStateFlow()
    override val completion = state
        .map { rules.completion(state = it) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = rules.completion(state = state.value),
        )
    override val isComplete = completion
        .map { it.isComplete }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = completion.value.isComplete,
        )
    private val mutableSaveFailures = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val saveFailures = mutableSaveFailures.asSharedFlow()
    private var baseline = DemographicsResponse.empty
    private var debounce: Job? = null
    private val saveMutex = Mutex()

    override fun configure() {
        scope.launch { account.details.collect { populate(details = it) } }
        scope.launch { regionSource.regions.collect { region -> mutableState.update { it.copy(region = region) } } }
    }

    private fun populate(details: AccountDetails?) {
        val current = mutableState.value
        val owner = details?.accountId
        val sameOwner = current.accountId == owner
        if (!sameOwner) debounce?.cancel()
        val incoming = details?.let { codec.read(details = it) } ?: DemographicsResponse.empty
        val response = if (sameOwner) {
            codec.merged(
                target = incoming,
                source = current.response,
                fields = current.pending,
            )
        } else {
            incoming
        }
        baseline = incoming
        mutableState.value = DemographicsState(
            accountId = owner,
            region = regionSource.region(),
            optedIntoTrial = details?.didOptInToTrial == true,
            response = response,
            pending = codec.differences(first = incoming, second = response),
        )
    }

    override fun update(change: (DemographicsResponse) -> DemographicsResponse) {
        val current = mutableState.value
        if (current.accountId == null) return
        val response = rules.inferred(
            previous = current.response,
            updated = change(current.response),
        )
        mutableState.value = current.copy(
            response = response,
            pending = codec.differences(first = baseline, second = response),
        )
        debounce?.cancel()
        debounce = scope.launch {
            delay(SAVE_DELAY)
            debounce = null
            flush()
        }
    }

    override fun flushSoon() {
        scope.launch { flush() }
    }

    override suspend fun flush(): Result<Unit> = saveMutex.withLock {
        debounce?.cancel()
        val submitted = mutableState.value
        if (submitted.accountId == null || account.details.value?.accountId != submitted.accountId) {
            return@withLock Result.failure(IllegalStateException("No signed-in participant to store demographics for."))
        }
        if (submitted.pending.isEmpty()) return@withLock Result.success(Unit)
        val result = codec.changes(
            baseline = baseline,
            response = submitted.response,
        ).mapCatching { accountService.updateAccountDetails(modifications = it).getOrThrow() }
        val current = mutableState.value
        if (current.accountId != submitted.accountId) {
            return@withLock Result.failure(IllegalStateException("The participant signed out while storing demographics."))
        }
        if (result.isSuccess) {
            baseline = codec.merged(
                target = baseline,
                source = submitted.response,
                fields = submitted.pending,
            )
        }
        mutableState.value = current.copy(pending = codec.differences(first = baseline, second = current.response))
        if (result.isFailure) mutableSaveFailures.tryEmit(Unit)
        result
    }

    private val Map<DemographicsField, DemographicsCompletion>.isComplete
        get() = values.none { it.isMissingRequired }

    private companion object {
        val SAVE_DELAY = 1.seconds
    }
}
