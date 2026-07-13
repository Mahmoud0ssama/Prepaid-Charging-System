#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_ALL=false
CLEAN=false

usage() {
    cat <<'EOF'
Usage: ./build.sh [options]

Build ChargingProject Maven modules.

Options:
  --all     Also build the optional MobilePhoneRPT module
  --clean   Run mvn clean before packaging
  -h, --help
            Show this help message

Default build targets:
  - MSC server + web API (target/msc-1.0.0.jar)
  - MobilePhone client (MobilePhone/target/MobilePhone-1.0-SNAPSHOT.jar)
  - AGI service (AGI/charging-agi/target/charging-agi-1.0-SNAPSHOT.jar)
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --all) BUILD_ALL=true ;;
        --clean) CLEAN=true ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
    shift
done

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Error: '$1' is required but not installed." >&2
        exit 1
    fi
}

build_module() {
    local name="$1"
    local dir="$2"
    shift 2

    echo
    echo "==> Building ${name}"
    (
        cd "${dir}"
        if [[ "${CLEAN}" == true ]]; then
            mvn clean package "$@"
        else
            mvn package "$@"
        fi
    )
}

require_command java
require_command mvn

echo "ChargingProject build"
echo "Root: ${ROOT_DIR}"
echo "Java: $(java -version 2>&1 | head -n 1)"
echo "Maven: $(mvn -version 2>&1 | head -n 1)"

MVN_ARGS=(-q -DskipTests)

build_module "MSC + Web API" "${ROOT_DIR}" "${MVN_ARGS[@]}"
build_module "MobilePhone" "${ROOT_DIR}/MobilePhone" "${MVN_ARGS[@]}"
build_module "AGI" "${ROOT_DIR}/AGI/charging-agi" "${MVN_ARGS[@]}"

if [[ "${BUILD_ALL}" == true ]]; then
    build_module "MobilePhoneRPT" "${ROOT_DIR}/MobilePhoneRPT" \
        "${MVN_ARGS[@]}" \
        -Dmaven.compiler.source=21 \
        -Dmaven.compiler.target=21 || {
        echo "Warning: MobilePhoneRPT build failed (known package mismatch in Call.java)." >&2
    }
fi

echo
echo "Build complete."
echo
echo "Run services:"
echo "  MSC:        java -jar target/msc-1.0.0.jar"
echo "  Web server: java -cp target/msc-1.0.0.jar msc.web.WebServer"
echo "  MobilePhone: java -jar MobilePhone/target/MobilePhone-1.0-SNAPSHOT.jar <MSISDN>"
echo "  AGI:        java -jar AGI/charging-agi/target/charging-agi-1.0-SNAPSHOT.jar"
