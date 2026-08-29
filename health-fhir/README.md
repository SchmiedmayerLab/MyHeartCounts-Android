# Module health-fhir

<!--

This source file is part of the My Heart Counts Android open-source project

SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)

SPDX-License-Identifier: MIT

-->

`health-fhir` is the AndroidX Health Connect 1.1.0 producer for the Grove FHIR
R4 Mobile and Health Connect contracts. One conversion event represents
exactly one immutable source-record revision and produces a FHIR collection
`Bundle`. A source removal is a separate Provenance-only retraction event; it is
an assertion for a configured sink, not a FHIR delete command.

The module converts and coordinates export and includes a production Room
journal. It does not read Health Connect, select a repository schema, upload
data, manage cryptographic keys, or advance a Health Connect changes token. The
application opens the journal and supplies an idempotent sink. The coordinator
commits local state only after that sink acknowledges the exact serialized
event.

This library is therefore not a standalone production exporter. Its
`RoomHealthConnectExportJournal` supplies exact-payload persistence, global
monotonic event sequences, revision compare-and-set, and renewable fenced source
and reconciliation leases across database instances without holding a database
transaction across sink I/O. It uses an exported versioned Room schema and never
falls back to destructive migration. The application still owns database
backup/retention policy, the managed HMAC key, Health Connect changes-token
coordination, credentials, and destination acknowledgement semantics.

## Required configuration

A deployment must persist all of the following independently of an app process:

- one repository-scope `FhirIdentifierKey` for exactly one Health Connect
  repository;
- one lowercase producer-instance UUID and one positive monotonic event counter;
- one managed HMAC-SHA-256 key, key id, positive epoch, and deployment-owned
  identity-system family;
- distinct deployment-owned event and entry-node Identifier systems; and
- the export journal and pending payload bytes.

Open one durable journal database and retain it for at least as long as an
emitted graph can be replayed or retracted:

```kotlin
val journal = RoomHealthConnectExportJournal.open(
    context = applicationContext,
    databaseName = "study-health-connect-fhir-journal.db",
)

val coordinator = HealthConnectExportCoordinator(
    converter = converter,
    journal = journal,
    sink = idempotentDestinationSink,
)
```

The default lease timing is appropriate for ordinary on-device export. A caller
may supply `RoomHealthConnectJournalOptions` for an unusually slow or contended
deployment; shortening leases increases false lease-loss risk. Close the journal
only when the owning application component is permanently stopping.

The public `00..1f` key in the Grove conformance vectors is rejected by the
production `GroveHmacIdentityKey` constructor. A production key must contain at
least 32 random bytes. Rotation changes both epoch and Identifier system; old
epochs must remain available while their outputs can be replayed or retracted.

```kotlin
val identityKey = GroveHmacIdentityKey(
    identifierSystemFamily = "https://study.example/fhir/NamingSystem/grove-opaque-v0",
    keyId = managedKeyId,
    epoch = managedKeyEpoch,
    secret = managedKeyBytes,
)

val scope = HealthConnectSynchronizationScope.create(
    repositoryScope = persistedRepositoryScope,
    producerInstance = persistedProducerInstanceUuid,
    configurationFingerprint = exactReadFilterFingerprint,
    identityKey = identityKey,
)

val converter = HealthConnectConverter(
    context = HealthConnectConversionContext(
        subject = HealthConnectPatientSubject.Bundled(participantPatient),
        assembler = converterApplicationDevice,
        assemblerHost = currentHostFacts()?.let { host ->
            HealthConnectHostDeviceResource(
                sourceDeviceToken = host.governedSnapshotToken,
                resource = host.fhirTemplate,
            )
        },
        eventIdentifierSystem = "https://study.example/fhir/NamingSystem/grove-event-v0",
        entryNodeIdentifierSystem = "https://study.example/fhir/NamingSystem/grove-entry-node-v0",
        userAuthoredTextPolicy = HealthConnectUserAuthoredTextPolicy.RETAIN,
        // Optional: omit this block unless wire-level native round-trip is required.
        nativeIdentifierDisclosure = HealthConnectNativeIdentifierDisclosure(
            system =
                "https://study.example/fhir/identifiers/device-7/health-connect-records",
            type = HealthConnectNativeIdentifierType(
                text = "Health Connect repository record id",
            ),
        ),
        recordingDevice = { sourceDevice ->
            deviceRegistry.stablePerUnitToken(sourceDevice)?.let { token ->
                HealthConnectRecordingDeviceResource(
                    stablePerUnitToken = token,
                    resource = recordingDeviceDescription(sourceDevice),
                )
            }
        },
    ),
    synchronizationScope = scope,
)

val outcome = converter.convertOutcome(
    record = recordReadFromHealthConnect,
    convertedAt = assemblyInstant,
    eventSequence = persistedNextEventSequence,
)
```

`userAuthoredTextPolicy` is intentionally mandatory: choose `RETAIN` to emit
admitted Health Connect titles/notes or `OMIT` to suppress them. Neither behavior
is an implicit default or inferred privacy policy.

Use `HealthConnectPatientSubject.Logical` with a complete, deployment-owned
pseudonymous `Identifier` when the receiver resolves the participant by logical
identity; Grove emits an identifier-only reference and does not fabricate a
`Patient` entry. Use `HealthConnectPatientSubject.Bundled` only when concrete
patient facts belong in the event Bundle.

Use `convertOutcome` when record-data rejection is part of normal collection
flow. It distinguishes `Converted`, `Unsupported`, and `Rejected`. `convert`
retains the throwing boundary for callers that deliberately treat source-data
failures as exceptions. Producer configuration and graph-invariant failures are
programming errors in both APIs.

## Identity and privacy

Every produced Observation carries exactly two Grove-typed opaque identifiers:
`source-record` and `source-output`. A synthesized glucose Specimen carries the
same source-record identity plus its own `source-output` identity, whose
`specimen` discriminator is the exact admitted source enum. Writer records and
recording Devices have separate domains. Values use the normative
`v0:<keyId>:<epoch>:<base64url HMAC-SHA-256>` form over unsigned 32-bit
length-framed UTF-8 fields. Repository scope and stable physical-device tokens
are never serialized. Writer ids are emitted only as separately scoped opaque
writer-record identities. Raw Health Connect record ids are omitted by default.

The closed protocol implementation also recognizes the Provider-specific
`provider-output` and `provider-artifact` domains even though this adapter emits
the generic Health Connect source domains. This keeps normative parsing and
vector behavior aligned with the complete Mobile exchange protocol rather than
silently treating Provider coordinates as generic source coordinates.

When a deployment explicitly configures `nativeIdentifierDisclosure`, Grove
adds the exact `Metadata.id` under that caller-owned absolute repository/store
system to the one-to-one primary Observation. The native Identifier supplements
the two mandatory Grove identities; it is never an event, entry, or retraction
key. It is not repeated on heart-rate/sample outputs, sleep stages, workout
segments or laps, skin-temperature deltas, present-nutrient outputs, synthesized
Specimens, Devices, or Provenance. Series and fan-out records without a single
primary output therefore do not carry it. Grove event, entry-node, opaque
identity, and identifier-role systems are rejected for this purpose.

Output identity is independent of mutable clinical values. Exactly-one outputs
use role `single` and their measurement id; sample outputs use `sample` and
`<UTC9>|<occurrence>`; sleep stages use `sleep-stage` and
`<start>|<end>|<source-token>|<occurrence>`; present nutrients use
`present-field` and their measurement id. This makes a corrected value retain
its source slot identity while same-time duplicates remain distinct. For ordered
Health Connect sample and stage lists, `occurrence` is assigned among identical
canonical coordinates in the exact platform-list order before output sorting.
The adapter never derives identity from a clinical value or unordered iteration.

Event values are clear `e0:<producer-instance>:<positive-sequence>` identifiers.
Entries without a selected business identity use deterministic `n0:` entry-node
keys. Bundle `fullUrl` values are UUIDv5 over the length-framed complete
Identifier pair using the Grove namespace. UUIDv5 is graph addressing, not a
privacy control.

A shared FHIR Device means one physical instance. Manufacturer and model are
descriptive only, so a recording Device is omitted unless the deployment
supplies a governed stable per-unit token. Grove HMACs that token into a stable
typed `recording-device` identity and a separate event-scoped `device-snapshot`;
the snapshot is the Bundle entry key. Recording Device templates cannot carry
caller identifiers, and `Device.serialNumber` is never admitted.

Application and host hardware are also separate immutable snapshots. When an
`assemblerHost` is supplied, Grove emits one profiled host snapshot and links
the converter application snapshot to it through `Device.parent`;
operating-system facts stay on the host rather than being folded into an
application version.

`Metadata.dataOrigin.packageName` has a deliberately different identity model.
The conversion Provenance carries it at `Provenance.entity.agent.who` as a typed,
identifier-only logical `Device` Reference using
`https://grovealliance.org/fhir/health-connect/NamingSystem/android-package-name`.
It identifies an application product, not an installation, host, account,
person, or physical recorder. Grove does not fabricate a Bundle Device entry or
claim a Device profile for that logical reference.

HMAC identifiers do not de-identify the patient, clinical values, timestamps,
relationships, or payloads in a Bundle.

## Clinical and lifecycle semantics

- `Observation.effective[x]` comes from the source clinical time. Mobile
  effective instants use the guide's millisecond half-even policy while identity
  inputs retain exact nanoseconds.
- `Observation.issued` is `Metadata.lastModifiedTime`.
- A present `Metadata.id` must be nonblank Unicode-scalar text. Its
  `clientRecordVersion` must be non-negative (including `0` and
  `Long.MAX_VALUE`); a negative version fails closed. When `Metadata.id` is
  absent, AndroidX exposes no presence bit for the default version, so neither
  a writer id nor version is emitted.
- conversion `Provenance.occurred[x]` is the emitted source activity time or
  span, `Provenance.recorded` is the assertion time, and `Bundle.timestamp` is
  assembly time.
- `RestingHeartRateRecord` is a point `effectiveDateTime` with LOINC `40443-4`;
  the producer does not invent a daily average.
- Mindfulness type and VO2 max measurement method retain their exact AndroidX
  tokens in Health Connect code systems through `Observation.method`. The
  configured text policy consistently retains or omits nonblank mindfulness,
  exercise, and sleep titles through the one shared session-title extension and
  notes through `Observation.note`.
- Cycling cadence, power, speed, step cadence, heart rate, and skin temperature
  emit one Observation per source sample/delta. Nutrition emits one Observation
  per present nutrient, including biotin. Blood glucose emits exactly one of the
  four admitted specimen-profile alternatives and one deterministic Specimen;
  unknown and tear sources are rejected.
- Updates first emit a separate retraction for every prior output/artifact and
  event-scoped Device snapshot, then a new active event. Retractions contain one
  profiled Provenance and no copied clinical resource or value. Each target is a
  typed complete Identifier pair with a closed target-role extension.

`HealthConnectFieldDispositions` inventories every public top-level, metadata,
and nested field in the pinned AndroidX 1.1.0 source API. Tests fail when a field
is added without a mapped, intentionally omitted, rejected, or unavailable
disposition. `PlannedExerciseSessionRecord` remains the only deferred type; the
other 40 source types are explicitly supported.

## Retry and sink rules

Use `HealthConnectExportCoordinator` for durable export. An exact retry reuses
the event Identifier, all times, entry keys, JSON bytes, and checksum. A content
or source-version change receives a new sequence. The sink must durably and
idempotently apply the complete batch before returning its acknowledgement and
must index complete `(Identifier.system, Identifier.value)` pairs. A collection
Bundle has no transaction or delete semantics; atomic application and lifecycle
policy belong to the sink.

The journal implementation must issue renewable, monotonically fenced leases
across coordinator instances and processes sharing one device-local repository. A source transition checks
its live fence and base revision at stage, local storage, and exact-event
completion; a reconciliation additionally holds one repository/type fence while
it invokes the complete-read callback, drains pending events, upserts present
records, and derives absence. Do not read the complete source list before calling
`reconcile`: the callback API deliberately puts that read inside the fence. A
database-backed implementation must not keep a database transaction open across
the source read or sink network I/O.

The adversarial in-memory journal is test-only. Production integrations should
use `RoomHealthConnectExportJournal`, which implements the same port with exact
payload persistence, global sequence allocation, revision CAS, and renewable
monotonic fences. The application must still integration-test its managed
identity key/epoch, persistent producer instance, exact read-filter fingerprint,
changes-token commit ordering, backup/retention policy, and idempotent
destination sink; those policies cannot be inferred by this FHIR library.

## Conformance

The producer tests lock the normative HMAC, Unicode, event, entry-node, and
UUIDv5 vectors; output-count rules; clinical mappings; immutable retry payloads;
and active/retraction graph shapes. The active boundary admits only the closed
output, supporting, and lifecycle resource type sets; prohibits contained
resources; resolves every literal reference inside the Bundle; closes direct
Observation, DocumentReference, Device, QuestionnaireResponse, and Provenance
profile modes; requires exactly one transform Provenance; and rejects
disconnected support. The shared structured corpus is pinned to its exact 31
reviewed negative cases. The capability export reports the exact AndroidX
baseline and supported/deferred inventory.

Run the offline official-validator lane from a clean producer revision with the
exact packages:

```bash
./Scripts/validate-health-connect-fhir-conformance.sh \
  --grove-fhir /path/to/grove-fhir \
  --mobile-package /path/to/mobile/package.tgz \
  --health-connect-package /path/to/health-connect/package.tgz \
  --validator-jar /path/to/validator_cli.jar
```

The script invokes the catalog generator by repository-absolute path and
deliberately refuses a dirty source tree—including generator changes—so generated
fixtures can be attributed to one exact producer revision.
