#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ARTIFACT="$SCRIPT_DIR/dist/crm-jrpc-server-1.0.0.jar"
MAVEN_FALLBACK="/home/jay/.local/tools/apache-maven-3.9.9/bin/mvn"

if command -v mvn >/dev/null 2>&1; then
    MAVEN_COMMAND=$(command -v mvn)
elif [[ -x "${MAVEN_BIN:-$MAVEN_FALLBACK}" ]]; then
    MAVEN_COMMAND="${MAVEN_BIN:-$MAVEN_FALLBACK}"
else
    echo "Maven was not found. Install Maven or set MAVEN_BIN to its executable." >&2
    exit 1
fi

command -v java >/dev/null 2>&1 || {
    echo "Java was not found in PATH." >&2
    exit 1
}

echo "Building and testing CRM JRPC server..."
"$MAVEN_COMMAND" -f "$SCRIPT_DIR/pom.xml" clean package

[[ -s "$ARTIFACT" ]] || {
    echo "Build completed without producing $ARTIFACT" >&2
    exit 1
}

JAR_ENTRIES=$(jar tf "$ARTIFACT")
for required in \
    in/petsfort/crm/CrmApplication.class \
    in/petsfort/crm/CrmRpc.class \
    in/petsfort/crm/CrmHandler.class \
    in/petsfort/crm/EmbeddedHttpServer.class \
    crm-assets/index.html \
    crm-assets/database.html \
    crm-assets/analytics.html \
    crm-assets/sitemap.xml
do
    grep -Fxq "$required" <<<"$JAR_ENTRIES" || {
        echo "Required JAR entry is missing: $required" >&2
        exit 1
    }
done

if grep -Eq '^(com/jay/|com/google/gson/|com/google/firebase/)' <<<"$JAR_ENTRIES"; then
    echo "The JAR incorrectly contains a Studio-provided dependency." >&2
    exit 1
fi

MANIFEST=$(unzip -p "$ARTIFACT" META-INF/MANIFEST.MF)
grep -Fq 'Jrpc-Lifecycle-Class: in.petsfort.crm.CrmApplication' <<<"$MANIFEST" || {
    echo "The JRPC lifecycle manifest entry is missing." >&2
    exit 1
}

echo
echo "Build successful: $ARTIFACT"
ls -lh "$ARTIFACT"
sha256sum "$ARTIFACT"
