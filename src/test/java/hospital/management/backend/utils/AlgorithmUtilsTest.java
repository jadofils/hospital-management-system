package hospital.management.backend.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AlgorithmUtils")
class AlgorithmUtilsTest {

    // ── mergeSort ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("mergeSort: null list does nothing (no NPE)")
    void mergeSort_nullList_noOp() {
        assertDoesNotThrow(() -> AlgorithmUtils.mergeSort(null, Integer::compare));
    }

    @Test
    @DisplayName("mergeSort: empty list does nothing")
    void mergeSort_emptyList_noOp() {
        List<Integer> list = new ArrayList<>();
        AlgorithmUtils.mergeSort(list, Integer::compare);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("mergeSort: single-element list is unchanged")
    void mergeSort_singleElement_unchanged() {
        List<Integer> list = new ArrayList<>(List.of(42));
        AlgorithmUtils.mergeSort(list, Integer::compare);
        assertEquals(List.of(42), list);
    }

    @Test
    @DisplayName("mergeSort: already-sorted list stays sorted")
    void mergeSort_alreadySorted_noChange() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        AlgorithmUtils.mergeSort(list, Integer::compare);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    @DisplayName("mergeSort: reverse-sorted list becomes ascending")
    void mergeSort_reversed_sortedAscending() {
        List<Integer> list = new ArrayList<>(Arrays.asList(5, 4, 3, 2, 1));
        AlgorithmUtils.mergeSort(list, Integer::compare);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    @DisplayName("mergeSort: list with duplicates is sorted and all elements retained")
    void mergeSort_withDuplicates_sortedAllRetained() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6, 5, 3));
        AlgorithmUtils.mergeSort(list, Integer::compare);
        assertEquals(Arrays.asList(1, 1, 2, 3, 3, 4, 5, 5, 6, 9), list);
    }

    @Test
    @DisplayName("mergeSort: string list sorted case-insensitively")
    void mergeSort_strings_caseInsensitiveOrder() {
        List<String> list = new ArrayList<>(Arrays.asList("banana", "Apple", "cherry", "apricot"));
        AlgorithmUtils.mergeSort(list, String.CASE_INSENSITIVE_ORDER);
        assertEquals(Arrays.asList("Apple", "apricot", "banana", "cherry"), list);
    }

    @Test
    @DisplayName("mergeSort: stable — equal elements preserve original relative order")
    void mergeSort_stable_preservesRelativeOrderForEqualElements() {
        record Person(String name, int insertionOrder) {}
        List<Person> list = new ArrayList<>(Arrays.asList(
            new Person("Alice", 0),
            new Person("Bob",   1),
            new Person("Alice", 2),
            new Person("Bob",   3)
        ));
        AlgorithmUtils.mergeSort(list, (a, b) -> a.name().compareToIgnoreCase(b.name()));
        assertEquals(0, list.get(0).insertionOrder(), "first Alice should be original index 0");
        assertEquals(2, list.get(1).insertionOrder(), "second Alice should be original index 2");
        assertEquals(1, list.get(2).insertionOrder(), "first Bob should be original index 1");
        assertEquals(3, list.get(3).insertionOrder(), "second Bob should be original index 3");
    }

    @Test
    @DisplayName("mergeSort: large random list matches Collections.sort result")
    void mergeSort_largeList_matchesCollectionsSort() {
        List<Integer> expected = new ArrayList<>();
        for (int i = 500; i > 0; i--) expected.add(i);
        List<Integer> actual = new ArrayList<>(expected);

        Collections.sort(expected);
        AlgorithmUtils.mergeSort(actual, Integer::compare);
        assertEquals(expected, actual);
    }

    // ── binarySearch ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("binarySearch: null list returns -1")
    void binarySearch_nullList_returnsMinusOne() {
        assertEquals(-1, AlgorithmUtils.<String, String>binarySearch(null, "x", s -> s));
    }

    @Test
    @DisplayName("binarySearch: empty list returns -1")
    void binarySearch_emptyList_returnsMinusOne() {
        assertEquals(-1, AlgorithmUtils.binarySearch(new ArrayList<String>(), "x", s -> s));
    }

    @Test
    @DisplayName("binarySearch: null target returns -1")
    void binarySearch_nullTarget_returnsMinusOne() {
        List<String> list = List.of("a", "b", "c");
        assertEquals(-1, AlgorithmUtils.binarySearch(list, null, s -> s));
    }

    @Test
    @DisplayName("binarySearch: element not present returns -1")
    void binarySearch_notFound_returnsMinusOne() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9);
        assertEquals(-1, AlgorithmUtils.binarySearch(list, 4, i -> i));
    }

    @Test
    @DisplayName("binarySearch: element at first index found")
    void binarySearch_firstElement_returnsZero() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9);
        assertEquals(0, AlgorithmUtils.binarySearch(list, 1, i -> i));
    }

    @Test
    @DisplayName("binarySearch: element at last index found")
    void binarySearch_lastElement_returnsLastIndex() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9);
        assertEquals(4, AlgorithmUtils.binarySearch(list, 9, i -> i));
    }

    @Test
    @DisplayName("binarySearch: element in middle found")
    void binarySearch_middleElement_returnsCorrectIndex() {
        List<Integer> list = Arrays.asList(2, 4, 6, 8, 10);
        assertEquals(2, AlgorithmUtils.binarySearch(list, 6, i -> i));
    }

    @Test
    @DisplayName("binarySearch: string keys found correctly")
    void binarySearch_stringKeys_found() {
        record Item(String id, String name) {}
        List<Item> items = Arrays.asList(
            new Item("aaa", "Alpha"),
            new Item("bbb", "Beta"),
            new Item("ccc", "Gamma"),
            new Item("ddd", "Delta")
        );
        int idx = AlgorithmUtils.binarySearch(items, "ccc", Item::id);
        assertEquals(2, idx);
        assertEquals("Gamma", items.get(idx).name());
    }

    @Test
    @DisplayName("binarySearch: single-element list, element present")
    void binarySearch_singleElement_found() {
        assertEquals(0, AlgorithmUtils.binarySearch(List.of(99), 99, i -> i));
    }

    @Test
    @DisplayName("binarySearch: single-element list, element absent")
    void binarySearch_singleElement_notFound() {
        assertEquals(-1, AlgorithmUtils.binarySearch(List.of(99), 100, i -> i));
    }
}