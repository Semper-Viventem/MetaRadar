package f.cking.software.utils

import f.cking.software.splitToBatches
import f.cking.software.splitToBatchesEqual
import junit.framework.TestCase.assertEquals
import org.junit.Test

class SplitToBatchesTest {
    @Test
    fun `test list size is less than batch`() {
        testSplitToBatches(
            listSize = 0,
            batchSize = 1,
            expected = listOf(emptyList()),
        )
        testSplitToBatches(
            listSize = 1,
            batchSize = 2,
            expected = listOf(listOf(0)),
        )
    }

    @Test
    fun `test list size is equal to batch size`() {
        testSplitToBatches(
            listSize = 1,
            batchSize = 1,
            expected = listOf(listOf(0)),
        )
    }

    @Test
    fun `test batch size is grater than list`() {
        testSplitToBatches(
            listSize = 5,
            batchSize = 10,
            expected = listOf(listOf(0, 1, 2, 3, 4)),
        )
    }

    @Test
    fun `test general scenarios`() {
        testSplitToBatches(
            listSize = 2,
            batchSize = 1,
            expected = listOf(listOf(0), listOf(1)),
        )
        testSplitToBatches(
            listSize = 2,
            batchSize = 2,
            expected = listOf(listOf(0, 1)),
        )
        testSplitToBatches(
            listSize = 3,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2)),
        )
        testSplitToBatches(
            listSize = 4,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2, 3)),
        )
        testSplitToBatches(
            listSize = 5,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2, 3), listOf(4)),
        )
        testSplitToBatches(
            listSize = 6,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2, 3), listOf(4, 5)),
        )
        testSplitToBatches(
            listSize = 7,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2, 3), listOf(4, 5), listOf(6)),
        )
        testSplitToBatches(
            listSize = 8,
            batchSize = 2,
            expected = listOf(listOf(0, 1), listOf(2, 3), listOf(4, 5), listOf(6, 7)),
        )
    }

    @Test
    fun `split to equal batches`() {
        testSplitToBatchesEq(
            listSize = 0,
            batchCount = 1,
            expected = listOf(emptyList()),
        )
        testSplitToBatchesEq(
            listSize = 1,
            batchCount = 2,
            expected = listOf(listOf(0), emptyList()),
        )
        testSplitToBatchesEq(
            listSize = 2,
            batchCount = 2,
            expected = listOf(listOf(0), listOf(1)),
        )
        testSplitToBatchesEq(
            listSize = 3,
            batchCount = 2,
            expected = listOf(listOf(0, 2), listOf(1)),
        )
        testSplitToBatchesEq(
            listSize = 4,
            batchCount = 2,
            expected = listOf(listOf(0, 2), listOf(1, 3)),
        )
        testSplitToBatchesEq(
            listSize = 5,
            batchCount = 3,
            expected = listOf(listOf(0, 3), listOf(1, 4), listOf(2)),
        )
        testSplitToBatchesEq(
            listSize = 6,
            batchCount = 3,
            expected = listOf(listOf(0, 3), listOf(1, 4), listOf(2, 5)),
        )
        testSplitToBatchesEq(
            listSize = 8,
            batchCount = 3,
            expected = listOf(listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5)),
        )
    }

    private fun testSplitToBatches(
        listSize: Int,
        batchSize: Int,
        expected: List<List<Int>>,
    ) {
        val list: List<Int> = (0 until listSize).toList()
        val actual = list.splitToBatches(batchSize)
        assertEquals(expected, actual)
    }

    private fun testSplitToBatchesEq(
        listSize: Int,
        batchCount: Int,
        expected: List<List<Int>>,
    ) {
        val list: List<Int> = (0 until listSize).toList()
        val actual = list.splitToBatchesEqual(batchCount)
        assertEquals(expected, actual)
    }
}
