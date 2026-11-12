# CoReMatch

This repository implements type–attribute composite secondary indexes and replay-free complex-event pattern matching over index candidates, on top of the companion code of a SIGMOD 2021 paper.

The experiments in the CoReMatch paper were completed on all four datasets. The code uploaded to GitHub keeps only part of the Chicago Crime data as a runnable example. The Windows and Linux scripts both use `CrimeWithComposite` as the experiment entry point.

## Source

This project is developed from the companion code of the following paper:

> M. Körber, N. Glombiewski, and B. Seeger, “Index-accelerated pattern matching in event stores,” in *Proceedings of the 2021 ACM SIGMOD International Conference on Management of Data*, 2021, pp. 1023–1036.

The original system provides the event store, primary index, single-attribute secondary indexes, the basic pattern-matching framework, and cost-based selection.

Part of the underlying index and cursor infrastructure comes from the XXL library. Its license is in `xxl/LICENSE`.

Copyright in that base implementation remains with the original authors or the corresponding right holders. While reusing that infrastructure, this project adds the composite index structure, query-constraint handling, bound-guided match enumeration, and the replay-free execution flow.

These extensions cover index organization, query processing, and match execution. They go beyond parameter tuning or small configuration changes to the original system. This repository is not the official reproduction of the SIGMOD 2021 paper.

Use, modification, and redistribution must follow the licenses of the original code, the XXL library, and the other dependencies, and must keep the required copyright and license notices.

## What this repository adds

The original system stores and retrieves events with a primary index plus single-attribute secondary indexes. This project adds type–attribute composite indexes, query-condition decomposition, index-access routing, bound-guided enumeration, and verification of cross-event residual constraints, forming a replay-free pattern-matching path.

- **Composite secondary indexes.** New types are `CompositeKey`, `CompositeAttributeRange`, and `LSMCompositeTimeIndex`. A Crime event has `ID`, `PRIMARY`, `BEAT`, `LATITUDE`, and `LONGITUDE`. Besides the independent type index on `PRIMARY`, database creation builds `(PRIMARY, LATITUDE)`, `(PRIMARY, LONGITUDE)`, and `(PRIMARY, BEAT)`. A composite entry locates the full event in the primary index by EventID. The index directory is `data/db/crime`.

- **Composite lookup by pattern position.** `CompositeConditionBuilder` turns the type and attribute conditions at one pattern position into a composite lookup and routes it by attribute. Several `PRIMARY` values at the same position are a disjunction and are looked up separately. A shared BEAT or latitude/longitude condition is counted once. Two crime types plus latitude and longitude are 4 conditions. A position that has only an event type uses the independent `PRIMARY` index.

- **Cross-event residual constraints.** Constant attribute ranges on each pattern position are used for index lookup. `attributeEqualities` in the JSON states cross-event equalities, for example `A.BEAT=C.BEAT`. These equalities stay residual constraints and are checked after a complete candidate match is materialized. They are not rewritten into a new single-event index range. A query without this field does not check a cross-event equality.

- **Bound-guided match enumeration.** `pattern.matchPolicy` accepts `STNM` (Skip-Till-Next-Match) and `STAM` (Skip-Till-Any-Match). Under STAM, `pattern.stamMode` accepts `DFS` and `SCAN`. DFS derives the legal successor range from candidate sequence numbers and timestamps, then enumerates combinations that satisfy order and the time window. SCAN searches successors by scanning candidates one by one.

- **Replay-free matching.** The `ANCHOR_ONLY` execution path directly enumerates valid event combinations from indexed candidate lists using sequence numbers and timestamps. Complete candidate matches are materialized through EventIDs/TIDs for residual constraint verification, without generating replay intervals or performing NFA-based event replay.

`CrimeWithComposite.main` runs **CoReMatch-Any**, the replay-free Any-Match variant presented in the paper, using `ANCHOR_ONLY`, `STAM`, and `DFS`.

Pattern positions, event types, attribute ranges, disjunctions, and cross-event equalities are defined in:

`pattern/pattern-experiments/src/main/resources/range_queries_composite.json`

## Queries included with this repository

The JSON file provides four Chicago Crime queries that exercise different composite-index lookups and pattern-matching functions:

| Query | What it shows                                                                                                                                                         |
| --- |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Beat_range` | Three pattern positions share a BEAT range and use `(PRIMARY, BEAT)`                                                                                                  |
| `Lat_lon_range` | Three pattern positions include latitude and longitude range constraints, and candidate events are jointly filtered using type–attribute composite indexes            |
| `Or_primary_lat_lon` | One pattern position has several event types, and the shared latitude/longitude range is attached once                                                                |
| `Cross_event_lat_lon` | A and B have their own latitude/longitude ranges, C is looked up by type only, and the latitude/longitude equalities between A and C are checked after materialization |

The concrete predicates, pattern structure, and time window are those in `range_queries_composite.json`.

The recorded result of each query is `results[matchCount, phaseTimeMs, queryHeapPeakMB]`. Phase time covers query preparation and the full-condition strategy. Database creation and page-cache warmup are outside that window.

## Environment and data

Requirements:

- JDK 14 or newer
- Maven 3 or newer
- Windows or Linux
- `java` and `mvn` on `PATH`

Check them with:

```bash
java -version
mvn -version
```

The Crime example CSV is:

```text
data/src/CrimesExample.csv
```

On Windows the same path is:

```text
data\src\CrimesExample.csv
```

The launch scripts set `EXPERIMENTS_BASE_PATH` to `<repository root>/data`. The program reads the CSV from `EXPERIMENTS_BASE_PATH/src`.

Indexes are stored in:

```text
data/db/crime
```

If this directory is missing, the program builds the indexes from the CSV. If it already exists, the program loads them.

The first build can take several gigabytes of disk. Leave enough free space.

## Windows

The Windows script is:

```text
pattern\pattern-experiments\scripts\run.bat
```

From the repository root, in CMD or the IDEA terminal:

```bat
pattern\pattern-experiments\scripts\run.bat detail
```

`detail` prints every matched event. `simple` keeps the index statistics and `results[...]` only. Omitting the argument uses `detail`.

You can also double-click `run.bat` in Explorer, or start it by absolute path from another directory.

The script locates the repository from its own path and then:

1. Runs `mvn clean install -DskipTests` at the repository root.
2. Compiles the `pattern-experiments` module.
3. Builds the runtime classpath.
4. Sets `EXPERIMENTS_BASE_PATH`.
5. Runs `CoReMatch.pattern.experiments.CrimeWithComposite`.
6. Saves the log and prints it.

The Windows log path is:

```text
pattern\pattern-experiments\scripts\out\CrimeWithComposite.out
```

When the run finishes, the script prints the log and waits at `pause`.

If the Maven build fails, the script stops and does not start Java.

## Linux

The Linux script is:

```text
pattern/pattern-experiments/scripts/run.sh
```

From the repository root:

```bash
bash pattern/pattern-experiments/scripts/run.sh detail
```

Or enter the script directory and run:

```bash
bash run.sh simple
```

From another directory, use the absolute path of the script. The argument is the same as on Windows: `detail` prints every matched event, `simple` keeps the index statistics and `results[...]` only, and omitting the argument uses `detail`.

The script locates the repository from its own path, runs the Maven build, compiles the experiment module, builds the classpath, sets `EXPERIMENTS_BASE_PATH`, and then runs:

```text
CoReMatch.pattern.experiments.CrimeWithComposite
```

Standard output and standard error are saved to:

```text
pattern/pattern-experiments/scripts/out/CrimeWithComposite.out
```

The script prints the log when the run finishes.

If the Maven build fails or the classpath file is empty, the script exits.

## Where to look

| Item | Path |
| --- | --- |
| Run log | `pattern/pattern-experiments/scripts/out/CrimeWithComposite.out` |
| Query definitions | `pattern/pattern-experiments/src/main/resources/range_queries_composite.json` |
| Event data | `data/src/CrimesExample.csv` |
| Indexes | `data/db/crime` |
