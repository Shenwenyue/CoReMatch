package CoReMatch.pattern.cost.cursor;

import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.util.TimeInterval;
import CoReMatch.pattern.cost.execution.MatchPolicy;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import CoReMatch.pattern.util.CachedCursor;
import CoReMatch.pattern.util.PhaseTimers;
import xxl.core.cursors.Cursor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class PatternCandidateBindingsIterator {

    private final long window;
    private final List<TimeInterval> filter;
    private final List<TransformedPattern.ExecutableSubPattern> subPatterns;
    private final List<? extends SecondaryTimeIndex<?>> indexes;
    private final SubPatternCandidate[] work;
    private final Cursor<SubPatternCandidate>[] cursors;
    private final Map<String, CachedCursor> queryResultCache;
    private final MatchPolicy matchPolicy;

    private final boolean stamBoundGuided;
    private boolean opened = false;

    private List<SubPatternCandidate>[] stamTables;

    private long[][] stamSeqs;
    private long[][] stamMaxSeqs;
    private long[][] stamMinTs;
    private long[][] stamTss;

    private int[][] nextHi;

    private int[][] endLo;
    private Deque<StamFrame> stamStack;
    private SubPatternCandidate[] stamPick;
    private boolean stamPrepared;

    @SuppressWarnings("unchecked")
    PatternCandidateBindingsIterator(
            long window,
            TransformedPattern.ExecutableConfiguration config,
            List<? extends SecondaryTimeIndex<?>> indexes,
            List<TimeInterval> filter,
            Map<String, CachedCursor> queryResultCache) {
        this.window = window;
        this.subPatterns = config.getSubPatterns();
        this.indexes = indexes;
        this.work = new SubPatternCandidate[subPatterns.size()];
        this.cursors = new Cursor[subPatterns.size()];
        this.filter = filter;
        this.queryResultCache = queryResultCache != null ? queryResultCache : new HashMap<>();
        this.matchPolicy = MatchPolicy.fromSystemProperty();
        this.stamBoundGuided = MatchPolicy.stamUsesPrecomputedBounds();
    }

    void open() {
        if (opened) {
            return;
        }
        opened = true;

        PhaseTimers.enterPhase2();
        try {
        for (int i = 0; i < cursors.length; i++) {
            var c = subPatterns.get(i);
            System.out.println(c);
            SubPatternCandidateCursor spCursor =
                    new SubPatternCandidateCursor(c, indexes, filter, true, queryResultCache);
            cursors[i] = spCursor;
            cursors[i].open();
        }
        } finally {
            PhaseTimers.exitPhase2();
        }
        if (matchPolicy == MatchPolicy.STAM) {
            System.out.printf("[MatchPolicy] STAM, successor=%s, n=%d%n",
                    stamBoundGuided ? "DFS" : "SCAN", subPatterns.size());
        } else {
            System.out.printf("[MatchPolicy] %s, n=%d%n", matchPolicy, subPatterns.size());
        }

        for (int i = 0; i < cursors.length; i++) {
            if (!cursors[i].hasNext()) {
                if (matchPolicy == MatchPolicy.STAM) {
                    prepareStamTables();
                }
                return;
            }
        }
        if (matchPolicy == MatchPolicy.STAM) {
            prepareStamTables();
        }
    }

    void close() {
        if (!opened) {
            return;
        }
        for (var c : cursors) {
            if (c != null) {
                c.close();
            }
        }
        stamStack = null;
        stamTables = null;
        stamSeqs = null;
        stamMaxSeqs = null;
        stamMinTs = null;
        stamTss = null;
        nextHi = null;
        endLo = null;
        stamPick = null;
        stamPrepared = false;
        opened = false;
    }

    AnchorBinding computeNextBinding() {
        if (matchPolicy == MatchPolicy.STAM) {
            return computeNextStamBinding();
        }
        return computeNextStnmBinding();
    }

    private AnchorBinding computeNextStnmBinding() {
        PhaseTimers.enterPhase3();
        try {
            return computeNextStnmBindingTimed();
        } finally {
            PhaseTimers.exitPhase3();
        }
    }

    private AnchorBinding computeNextStnmBindingTimed() {
        AnchorBinding result = null;
        outer:
        while (cursors[0].hasNext() && result == null) {
            work[0] = cursors[0].next();

            for (int i = 1; i < subPatterns.size(); i++) {
                nextElement(i, work[i - 1].getMaxSeq() + 1);
                if (work[i] == null) {
                    continue outer;
                }
            }

            long ts = work[0].getMinTs();
            long te = work[work.length - 1].getMaxTs();

            if (te - ts <= window) {
                SubPatternCandidate[] anchors = new SubPatternCandidate[work.length];
                for (int j = 0; j < work.length; j++) {
                    anchors[j] = work[j];
                }
                result = new AnchorBinding(anchors, new TimeInterval(ts, te));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void prepareStamTables() {
        if (stamPrepared) {
            return;
        }
        stamPrepared = true;
        int n = subPatterns.size();
        stamTables = new List[n];
        stamSeqs = new long[n][];
        stamMaxSeqs = new long[n][];
        stamMinTs = new long[n][];
        stamTss = new long[n][];
        PhaseTimers.enterPhase2();
        try {
        for (int i = 0; i < n; i++) {
            List<SubPatternCandidate> table = new ArrayList<>();
            if (cursors[i] != null) {
                while (cursors[i].hasNext()) {
                    table.add(cursors[i].next());
                }
            }

            Collections.reverse(table);
            stamTables[i] = table;
            int size = table.size();
            long[] seqs = new long[size];
            long[] maxSeqs = new long[size];
            long[] minTs = new long[size];
            long[] tss = new long[size];
            for (int j = 0; j < size; j++) {
                SubPatternCandidate row = table.get(j);
                seqs[j] = row.getMinSeq();
                maxSeqs[j] = row.getMaxSeq();
                minTs[j] = row.getMinTs();
                tss[j] = row.getMaxTs();
            }
            stamSeqs[i] = seqs;
            stamMaxSeqs[i] = maxSeqs;
            stamMinTs[i] = minTs;
            stamTss[i] = tss;
            
        }
        } finally {
            PhaseTimers.exitPhase2();
        }
        if (stamBoundGuided) {
            buildStamBounds();
        }
        stamPick = new SubPatternCandidate[n];
        stamStack = new ArrayDeque<>();
        if (n == 0 || stamTables[0].isEmpty()) {
            return;
        }
        stamStack.push(new StamFrame(0, 0, stamTables[0].size(), -1, -1));
    }

    private void buildStamBounds() {
        PhaseTimers.enterPhase3();
        try {
            buildStamBoundsTimed();
        } finally {
            PhaseTimers.exitPhase3();
        }
    }

    private void buildStamBoundsTimed() {
        int n = stamTables.length;
        nextHi = new int[n][];
        endLo = new int[n][];
        if (n < 2) {
            return;
        }
        long[] firstMinTs = stamMinTs[0];
        int m1 = firstMinTs.length;
        for (int i = 1; i < n; i++) {
            long[] prevMaxSeq = stamMaxSeqs[i - 1];
            long[] curMinSeq = stamSeqs[i];
            long[] curMaxTs = stamTss[i];
            int prevN = prevMaxSeq.length;
            int curN = curMinSeq.length;

            int[] hi = new int[prevN];
            int h = 0;
            for (int k = 0; k < prevN; k++) {
                long eSeq = prevMaxSeq[k];
                while (h < curN && curMinSeq[h] > eSeq) {
                    h++;
                }
                hi[k] = h;
            }
            nextHi[i] = hi;

            int[] lo = new int[m1];
            int ell = 0;
            for (int p = 0; p < m1; p++) {
                long t0 = firstMinTs[p];
                while (ell < curN && curMaxTs[ell] - t0 > window) {
                    ell++;
                }
                lo[p] = ell;
            }
            endLo[i] = lo;
        }
    }

    private AnchorBinding computeNextStamBinding() {
        if (!stamPrepared) {
            prepareStamTables();
        }
        if (stamStack == null || stamPick == null || stamTables == null) {
            return null;
        }
        PhaseTimers.enterPhase3();
        try {
            return stamBoundGuided ? computeNextStamBindingTimed() : computeNextStamScanTimed();
        } finally {
            PhaseTimers.exitPhase3();
        }
    }

    private AnchorBinding computeNextStamBindingTimed() {
        int n = stamTables.length;
        while (!stamStack.isEmpty()) {
            StamFrame frame = stamStack.peek();
            if (frame.index >= frame.end) {
                stamStack.pop();
                continue;
            }
            int pickedIdx = frame.index;
            SubPatternCandidate cand = stamTables[frame.depth].get(pickedIdx);
            frame.index++;

            if (frame.depth == 0) {
                stamPick[0] = cand;
                if (n == 1) {
                    long ts = cand.getMinTs();
                    long te = cand.getMaxTs();
                    if (te - ts <= window) {
                        return copyBinding(stamPick, ts, te);
                    }
                    continue;
                }
                pushWindowedSuccessors(1, pickedIdx, pickedIdx);
                continue;
            }

            stamPick[frame.depth] = cand;
            if (frame.depth == n - 1) {
                long ts = stamPick[0].getMinTs();
                long te = cand.getMaxTs();
                if (te - ts <= window) {
                    return copyBinding(stamPick, ts, te);
                }
                continue;
            }
            pushWindowedSuccessors(frame.depth + 1, frame.firstIdx, pickedIdx);
        }
        return null;
    }

    private void pushWindowedSuccessors(int tableIdx, int firstIdx, int lastIdx) {
        int lo = endLo[tableIdx][firstIdx];
        int hi = nextHi[tableIdx][lastIdx];
        if (lo < hi) {
            stamStack.push(new StamFrame(tableIdx, lo, hi, firstIdx, lastIdx));
        }
    }

    private AnchorBinding computeNextStamScanTimed() {
        int n = stamTables.length;
        while (!stamStack.isEmpty()) {
            StamFrame frame = stamStack.peek();
            if (frame.index >= frame.end) {
                stamStack.pop();
                continue;
            }
            int pickedIdx = frame.index;
            SubPatternCandidate cand = stamTables[frame.depth].get(pickedIdx);
            frame.index++;

            if (frame.depth == 0) {
                stamPick[0] = cand;
                if (n == 1) {
                    long ts = cand.getMinTs();
                    long te = cand.getMaxTs();
                    if (te - ts <= window) {
                        return copyBinding(stamPick, ts, te);
                    }
                    continue;
                }
                stamStack.push(new StamFrame(1, 0, stamTables[1].size(), pickedIdx, pickedIdx));
                continue;
            }

            long firstTs = stamMinTs[0][frame.firstIdx];
            long lastSeq = stamMaxSeqs[frame.depth - 1][frame.lastIdx];
            if (stamSeqs[frame.depth][pickedIdx] <= lastSeq
                    || stamTss[frame.depth][pickedIdx] - firstTs > window) {
                continue;
            }

            stamPick[frame.depth] = cand;
            if (frame.depth == n - 1) {
                long te = cand.getMaxTs();
                if (te - firstTs <= window) {
                    return copyBinding(stamPick, firstTs, te);
                }
                continue;
            }
            int nextDepth = frame.depth + 1;
            stamStack.push(new StamFrame(nextDepth, 0, stamTables[nextDepth].size(), frame.firstIdx, pickedIdx));
        }
        return null;
    }

    private static AnchorBinding copyBinding(SubPatternCandidate[] pick, long ts, long te) {
        SubPatternCandidate[] anchors = new SubPatternCandidate[pick.length];
        System.arraycopy(pick, 0, anchors, 0, pick.length);
        return new AnchorBinding(anchors, new TimeInterval(ts, te));
    }

    private void nextElement(int idx, long seqMin) {
        if (work[idx] == null && cursors[idx].hasNext()) {
            work[idx] = cursors[idx].next();
        }
        while (work[idx] != null && work[idx].getMinSeq() < seqMin && cursors[idx].hasNext()) {
            work[idx] = cursors[idx].next();
        }
        if (work[idx] == null || work[idx].getMinSeq() < seqMin) {
            work[idx] = null;
        }
    }

    private static final class StamFrame {
        final int depth;
        int index;
        final int end;
        final int firstIdx;

        final int lastIdx;

        StamFrame(int depth, int index, int end, int firstIdx, int lastIdx) {
            this.depth = depth;
            this.index = index;
            this.end = end;
            this.firstIdx = firstIdx;
            this.lastIdx = lastIdx;
        }
    }
}
