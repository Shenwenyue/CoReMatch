
set -u

MATCH_OUTPUT="detail"
if [ "${1:-}" = "simple" ] || [ "${1:-}" = "detail" ]; then
    MATCH_OUTPUT="$1"
elif [ -n "${1:-}" ]; then
    echo "Usage: bash run.sh [simple|detail]"
    echo "  simple  index stats and results only"
    echo "  detail  also print each matched event (default)"
    exit 1
fi
echo "Match output: $MATCH_OUTPUT"

CWD="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Current script directory (CWD): $CWD"

mkdir -p "$CWD/out"
echo "Output directory: $CWD/out"

cd "$CWD/.."
EXP_DIR="$(pwd)"
echo "Experiments directory (EXP_DIR): $EXP_DIR"

cd "$EXP_DIR/../.."
ROOT_DIR="$(pwd)"
echo "Project root directory (ROOT_DIR): $ROOT_DIR"

echo "Building whole project ..."
if ! mvn clean install -DskipTests; then
    echo "Maven build failed, abort."
    exit 1
fi

export EXPERIMENTS_BASE_PATH="$ROOT_DIR/data"
echo "Experiments base path (EXPERIMENTS_BASE_PATH): $EXPERIMENTS_BASE_PATH"

cd "$EXP_DIR"
if [ ! -f pom.xml ]; then
    echo "[ERROR] pom.xml not found in $(pwd)"
    exit 1
fi

echo "Building classpath"
if ! mvn clean compile dependency:build-classpath \
    -Dmdep.outputFile=load_classpath \
    -Dmdep.overwriteIfNewer=true \
    -DskipTests; then
    echo "[ERROR] Maven failed"
    exit 1
fi

if [ ! -s load_classpath ]; then
    echo "[ERROR] load_classpath is missing or empty"
    exit 1
fi

CLASSPATH="$(cat load_classpath)"
echo "Classpath loaded successfully"

classes=(
    "CoReMatch.pattern.experiments.CrimeWithComposite"
)

for i in "${classes[@]}"; do
    echo "=================================================="
    echo "Running experiment: $i"
    log="$CWD/out/CrimeWithComposite.out"
    echo "Log file: $log"
    if java -Dfile.encoding=UTF-8 \
        -Dcorematch.matchOutput="$MATCH_OUTPUT" \
        -cp "target/classes:${CLASSPATH}" \
        "$i" > "$log" 2>&1; then
        echo "Experiment $i completed successfully"
    else
        echo "Error running experiment $i"
    fi
    echo "----- begin log -----"
    cat "$log"
    echo "----- end log -----"
done

cd "$CWD"
echo "Script execution completed."
echo "Results saved under: $CWD/out"
