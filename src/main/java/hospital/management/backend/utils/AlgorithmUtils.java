package hospital.management.backend.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Standalone in-memory sorting and searching algorithms used across the service
 * layer to order cached collections and locate elements without a database round-trip.
 *
 * <h2>Merge Sort</h2>
 * <ul>
 *   <li>Time complexity: O(n log n) — best, average, and worst case.</li>
 *   <li>Space complexity: O(n) auxiliary for the merge step.</li>
 *   <li>Stable: yes — elements that compare equal preserve their original order.</li>
 *   <li>Use over SQL ORDER BY when the list is already in memory (cached) and a
 *       second database query would be wasteful.</li>
 *   <li>Prefer over quicksort when stability is required (e.g., sorting doctors
 *       by name and keeping insertion order as a tiebreaker).</li>
 * </ul>
 *
 * <h2>Binary Search</h2>
 * <ul>
 *   <li>Time complexity: O(log n).</li>
 *   <li>Space complexity: O(1).</li>
 *   <li>Precondition: the list MUST already be sorted in ascending order by the
 *       same key that is passed to the search.</li>
 *   <li>Use after mergeSort to locate a specific entity (e.g., a doctor by ID)
 *       inside a cached, sorted list — avoids a DAO lookup entirely.</li>
 * </ul>
 */
public final class AlgorithmUtils {

    private AlgorithmUtils() {}

    // ── Merge Sort ────────────────────────────────────────────────────────────

    /**
     * Sorts {@code list} in-place using a top-down merge sort.
     *
     * @param list       the list to sort (modified in-place)
     * @param comparator defines the element ordering
     * @param <T>        element type
     */
    public static <T> void mergeSort(List<T> list, Comparator<T> comparator) {
        if (list == null || list.size() <= 1) return;
        List<T> temp = new ArrayList<>(list);
        mergeSortHelper(list, temp, 0, list.size() - 1, comparator);
    }

    private static <T> void mergeSortHelper(List<T> list, List<T> temp,
                                            int left, int right,
                                            Comparator<T> comparator) {
        if (left >= right) return;
        int mid = left + (right - left) / 2;
        mergeSortHelper(list, temp, left,    mid,   comparator);
        mergeSortHelper(list, temp, mid + 1, right, comparator);
        merge(list, temp, left, mid, right, comparator);
    }

    private static <T> void merge(List<T> list, List<T> temp,
                                  int left, int mid, int right,
                                  Comparator<T> comparator) {
        for (int k = left; k <= right; k++) temp.set(k, list.get(k));
        int i = left, j = mid + 1, k = left;
        while (i <= mid && j <= right) {
            if (comparator.compare(temp.get(i), temp.get(j)) <= 0) {
                list.set(k++, temp.get(i++));
            } else {
                list.set(k++, temp.get(j++));
            }
        }
        while (i <= mid) list.set(k++, temp.get(i++));
        // remaining right-half elements are already in their correct positions
    }

    // ── Binary Search ─────────────────────────────────────────────────────────

    /**
     * Searches for the element whose key equals {@code targetKey} using binary search.
     *
     * <p><strong>Precondition:</strong> {@code list} must be sorted in ascending
     * order by the values returned by {@code keyExtractor}. Violating this
     * precondition produces an undefined result.
     *
     * @param list         sorted list to search
     * @param targetKey    the key value to locate
     * @param keyExtractor extracts the comparable key from each element
     * @param <T>          element type
     * @param <K>          key type — must implement {@link Comparable}
     * @return index of the matching element, or {@code -1} if not found
     */
    public static <T, K extends Comparable<K>> int binarySearch(
            List<T> list, K targetKey, Function<T, K> keyExtractor) {
        if (list == null || list.isEmpty() || targetKey == null) return -1;
        int lo = 0, hi = list.size() - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int cmp = keyExtractor.apply(list.get(mid)).compareTo(targetKey);
            if (cmp == 0)      return mid;
            else if (cmp < 0)  lo = mid + 1;
            else               hi = mid - 1;
        }
        return -1;
    }
}