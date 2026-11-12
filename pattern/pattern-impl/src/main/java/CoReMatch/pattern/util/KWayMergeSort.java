package CoReMatch.pattern.util;

import CoReMatch.db.core.secondaryindex.CompositeKey;
import CoReMatch.db.core.secondaryindex.EventID;
import xxl.core.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class KWayMergeSort {

    public static List<Pair<?, EventID>> mergeSortDescending(List<Pair<?, EventID>> data) {
        if (data == null || data.size() <= 1) {
            return data;
        }

        Map<Object, List<Pair<?, EventID>>> groups = new HashMap<>();
        boolean isCompositeKey = false;

        Object firstKey = data.get(0).getElement1();
        if (firstKey instanceof CompositeKey) {
            isCompositeKey = true;
        }

        for (Pair<?, EventID> pair : data) {
            Object groupKey;
            if (isCompositeKey && pair.getElement1() instanceof CompositeKey) {
                CompositeKey<?, ?> compositeKey = (CompositeKey<?, ?>) pair.getElement1();
                    groupKey = compositeKey.getKey2();
            } else {
                groupKey = pair.getElement1();
            }

            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(pair);
        }

        int k = groups.size();

        if (k == 1) {
            return data;
        }

        return kWayMergeDescending(groups);
    }

    private static List<Pair<?, EventID>> kWayMergeDescending(Map<Object, List<Pair<?, EventID>>> groups) {
        int totalSize = groups.values().stream().mapToInt(List::size).sum();
        ArrayList<Pair<?, EventID>> result = new ArrayList<>(totalSize);

        List<List<Pair<?, EventID>>> groupList = new ArrayList<>(groups.values());
        final int k = groupList.size();

        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
            (a, b) -> {

                EventID eA = groupList.get(a[0]).get(a[1]).getElement2();
                EventID eB = groupList.get(b[0]).get(b[1]).getElement2();

                return Long.compare(eB.getSequenceId(), eA.getSequenceId());
            }
        );

        for (int i = 0; i < k; i++) {
            List<Pair<?, EventID>> group = groupList.get(i);
            if (!group.isEmpty()) {
                maxHeap.offer(new int[]{i, 0});
            }
        }

        long comparisonCount = 0;
        while (!maxHeap.isEmpty()) {
            int[] top = maxHeap.poll();
            int gIdx = top[0];
            int pos = top[1];

            Pair<?, EventID> current = groupList.get(gIdx).get(pos);
            result.add(current);

            List<Pair<?, EventID>> group = groupList.get(gIdx);
            if (pos + 1 < group.size()) {
                maxHeap.offer(new int[]{gIdx, pos + 1});
            }
            comparisonCount++;
        }

        return result;
    }

    public static List<Pair<?, EventID>> mergeSortDescending(
            List<Pair<?, EventID>> data,
            java.util.function.Function<Pair<?, EventID>, Object> groupKeyExtractor) {

        if (data == null || data.size() <= 1) {
            return data;
        }

        Map<Object, List<Pair<?, EventID>>> groups = new HashMap<>();
        for (Pair<?, EventID> pair : data) {
            Object groupKey = groupKeyExtractor.apply(pair);
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(pair);
        }

        int k = groups.size();
        if (k == 1) {
            return data;
        }

        return kWayMergeDescending(groups);
    }

    public static List<Pair<?, EventID>> mergeUnionDescending(List<List<Pair<?, EventID>>> branches) {
        if (branches == null || branches.isEmpty()) {
            return new ArrayList<>();
        }

        List<List<Pair<?, EventID>>> nonEmpty = new ArrayList<>();
        for (List<Pair<?, EventID>> branch : branches) {
            if (branch != null && !branch.isEmpty()) {
                nonEmpty.add(branch);
            }
        }
        if (nonEmpty.isEmpty()) {
            return new ArrayList<>();
        }
        if (nonEmpty.size() == 1) {
            return new ArrayList<>(nonEmpty.get(0));
        }

        int totalSize = nonEmpty.stream().mapToInt(List::size).sum();
        ArrayList<Pair<?, EventID>> result = new ArrayList<>(totalSize);
        final int branchCount = nonEmpty.size();

        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
            (a, b) -> {
                EventID eA = nonEmpty.get(a[0]).get(a[1]).getElement2();
                EventID eB = nonEmpty.get(b[0]).get(b[1]).getElement2();
                return Long.compare(eB.getSequenceId(), eA.getSequenceId());
            }
        );

        for (int i = 0; i < branchCount; i++) {
            maxHeap.offer(new int[]{i, 0});
        }

        EventID lastAdded = null;
        long comparisonCount = 0;
        while (!maxHeap.isEmpty()) {
            int[] top = maxHeap.poll();
            int bIdx = top[0];
            int pos = top[1];

            Pair<?, EventID> current = nonEmpty.get(bIdx).get(pos);
            EventID currentId = current.getElement2();

            if (lastAdded == null || !currentId.equals(lastAdded)) {
                result.add(current);
                lastAdded = currentId;
            }

            if (pos + 1 < nonEmpty.get(bIdx).size()) {
                maxHeap.offer(new int[]{bIdx, pos + 1});
            }
            comparisonCount++;
        }

        return result;
    }

    public static List<Pair<?, EventID>> intersectDescendingByEventId(List<List<Pair<?, EventID>>> branches) {
        if (branches == null || branches.isEmpty()) {
            return new ArrayList<>();
        }
        List<List<Pair<?, EventID>>> nonEmpty = new ArrayList<>();
        for (List<Pair<?, EventID>> branch : branches) {
            if (branch != null && !branch.isEmpty()) {
                nonEmpty.add(branch);
            }
        }
        if (nonEmpty.isEmpty()) {
            return new ArrayList<>();
        }
        if (nonEmpty.size() == 1) {
            return new ArrayList<>(nonEmpty.get(0));
        }

        List<Pair<?, EventID>> result = intersectTwoDescending(nonEmpty.get(0), nonEmpty.get(1));
        for (int i = 2; i < nonEmpty.size(); i++) {
            result = intersectTwoDescending(result, nonEmpty.get(i));
        }
        return result;
    }

    private static List<Pair<?, EventID>> intersectTwoDescending(
            List<Pair<?, EventID>> left,
            List<Pair<?, EventID>> right) {
        if (left == null || left.isEmpty() || right == null || right.isEmpty()) {
            return new ArrayList<>();
        }
        ArrayList<Pair<?, EventID>> result = new ArrayList<>(Math.min(left.size(), right.size()));
        int i = 0;
        int j = 0;
        while (i < left.size() && j < right.size()) {
            EventID leftId = left.get(i).getElement2();
            EventID rightId = right.get(j).getElement2();
            long leftSeq = leftId.getSequenceId();
            long rightSeq = rightId.getSequenceId();
            if (leftSeq == rightSeq) {
                result.add(left.get(i));
                i++;
                j++;
            } else if (leftSeq > rightSeq) {
                i++;
            } else {
                j++;
            }
        }
        return result;
    }
}
