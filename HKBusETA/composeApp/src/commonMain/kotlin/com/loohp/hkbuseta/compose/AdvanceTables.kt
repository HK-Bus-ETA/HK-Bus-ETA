package com.loohp.hkbuseta.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loohp.hkbuseta.common.utils.Immutable
import kotlin.math.max
import kotlin.math.min

@Immutable
data class TableColumn(
    val width: TableColumnWidth,
    val alignment: Alignment.Horizontal = Alignment.Start
)

@Immutable
sealed interface TableColumnWidth {
    data class Weight(val weight: Float): TableColumnWidth
    data class Fixed(val width: Dp): TableColumnWidth
    data object Wrap: TableColumnWidth
    data class Max(val a: TableColumnWidth, val b: TableColumnWidth): TableColumnWidth
    data class Min(val a: TableColumnWidth, val b: TableColumnWidth): TableColumnWidth
}

@Immutable
data class TableRow(
    val alignment: TableRowAlignment = TableRowAlignment.Vertical(Alignment.CenterVertically),
    val onClick: (() -> Unit)? = null,
    val background: @Composable () -> Unit = { /* do nothing */ },
    val horizontalExtension: Dp = 0.dp
)

@Immutable
sealed interface TableRowAlignment {
    data class Vertical(val alignment: Alignment.Vertical): TableRowAlignment
    data class Baseline(val alignment: AlignmentLine): TableRowAlignment
}

val DefaultTableRow: TableRow = TableRow()

@Composable
fun Table(
    columnsCount: Int,
    columns: (Int) -> TableColumn = { TableColumn(width = TableColumnWidth.Weight(1F / columnsCount)) },
    modifier: Modifier = Modifier,
    rowSpacing: Dp = 0.dp,
    columnSpacing: Dp = 0.dp,
    rowDivider: @Composable () -> Unit = { /* do nothing */ },
    rows: (Int) -> TableRow = { DefaultTableRow },
    content: @Composable () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    SubcomposeLayout(
        modifier = modifier
    ) { constraints ->
        // 1. Column policies & alignments
        val policies = List(columnsCount) { columns.invoke(it).width }
        val hAligns = List(columnsCount) { columns.invoke(it).alignment }
        val rowSpacingPx = rowSpacing.roundToPx()
        val colSpacingPx = columnSpacing.roundToPx()

        // 2. Subcompose all cells
        val cellMeasurables = subcompose("cells", content)
        val rowCount = (cellMeasurables.size + columnsCount - 1) / columnsCount

        // 3. Intrinsic width resolver
        fun intrinsicWidthFor(policy: TableColumnWidth, colIndex: Int): Int = when (policy) {
            is TableColumnWidth.Fixed -> policy.width.roundToPx()
            is TableColumnWidth.Wrap -> cellMeasurables
                .filterIndexed { idx, _ -> idx % columnsCount == colIndex }
                .maxOfOrNull { it.maxIntrinsicWidth(constraints.maxHeight) } ?: 0
            is TableColumnWidth.Max -> max(
                intrinsicWidthFor(policy.a, colIndex),
                intrinsicWidthFor(policy.b, colIndex)
            )
            is TableColumnWidth.Min -> min(
                intrinsicWidthFor(policy.a, colIndex),
                intrinsicWidthFor(policy.b, colIndex)
            )
            is TableColumnWidth.Weight -> 0
        }

        val baseWidths = policies.mapIndexed { i, p -> intrinsicWidthFor(p, i) }
        val totalFixed = baseWidths.sum() + colSpacingPx * (columnsCount - 1)
        val totalWeight = policies.mapNotNull { (it as? TableColumnWidth.Weight)?.weight }.sum()
        val remaining = (constraints.maxWidth - totalFixed).coerceAtLeast(0)
        val colWidths = baseWidths.mapIndexed { i, w ->
            (policies[i] as? TableColumnWidth.Weight)?.let { p ->
                ((p.weight / totalWeight) * remaining).toInt()
            }?: w
        }

        // 4. Measure all cells to fixed colWidths
        val cellPlaceables = cellMeasurables.mapIndexed { i, m ->
            val col = i % columnsCount
            m.measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = colWidths[col],
                    minHeight = 0,
                    maxHeight = constraints.maxHeight
                )
            )
        }

        // 5. Prepare dividers
        val dividerCount = max(0, rowCount - 1)
        val tableWidthPx = colWidths.sum() + colSpacingPx * (columnsCount - 1)
        val dividerPlaceables = List(dividerCount) { r ->
            subcompose("divider_$r", rowDivider)
                .firstOrNull()
                ?.measure(
                    Constraints.fixedWidth(tableWidthPx)
                        .copy(minHeight = 0, maxHeight = constraints.maxHeight)
                )
        }
        val dividerHeight = dividerPlaceables.firstOrNull()?.height?: 0

        // 6. Compute heights & baselines using rows(row).alignment
        val rowHeights = MutableList(rowCount) { 0 }
        val baselineAbove = MutableList(rowCount) { 0 }
        for (r in 0 until rowCount) {
            val start = r * columnsCount
            val end = minOf(start + columnsCount, cellPlaceables.size)
            val rowPlaceables = cellPlaceables.subList(start, end)

            when (val ra = rows(r).alignment) {
                is TableRowAlignment.Baseline -> {
                    var above = 0; var below = 0
                    for (placeable in rowPlaceables) {
                        val b = placeable[ra.alignment]
                        above = maxOf(above, b)
                        below = maxOf(below, placeable.height - b)
                    }
                    baselineAbove[r] = above
                    rowHeights[r] = above + below
                }
                is TableRowAlignment.Vertical -> {
                    rowHeights[r] = rowPlaceables.maxOfOrNull { it.height }?: 0
                }
            }
        }

        // 7. Final layout dimensions
        val layoutHeight = (rowHeights.sum() + (dividerHeight + rowSpacingPx) * dividerCount).coerceIn(constraints.minHeight, constraints.maxHeight)
        val layoutWidth = tableWidthPx.coerceIn(constraints.minWidth, constraints.maxWidth)

        layout(layoutWidth, layoutHeight) {
            // Precompute X offsets for each column
            val colX = List(columnsCount) { i -> (0 until i).sumOf { j -> colWidths[j] + colSpacingPx } }

            var y = 0
            for (r in 0 until rowCount) {
                val rowInfo = rows(r)
                val horizontalExtension = rowInfo.horizontalExtension.roundToPx()

                // 8a. Draw background composable for this row
                subcompose("background_$r", rowInfo.background)
                    .firstOrNull()
                    ?.measure(Constraints.fixed(layoutWidth + horizontalExtension * 2, rowHeights[r] + (dividerHeight + rowSpacingPx)))
                    ?.place(-horizontalExtension, y)

                // 8b. Draw clickable composable for this row
                rowInfo.onClick?.let { onClick ->
                    subcompose("onClick_$r") {
                        Spacer(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClick = onClick)
                        )
                    }
                        .firstOrNull()
                        ?.measure(Constraints.fixed(layoutWidth + horizontalExtension * 2, rowHeights[r] + (dividerHeight + rowSpacingPx)))
                        ?.place(-horizontalExtension, y)
                }

                // 8c. Place each cell in that row
                when (val alignment = rowInfo.alignment) {
                    is TableRowAlignment.Baseline -> {
                        val above = baselineAbove[r]
                        for (c in 0 until columnsCount) {
                            val idx = r * columnsCount + c
                            cellPlaceables.getOrNull(idx)?.place(
                                x = colX[c] + hAligns[c].align(cellPlaceables[idx].width, colWidths[c], layoutDirection),
                                y = y + (above - cellPlaceables[idx][alignment.alignment])
                            )
                        }
                    }
                    is TableRowAlignment.Vertical -> {
                        val h = rowHeights[r]
                        for (c in 0 until columnsCount) {
                            val idx = r * columnsCount + c
                            cellPlaceables.getOrNull(idx)?.place(
                                x = colX[c] + hAligns[c].align(cellPlaceables[idx].width, colWidths[c], layoutDirection),
                                y = y + alignment.alignment.align(cellPlaceables[idx].height, h)
                            )
                        }
                    }
                }

                y += rowHeights[r]
                if (r < rowCount - 1) {
                    // 8d. Place divider if needed
                    dividerPlaceables[r]?.place(0, y)
                    y += dividerHeight + rowSpacingPx
                }
            }
        }
    }
}