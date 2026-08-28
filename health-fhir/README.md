# Module health-fhir

<!--

This source file is part of the My Heart Counts Android open-source project

SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)

SPDX-License-Identifier: MIT

-->

The `health-fhir` module is the My Heart Counts Android producer for the Grove
FHIR 0.3.0 Mobile and Health Connect implementation guides. It converts an
explicitly supported AndroidX Health Connect 1.1 record into a deterministic
FHIR R4 collection `Bundle` containing the measurement `Observation` resources,
supporting `Device` and `Specimen` resources when applicable, and conversion
`Provenance`.

The module does not fetch Health Connect records, upload bundles, choose a
storage layout, or provide a deployment sink. Callers supply the durable journal
and idempotent sink; the coordinator commits its local export state only after
that sink acknowledges the exact serialized event. The surrounding collector
advances a durable Health Connect token only after that acknowledgement.

## New to FHIR?

FHIR is a standard for exchanging health data as JSON documents called resources.
This module emits four kinds: an `Observation` for each measurement, a `Device` for the thing that recorded it and for the application that converted it, a `Provenance` recording where the data came from, and a `Bundle` holding them together.

If those terms are new, read the *New to FHIR* page of the Grove Mobile implementation guide
(`https://grovealliance.org/fhir/mobile`) before this one.
It explains the resources emitted here, why a measurement carries two devices, and the difference between a server-assigned `id` and a business `identifier`, without assuming any FHIR background.

The guide is the single place those concepts are explained; this README covers only what is
specific to this module. One mapping is worth stating here, because it is this module's choice:
`Observation.effective` comes from the Health Connect record, and `issued` is the `convertedAt`
you pass to `convert`.

## Converting a record

```kotlin
val converter = HealthConnectConverter(
    context = HealthConnectConversionContext(
        subject = participantPatient,          // who the measurements are about
        assembler = thisApplicationDevice,     // the app performing the conversion
        sourceApplication = { packageName -> deviceFor(packageName) },
        recordingDevice = { device -> recordingDeviceFor(device) },
    ),
    synchronizationScope = scope,
)

val conversion = converter.convert(
    record = weightRecord,                     // an AndroidX Health Connect record
    convertedAt = Instant.now(),
    eventSequence = EventSequence("1"),        // durable, strictly increasing per export
)
```

Three things about that call are worth knowing before you use it:

- **The adapter never invents an identity.**
  `subject`, `assembler`, `sourceApplication`, and `recordingDevice` are all supplied by you, and each must resolve to a resource that ends up in the emitted Bundle.
  There is no fallback that quietly makes one up.
- **`eventSequence` is durable state, not a counter you reset.**
  It participates in the exchange identity, so the same record exported twice with the same sequence produces the same Bundle and deduplicates at the sink.
  `EventSequence` and `ScopeKey` are value classes: the format is checked once where the value is minted, and the two cannot be swapped for one another.
- **Unsupported records fail closed.**
  A record type outside the published inventory raises rather than emitting a partial or guessed resource.

## Supported source records

The closed 1.1 inventory contains 41 record types. The 0.3.0 producer supports
40 and explicitly defers 1:

| Status | Health Connect record types |
| --- | --- |
| Supported | `ActiveCaloriesBurnedRecord`, `BasalBodyTemperatureRecord`, `BasalMetabolicRateRecord`, `BloodGlucoseRecord`, `BloodPressureRecord`, `BodyFatRecord`, `BodyTemperatureRecord`, `BodyWaterMassRecord`, `BoneMassRecord`, `CervicalMucusRecord`, `CyclingPedalingCadenceRecord`, `DistanceRecord`, `ElevationGainedRecord`, `ExerciseSessionRecord`, `FloorsClimbedRecord`, `HeartRateRecord`, `HeartRateVariabilityRmssdRecord`, `HeightRecord`, `HydrationRecord`, `IntermenstrualBleedingRecord`, `LeanBodyMassRecord`, `MenstruationFlowRecord`, `MenstruationPeriodRecord`, `MindfulnessSessionRecord`, `NutritionRecord`, `OvulationTestRecord`, `OxygenSaturationRecord`, `PowerRecord`, `RespiratoryRateRecord`, `RestingHeartRateRecord`, `SexualActivityRecord`, `SkinTemperatureRecord`, `SleepSessionRecord`, `SpeedRecord`, `StepsCadenceRecord`, `StepsRecord`, `TotalCaloriesBurnedRecord`, `Vo2MaxRecord`, `WeightRecord`, `WheelchairPushesRecord` |
| Deferred | `PlannedExerciseSessionRecord` |

`HealthConnectCatalog` and its tests bind this matrix to the exact
`RecordType.all` inventory. Adding or removing an AndroidX source type therefore
fails until the adapter assigns it one status.

Blood-glucose output is additionally fail-closed by specimen meaning. Whole
blood, capillary blood, plasma, serum, and interstitial fluid select distinct
standard-first Health Connect adapter profiles and deterministic `Specimen`
resources. These meanings are not presented as shared Mobile profiles because
their specimen distinctions are specific to this source contract. Tears and
unknown specimen sources are not emitted under a false blood-glucose claim.

An exercise session becomes one `workout` Observation whose `hasMember` links one
`workout-segment` child per Health Connect segment and per lap. The shared
activity and segment classifications absorb the AndroidX long tail into `#other`,
and every output retains its exact `EXERCISE_TYPE_*` or `EXERCISE_SEGMENT_TYPE_*`
token as a second coding.

## Identity and revisions

`metadata.id` names the exact stored Record. It is not a deduplication key on its own: a writer that
re-imports a measurement reuses its `clientRecordId` and raises its `clientRecordVersion`, and the
stored Record then carries a new `metadata.id`.

When a Record carries a `clientRecordId`, the converter emits it as a second Observation identifier
and the `clientRecordVersion` as an extension, so a receiver supersedes the lower version instead of
counting the measurement twice. A Record without one carries neither; the module does not synthesize
a client identity it was not given.

`Observation.issued` is the Record's `metadata.lastModifiedTime`, not the conversion instant. An
unchanged Record has to convert to an identical graph, or the export journal stops recognising it as
unchanged and the outbox replays it. The conversion event itself is recorded on `Provenance`.

## Identity and graph contract

- Native Health Connect record identifiers are inputs to scoped, domain-separated
  digests; raw record IDs and the private repository scope are not placed on the
  FHIR wire.
- Every resource graph entry has a complete business `Identifier`. Its
  deterministic `urn:uuid:` full URL follows the Grove RFC 8785/UUIDv5 contract.
- `Resource.id` remains repository-owned and is not synthesized from source IDs.
- Every `Observation` carries exactly one typed Health Connect Record-class
  extension, so the exact AndroidX source type remains inspectable without
  exposing the raw record identifier.
- A physical recording Device requires an explicit producer-only identity
  admission: either deployment-scoped or caller-authorized hardware. The
  admission is never serialized, and `Device.serialNumber` is rejected in
  favor of complete, governed business identifiers.
- Mobile scalar and aggregate effective endpoints are rounded to the nearest
  millisecond with ties to even, while retaining a source offset when Health
  Connect supplies one. Exact nanosecond source instants remain available to
  the adapter's identity and synchronization contracts.
- Shared measurements directly claim exactly one shared Mobile profile and the
  Health Connect adapter profile. The four admitted glucose/specimen meanings
  instead claim exactly one adapter-specific child profile. Standard profiles
  are inherited rather than redundantly asserted.
- The converter application, physical recorder, source application, patient,
  study, and conversion `Provenance` retain distinct roles.

## Conformance

The producer-owned conformance command emits the complete deterministic corpus
and its machine-readable implementation capability inventory. It first proves
that the dependency-derived AndroidX version and all 41 supported/deferred
statuses exactly match the checked-out adapter catalog. It also binds one real
AndroidX-generated fixture for each of the 13 shared Mobile measurements to the
IG's exact source-neutral clinical vector. It then validates every resource with
the official offline FHIR Validator against the exact Grove Mobile and Health
Connect packages. The command runs only from an exact clean producer HEAD;
ignored or untracked files inside executable source roots are rejected rather
than attributed to the recorded revision:

```bash
./Scripts/validate-health-connect-fhir-conformance.sh \
  --grove-fhir /path/to/grove-fhir \
  --mobile-package /path/to/mobile/package.tgz \
  --health-connect-package /path/to/health-connect/package.tgz \
  --validator-jar /path/to/validator_cli.jar
```

Draft pull requests run the scoped Health/FHIR tests plus this conformance lane.
Ready-for-review pull requests additionally run the repository's complete
Android and signed-release gates.
