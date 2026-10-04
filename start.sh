#!/usr/bin/env bash
# Starts Math Journey: checks Java, builds the jar if needed, runs it and opens the browser.
# Usage: ./start.sh [--demo]
set -euo pipefail

cd "$(dirname "$0")"

JAR="backend/target/math-journey.jar"
URL="http://127.0.0.1:8080"
REQUIRED_JAVA=25

print_java_help() {
  cat <<HELP
Math Journey needs Java ${REQUIRED_JAVA} or newer (a JDK).

  macOS:  brew install openjdk@${REQUIRED_JAVA}
          (or download a JDK ${REQUIRED_JAVA} installer from https://adoptium.net)
  Linux:  sudo apt install openjdk-${REQUIRED_JAVA}-jdk    # Debian/Ubuntu
          sudo dnf install java-${REQUIRED_JAVA}-openjdk   # Fedora
          (or download a JDK ${REQUIRED_JAVA} from https://adoptium.net)

Then open a new terminal, check with 'java -version', and run ./start.sh again.
HELP
}

profile_args=()
for arg in "$@"; do
  case "$arg" in
    --demo) profile_args+=("--spring.profiles.active=demo") ;;
    -h|--help) echo "Usage: ./start.sh [--demo]"; exit 0 ;;
    *) echo "Unknown option: $arg" >&2; echo "Usage: ./start.sh [--demo]" >&2; exit 2 ;;
  esac
done

# --- Java check (accept >= 25: a newer JDK runs the jar too) ---
if ! command -v java >/dev/null 2>&1; then
  echo "Java was not found." >&2
  print_java_help >&2
  exit 1
fi

java_version="$(java -version 2>&1 | awk -F'"' '/version/ {print $2; exit}' || true)"
java_major="${java_version%%.*}"
if [[ "$java_major" == "1" ]]; then
  # Legacy scheme, e.g. 1.8.0_402
  java_major="$(echo "$java_version" | cut -d. -f2)"
fi
java_major="${java_major%%-*}"   # early-access builds report e.g. "25-ea"
if ! [[ "$java_major" =~ ^[0-9]+$ ]] || (( java_major < REQUIRED_JAVA )); then
  if [[ -z "$java_version" ]]; then
    echo "A 'java' command exists but did not report a version (is a JDK installed?)." >&2
  else
    echo "Found Java ${java_version}, but Math Journey needs Java ${REQUIRED_JAVA} or newer." >&2
  fi
  print_java_help >&2
  exit 1
fi

# --- Build the jar if it is missing ---
if [[ ! -f "$JAR" ]]; then
  echo "Building Math Journey (first run, this takes a few minutes)..."
  ./mvnw -q package -DskipTests
fi

# --- Refuse to start if the port is already taken ---
port_open() { (exec 3<>/dev/tcp/127.0.0.1/8080) 2>/dev/null; }
if port_open; then
  echo "Math Journey or another app is already using 127.0.0.1:8080." >&2
  echo "Stop it (or open $URL if Math Journey is already running) and try again." >&2
  exit 1
fi

# --- Start the app ---
java -jar "$JAR" "${profile_args[@]+"${profile_args[@]}"}" &
app_pid=$!
trap 'kill "$app_pid" 2>/dev/null || true' INT TERM

echo "Starting Math Journey..."
started=false
for _ in $(seq 1 60); do
  if ! kill -0 "$app_pid" 2>/dev/null; then
    echo "Math Journey stopped during startup. See the log above." >&2
    exit 1
  fi
  if port_open; then
    started=true
    echo "Math Journey is running at $URL (press Ctrl+C to stop)."
    if command -v open >/dev/null 2>&1; then
      open "$URL" || true
    elif command -v xdg-open >/dev/null 2>&1; then
      xdg-open "$URL" >/dev/null 2>&1 || true
    else
      echo "Open $URL in your browser."
    fi
    break
  fi
  sleep 1
done

if [[ "$started" != true ]]; then
  echo "Math Journey did not come up on $URL within 60 seconds; still waiting on it (press Ctrl+C to stop)." >&2
fi

wait "$app_pid"
