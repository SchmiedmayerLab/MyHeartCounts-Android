#!/usr/bin/env bash

# This source file belongs to the My Heart Counts Android project
#
# SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
#
# SPDX-License-Identifier: MIT

set -euo pipefail

usage() {
    cat <<'EOF'
Usage: Scripts/validate-health-connect-fhir-conformance.sh [options]

Required inputs (option or matching environment variable):
  --grove-fhir PATH             GROVE_FHIR_ROOT
  --mobile-package PATH         GROVE_MOBILE_PACKAGE
  --health-connect-package PATH GROVE_HEALTH_CONNECT_PACKAGE
  --validator-jar PATH          FHIR_VALIDATOR_JAR

Optional inputs:
  --export DIRECTORY            GROVE_CONFORMANCE_EXPORT
  --help

The producer emits its complete deterministic R4 corpus, creates a producer-neutral
manifest, and delegates structural and official validation to the conformance kit
from the exact grove-fhir checkout. The validator remains offline.
EOF
}

grove_fhir_root="${GROVE_FHIR_ROOT:-}"
mobile_package="${GROVE_MOBILE_PACKAGE:-}"
health_connect_package="${GROVE_HEALTH_CONNECT_PACKAGE:-}"
validator_jar="${FHIR_VALIDATOR_JAR:-}"
export_directory="${GROVE_CONFORMANCE_EXPORT:-}"

while (( $# > 0 )); do
    case "$1" in
        --grove-fhir)
            [[ $# -ge 2 ]] || { usage >&2; exit 2; }
            grove_fhir_root="$2"
            shift 2
            ;;
        --mobile-package)
            [[ $# -ge 2 ]] || { usage >&2; exit 2; }
            mobile_package="$2"
            shift 2
            ;;
        --health-connect-package)
            [[ $# -ge 2 ]] || { usage >&2; exit 2; }
            health_connect_package="$2"
            shift 2
            ;;
        --validator-jar)
            [[ $# -ge 2 ]] || { usage >&2; exit 2; }
            validator_jar="$2"
            shift 2
            ;;
        --export)
            [[ $# -ge 2 ]] || { usage >&2; exit 2; }
            export_directory="$2"
            shift 2
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

script_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "$script_directory/.." && pwd)"

require_regular_file() {
    local label="$1"
    local path="$2"
    if [[ -z "$path" || -L "$path" || ! -f "$path" ]]; then
        echo "$label must name an existing non-linked regular file: ${path:-<unset>}" >&2
        exit 2
    fi
}

if [[ -z "$grove_fhir_root" || -L "$grove_fhir_root" || ! -d "$grove_fhir_root" ]]; then
    echo "grove-fhir root must name an existing non-linked directory: ${grove_fhir_root:-<unset>}" >&2
    exit 2
fi
require_regular_file "Grove Mobile package" "$mobile_package"
require_regular_file "Grove Health Connect package" "$health_connect_package"
canonical_file() {
    local path="$1"
    local directory
    directory="$(cd "$(dirname "$path")" && pwd -P)"
    printf '%s/%s\n' "$directory" "$(basename "$path")"
}
grove_fhir_root="$(cd "$grove_fhir_root" && pwd -P)"
mobile_package="$(canonical_file "$mobile_package")"
health_connect_package="$(canonical_file "$health_connect_package")"
# The canonical contract is projected from the same catalogs the guides are built from, so a
# catalog change that never reached this repository has to fail here rather than at runtime.
python3 -B "$repository_root/Scripts/generate-grove-fhir-kotlin-contract.py" \
    --catalog-directory "$grove_fhir_root/catalog" \
    --output "$repository_root/health-fhir/src/main/kotlin/org/grovealliance/health/fhir/HealthConnectContract.kt" \
    --test-vector-output "$repository_root/health-fhir/src/test/resources/grove-exchange-protocol-test-vectors.json" \
    --check

require_regular_file "FHIR Validator jar" "$validator_jar"
validator_jar="$(canonical_file "$validator_jar")"
require_regular_file "grove-fhir producer validator" "$grove_fhir_root/Scripts/validate-producer.py"
require_regular_file "Grove Health Connect adapter catalog" "$grove_fhir_root/catalog/health-connect-adapter.json"
require_regular_file "Grove exchange-protocol catalog" "$grove_fhir_root/catalog/exchange-protocol.json"
require_regular_file "Grove measurement catalog" "$grove_fhir_root/catalog/measurement-catalog.json"

producer_inputs=(
    "Scripts/generate-grove-fhir-kotlin-contract.py"
    "Scripts/validate-health-connect-fhir-conformance.sh"
    "build-logic"
    "build.gradle.kts"
    "gradle"
    "gradle.properties"
    "gradlew"
    "health"
    "health-fhir"
    "settings.gradle.kts"
)
assert_clean_producer_inputs() {
    local dirty ignored
    dirty="$(git -C "$repository_root" status --porcelain=v1 --untracked-files=all -- "${producer_inputs[@]}")"
    if [[ -n "$dirty" ]]; then
        echo "Producer inputs differ from the exact Git HEAD:" >&2
        echo "$dirty" >&2
        exit 2
    fi
    ignored="$(
        git -C "$repository_root" ls-files --others --ignored --exclude-standard -- \
            Scripts build-logic/src gradle health/src health-fhir/src
    )"
    if [[ -n "$ignored" ]]; then
        echo "Ignored files exist inside executable producer input roots:" >&2
        echo "$ignored" >&2
        exit 2
    fi
}

assert_clean_producer_inputs
producer_revision="$(git -C "$repository_root" rev-parse --verify HEAD)"
temporary_root="$(mktemp -d "${TMPDIR:-/tmp}/mhc-health-connect-fhir.XXXXXX")"
temporary_root="$(cd "$temporary_root" && pwd -P)"
trap 'rm -rf -- "$temporary_root"' EXIT

generated_root="$temporary_root/generated"
conformance_root="$generated_root/conformance-fixtures"
wire_root="$generated_root/wire-fixtures"
capability_path="$generated_root/health-connect-capabilities.json"

(
    cd "$repository_root"
    GROVE_CONFORMANCE_EXPORT="$conformance_root" \
        GROVE_WIRE_EXPORT="$wire_root" \
        GROVE_CAPABILITY_EXPORT="$capability_path" \
        GROVE_EXCHANGE_PROTOCOL_CATALOG="$grove_fhir_root/catalog/exchange-protocol.json" \
        GROVE_MOBILE_EXCHANGE_CORPUS_DIRECTORY="$grove_fhir_root/Conformance/corpora/mobile-exchange" \
        ./gradlew :health-fhir:testDebugUnitTest --rerun-tasks --console=plain
)
assert_clean_producer_inputs

conformance_paths=()
while IFS= read -r path; do
    conformance_paths+=("$path")
done < <(find "$conformance_root" -maxdepth 1 -type f -name '*.json' -print | LC_ALL=C sort)
wire_paths=()
while IFS= read -r path; do
    wire_paths+=("$path")
done < <(find "$wire_root" -maxdepth 1 -type f -name '*.json' -print | LC_ALL=C sort)
if [[ "${#conformance_paths[@]}" -ne 75 || "${#wire_paths[@]}" -ne 4 ]]; then
    echo "Expected exactly 75 conformance resources and 4 wire resources; found ${#conformance_paths[@]} and ${#wire_paths[@]}." >&2
    exit 1
fi

python3 -B - "$capability_path" "$grove_fhir_root/catalog/health-connect-adapter.json" <<'PY'
import json
import sys
from pathlib import Path

capability_path = Path(sys.argv[1])
catalog_path = Path(sys.argv[2])
capability = json.loads(capability_path.read_text(encoding="utf-8"))
catalog = json.loads(catalog_path.read_text(encoding="utf-8"))

if capability != {
    "schemaVersion": 0,
    "sourcePackage": catalog["source"]["package"],
    "sourceVersion": catalog["source"]["version"],
    "sourceTypeExtension": catalog["sourceTypeExtension"]["url"],
    "fieldDispositionSourceVersion": catalog["source"]["version"],
    "allRecordTypes": sorted(row["token"] for row in catalog["recordTypes"]),
    "supportedRecordTypes": sorted(
        row["token"] for row in catalog["recordTypes"] if row["status"] == "supported"
    ),
    "deferredRecordTypes": sorted(
        row["token"] for row in catalog["recordTypes"] if row["status"] == "deferred"
    ),
}:
    raise SystemExit(
        "Android producer capabilities do not exactly match the checked-out "
        "Grove Health Connect adapter catalog"
    )
PY

manifest_root="$temporary_root/producer"
mkdir "$manifest_root"
mkdir "$manifest_root/resources"
cp "$capability_path" "$manifest_root/health-connect-capabilities.json"
for source in "${conformance_paths[@]}"; do
    cp "$source" "$manifest_root/resources/conformance-$(basename "$source")"
done
for source in "${wire_paths[@]}"; do
    cp "$source" "$manifest_root/resources/wire-$(basename "$source")"
done

python3 -B - "$manifest_root" "$producer_revision" \
    "$grove_fhir_root/catalog/measurement-catalog.json" <<'PY'
import json
import sys
from pathlib import Path

root = Path(sys.argv[1])
revision = sys.argv[2]
release_version = json.loads(Path(sys.argv[3]).read_text(encoding="utf-8"))["version"]
resources = []
for path in sorted((root / "resources").glob("*.json")):
    value = json.loads(path.read_text(encoding="utf-8"))
    profiles = value.get("meta", {}).get("profile", [])
    required = [
        profile for profile in profiles
        if isinstance(profile, str) and profile.startswith("https://grovealliance.org/fhir/")
    ]
    if not required:
        raise SystemExit(f"generated resource has no Grove profile: {path.name}")
    resources.append({"path": f"resources/{path.name}", "requiredProfiles": required})

semantic_vector_ids = [
    "active-energy",
    "basal-body-temperature",
    "blood-pressure",
    "body-height",
    "body-temperature",
    "body-weight",
    "distance",
    "heart-rate",
    "oxygen-saturation",
    "respiratory-rate",
    "sleep-duration",
    "sleep-stage",
    "step-count",
]
semantic_vectors = [
    {
        "id": identifier,
        "path": (
            "resources/conformance-health-connect-semantic-"
            f"{identifier}-observation.json"
        ),
        "resourcePointer": "",
    }
    for identifier in semantic_vector_ids
]
resource_paths = {resource["path"] for resource in resources}
missing_semantic_paths = {
    binding["path"] for binding in semantic_vectors
} - resource_paths
if missing_semantic_paths:
    raise SystemExit(
        "semantic-vector resources were not generated: "
        + ", ".join(sorted(missing_semantic_paths))
    )

manifest = {
    "schemaVersion": 0,
    "fhirVersion": "4.0.1",
    "producer": {
        "name": "My Heart Counts Android Health Connect",
        "version": release_version,
        "revision": revision,
    },
    "packages": [
        {
            "alias": "mobile",
            "packageId": "org.grovealliance.fhir.mobile",
            "version": release_version,
        },
        {
            "alias": "health-connect",
            "packageId": "org.grovealliance.fhir.health-connect",
            "version": release_version,
        },
    ],
    "resources": resources,
    "semanticVectors": semantic_vectors,
}
(root / "manifest.json").write_text(
    json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
PY

python3 -B "$grove_fhir_root/Scripts/validate-producer.py" \
    --manifest "$manifest_root/manifest.json" \
    --validator "$validator_jar" \
    --package "mobile=$mobile_package" \
    --package "health-connect=$health_connect_package"

if [[ -n "$export_directory" ]]; then
    if [[ -L "$export_directory" ]]; then
        echo "Export directory must not be a symbolic link: $export_directory" >&2
        exit 2
    fi
    mkdir -p "$export_directory"
    cp -R "$manifest_root/." "$export_directory/"
fi

echo "Validated 79 deterministic FHIR R4 producer resources against the exact grove-fhir package closure."
