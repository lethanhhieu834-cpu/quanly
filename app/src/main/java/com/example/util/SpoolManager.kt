package com.example.util

import java.util.Locale
import kotlin.math.abs

object SpoolManager {

    data class SpoolDeductionResult(
        val updatedSpools: List<Double>,
        val formattedBreakdown: String,
        val deductedSpoolIndex: Int = -1,
        val usedNewSpoolBecauseFirstTooShort: Boolean = false,
        val skippedSpoolsSummary: String? = null,
        val partialSpoolsCount: Int = 0,
        val isMultiplePartialSpools: Boolean = false,
        val warningMessage: String? = null,
        val hasContinuousCut: Boolean = true,
        val exactMatchUsed: Boolean = false,
        val totalRemainingMeters: Double = 0.0
    )

    data class ConversionResult(
        val enteredQty: Double,
        val enteredUnit: String,
        val lengthInMeters: Double,
        val quantityInPieces: Double,
        val calculatedPrice: Double,
        val conversionExplanation: String
    )

    /**
     * Deducts requested length from a list of pieces/spools (cây / cuộn).
     *
     * BUSINESS RULE:
     * "đơn vị tính cho ống các loại là cây. thí dụ ta nhập 6 cây mỗi cây dài 6m,
     *  khi ta bán 5m thì còn dư 5 cây 6m và 1 cây 1m,
     *  nếu ta bán 8m thì còn dư 3 cây 6m, 1 cây 1m, 1 cây 2m không được phép gộp chung,
     *  nếu bán thêm 1m nữa thì còn 3 cây 6m với 1 cây 2m, cứ vậy suy ra,
     *  chế độ mua bán dây điện hay ống hay cây khi có quy đổi thì thông minh thêm"
     *
     * Smart cutting strategy:
     * 1. Exact Match: If any piece exactly matches the requested length (e.g. selling 1m when a 1m piece exists),
     *    use it first! The piece is depleted to 0m without any waste.
     * 2. Best-Fit Leftover: If no exact match, find leftover pieces (length < standardLength) that have length >= request.
     *    Pick the smallest one to avoid cutting into full pieces.
     * 3. Full Piece Cut: If no leftover piece is big enough, take a full piece (length >= standardLength),
     *    cut requested length from it, leaving (standardLength - request) as a new leftover piece.
     * 4. Multi-Piece Cut: If request exceeds standard length (e.g. 8m of 6m pipe), deduct full pieces first,
     *    then recursively apply smart cut to the remainder.
     * 5. NEVER merge leftover pieces together!
     */
    fun deductFromSpools(
        currentSpools: List<Double>,
        lengthToDeduct: Double,
        unit: String = "m",
        pieceUnit: String = "cây",
        standardLength: Double = 6.0
    ): SpoolDeductionResult {
        if (currentSpools.isEmpty()) {
            return SpoolDeductionResult(
                updatedSpools = emptyList(),
                formattedBreakdown = "Hết $pieceUnit trong kho",
                hasContinuousCut = false,
                warningMessage = "Không còn $pieceUnit nào trong kho"
            )
        }

        if (lengthToDeduct <= 0) {
            val nonZero = currentSpools.filter { it > 0.0 }
            return SpoolDeductionResult(
                updatedSpools = nonZero,
                formattedBreakdown = formatSpoolBreakdown(nonZero, unit, pieceUnit),
                partialSpoolsCount = nonZero.count { it < standardLength },
                isMultiplePartialSpools = nonZero.distinct().size >= 2,
                warningMessage = getMultipleSpoolsWarning(nonZero, unit, pieceUnit),
                totalRemainingMeters = nonZero.sum()
            )
        }

        val spools = currentSpools.filter { it > 0.0 }.toMutableList()
        val stdLen = if (standardLength > 0) standardLength else (spools.maxOrNull() ?: 6.0)

        // Case A: Can be satisfied by a single piece in the list
        val maxAvailablePiece = spools.maxOrNull() ?: 0.0
        if (lengthToDeduct <= maxAvailablePiece) {
            // 1. Search for EXACT MATCH first (e.g. selling 1m when a 1m piece exists)
            val exactIndex = spools.indexOfFirst { abs(it - lengthToDeduct) < 0.001 }
            if (exactIndex != -1) {
                // Exact piece is completely used up!
                spools.removeAt(exactIndex)
                val finalSpools = spools.filter { it > 0.0 }
                val breakdown = formatSpoolBreakdown(finalSpools, unit, pieceUnit)
                val isMulti = finalSpools.distinct().size >= 2

                return SpoolDeductionResult(
                    updatedSpools = finalSpools,
                    formattedBreakdown = breakdown,
                    deductedSpoolIndex = exactIndex,
                    usedNewSpoolBecauseFirstTooShort = false,
                    skippedSpoolsSummary = "Đã tìm thấy đoạn lẻ đúng ${formatLength(lengthToDeduct)}$unit, xuất bán trọn vẹn và không tạo thêm phần thừa.",
                    partialSpoolsCount = finalSpools.count { it < stdLen },
                    isMultiplePartialSpools = isMulti,
                    warningMessage = if (isMulti) getMultipleSpoolsWarning(finalSpools, unit, pieceUnit) else null,
                    hasContinuousCut = true,
                    exactMatchUsed = true,
                    totalRemainingMeters = finalSpools.sum()
                )
            }

            // 2. Search for BEST-FIT PARTIAL PIECE (piece < standardLength but >= lengthToDeduct)
            val partialCandidates = spools.indices.filter { spools[it] < stdLen && spools[it] >= lengthToDeduct }
            if (partialCandidates.isNotEmpty()) {
                val bestPartialIndex = partialCandidates.minByOrNull { spools[it] }!!
                val originalLen = spools[bestPartialIndex]
                val remaining = originalLen - lengthToDeduct

                if (remaining > 0.001) {
                    spools[bestPartialIndex] = remaining
                } else {
                    spools.removeAt(bestPartialIndex)
                }

                val finalSpools = spools.filter { it > 0.0 }
                val breakdown = formatSpoolBreakdown(finalSpools, unit, pieceUnit)
                val isMulti = finalSpools.distinct().size >= 2

                return SpoolDeductionResult(
                    updatedSpools = finalSpools,
                    formattedBreakdown = breakdown,
                    deductedSpoolIndex = bestPartialIndex,
                    usedNewSpoolBecauseFirstTooShort = false,
                    skippedSpoolsSummary = "Cắt từ $pieceUnit dở ${formatLength(originalLen)}$unit -> còn dư ${formatLength(remaining)}$unit.",
                    partialSpoolsCount = finalSpools.count { it < stdLen },
                    isMultiplePartialSpools = isMulti,
                    warningMessage = if (isMulti) getMultipleSpoolsWarning(finalSpools, unit, pieceUnit) else null,
                    hasContinuousCut = true,
                    totalRemainingMeters = finalSpools.sum()
                )
            }

            // 3. Take a FULL PIECE (piece >= standardLength)
            val fullIndex = spools.indexOfFirst { it >= lengthToDeduct }
            if (fullIndex != -1) {
                val originalLen = spools[fullIndex]
                val remaining = originalLen - lengthToDeduct

                if (remaining > 0.001) {
                    spools[fullIndex] = remaining
                } else {
                    spools.removeAt(fullIndex)
                }

                val finalSpools = spools.filter { it > 0.0 }
                val breakdown = formatSpoolBreakdown(finalSpools, unit, pieceUnit)
                val isMulti = finalSpools.distinct().size >= 2
                val skippedPieces = spools.filter { it < lengthToDeduct }
                val skippedText = if (skippedPieces.isNotEmpty()) {
                    "Các đoạn lẻ nhỏ hơn (${skippedPieces.joinToString { "${formatLength(it)}$unit" }}) được giữ nguyên không gộp chung. Lấy 1 $pieceUnit mới ${formatLength(originalLen)}$unit để cắt ${formatLength(lengthToDeduct)}$unit."
                } else null

                return SpoolDeductionResult(
                    updatedSpools = finalSpools,
                    formattedBreakdown = breakdown,
                    deductedSpoolIndex = fullIndex,
                    usedNewSpoolBecauseFirstTooShort = skippedPieces.isNotEmpty(),
                    skippedSpoolsSummary = skippedText,
                    partialSpoolsCount = finalSpools.count { it < stdLen },
                    isMultiplePartialSpools = isMulti,
                    warningMessage = if (isMulti) getMultipleSpoolsWarning(finalSpools, unit, pieceUnit) else null,
                    hasContinuousCut = true,
                    totalRemainingMeters = finalSpools.sum()
                )
            }
        }

        // Case B: Request is greater than any single piece (e.g. selling 8m when standard is 6m)
        val totalAvailable = spools.sum()
        if (totalAvailable < lengthToDeduct) {
            return SpoolDeductionResult(
                updatedSpools = spools,
                formattedBreakdown = formatSpoolBreakdown(spools, unit, pieceUnit),
                hasContinuousCut = false,
                warningMessage = "CẢNH BÁO: Tổng tồn kho (${formatLength(totalAvailable)}$unit) không đủ bán ${formatLength(lengthToDeduct)}$unit!",
                totalRemainingMeters = totalAvailable
            )
        }

        // Multi-piece deduction:
        // Deduct as many full pieces as possible, then cut the remainder smartly
        var remainingNeeded = lengthToDeduct
        val updatedList = spools.toMutableList()

        while (remainingNeeded > 0) {
            // Find a piece to deduct from:
            // 1. If remainingNeeded >= stdLen and there's a full piece, take it!
            val fullIdx = updatedList.indexOfFirst { it >= stdLen }
            if (remainingNeeded >= stdLen && fullIdx != -1) {
                val pLen = updatedList[fullIdx]
                val cut = if (remainingNeeded >= pLen) pLen else remainingNeeded
                val rem = pLen - cut
                if (rem > 0.001) {
                    updatedList[fullIdx] = rem
                } else {
                    updatedList.removeAt(fullIdx)
                }
                remainingNeeded -= cut
                continue
            }

            // 2. Remaining needed is less than standard piece:
            // Look for exact match or best fit
            val subResult = deductFromSpools(
                currentSpools = updatedList,
                lengthToDeduct = remainingNeeded,
                unit = unit,
                pieceUnit = pieceUnit,
                standardLength = stdLen
            )
            updatedList.clear()
            updatedList.addAll(subResult.updatedSpools)
            remainingNeeded = 0.0
            break
        }

        val finalSpools = updatedList.filter { it > 0.0 }
        val breakdown = formatSpoolBreakdown(finalSpools, unit, pieceUnit)
        val isMulti = finalSpools.distinct().size >= 2

        return SpoolDeductionResult(
            updatedSpools = finalSpools,
            formattedBreakdown = breakdown,
            deductedSpoolIndex = -1,
            usedNewSpoolBecauseFirstTooShort = true,
            skippedSpoolsSummary = "Đơn hàng yêu cầu ${formatLength(lengthToDeduct)}$unit, hệ thống xuất đủ số lượng và bảo toàn các đoạn lẻ tách biệt.",
            partialSpoolsCount = finalSpools.count { it < stdLen },
            isMultiplePartialSpools = isMulti,
            warningMessage = if (isMulti) getMultipleSpoolsWarning(finalSpools, unit, pieceUnit) else null,
            hasContinuousCut = false, // Multi-cut
            totalRemainingMeters = finalSpools.sum()
        )
    }

    /**
     * Simulates deduction without modifying data, for real-time UI feedback.
     */
    fun simulateDeduction(
        currentSpools: List<Double>,
        lengthToDeduct: Double,
        unit: String = "m",
        pieceUnit: String = "cây",
        standardLength: Double = 6.0
    ): SpoolDeductionResult {
        return deductFromSpools(currentSpools, lengthToDeduct, unit, pieceUnit, standardLength)
    }

    /**
     * Formats spool/piece list into clean, exact human-readable Vietnamese phrasing.
     * Examples:
     * - "5 cây 6m và 1 cây 1m (không được phép gộp chung)"
     * - "3 cây 6m, 1 cây 1m, 1 cây 2m (không được phép gộp chung)"
     * - "3 cây 6m và 1 cây 2m (không được phép gộp chung)"
     * - "6 cây 6m"
     */
    fun formatSpoolBreakdown(
        spools: List<Double>,
        unit: String = "m",
        pieceUnit: String = "cây"
    ): String {
        if (spools.isEmpty()) return "Hết $pieceUnit"
        val valid = spools.filter { it > 0.0 }
        if (valid.isEmpty()) return "Hết $pieceUnit"

        // Group by length (rounded to 1 decimal)
        val grouped = mutableMapOf<Double, Int>()
        for (len in valid) {
            val rounded = (Math.round(len * 100.0) / 100.0)
            grouped[rounded] = (grouped[rounded] ?: 0) + 1
        }

        // Sort descending by length
        val sortedEntries = grouped.entries.sortedByDescending { it.key }

        val formattedParts = sortedEntries.map { (len, count) ->
            val lenStr = formatLength(len) + unit
            "$count $pieceUnit $lenStr"
        }

        val baseString = when (formattedParts.size) {
            1 -> formattedParts[0]
            2 -> "${formattedParts[0]} và ${formattedParts[1]}"
            else -> formattedParts.joinToString(", ")
        }

        return if (sortedEntries.size >= 2) {
            "$baseString (không được phép gộp chung)"
        } else {
            baseString
        }
    }

    /**
     * Warning message when 2 or more distinct piece lengths exist in stock.
     */
    fun getMultipleSpoolsWarning(
        spools: List<Double>,
        unit: String = "m",
        pieceUnit: String = "cây"
    ): String {
        val distinctSizes = spools.filter { it > 0.0 }.distinct().size
        if (distinctSizes < 2) return ""
        val details = formatSpoolBreakdown(spools, unit, pieceUnit)
        return "⚠️ CẢNH BÁO TỒN KHO: $details. Tuyệt đối không được phép gộp chung các đoạn thừa, phải tách biệt ra để quản lý và xuất bán!"
    }

    /**
     * Smart Unit Conversion Calculator:
     * Converts seamlessly between meters and standard pieces (cây / cuộn),
     * calculating exact price and providing live explanation.
     */
    fun calculateConversion(
        enteredQty: Double,
        isMeterMode: Boolean,
        standardPieceLength: Double,
        productBasePrice: Double, // Price in product.sellPrice
        productBaseUnit: String, // product.unit ("cây", "m", "cuộn", etc.)
        pieceUnit: String = "cây"
    ): ConversionResult {
        val stdLen = if (standardPieceLength > 0.0) standardPieceLength else 6.0

        val pricePerPiece: Double
        val pricePerMeter: Double

        if (productBaseUnit.equals("m", ignoreCase = true)) {
            // Base price is per meter (e.g. 5,000 VND / m)
            pricePerMeter = productBasePrice
            pricePerPiece = productBasePrice * stdLen
        } else {
            // Base price is per piece (e.g. 120,000 VND / cây)
            pricePerPiece = productBasePrice
            pricePerMeter = if (stdLen > 0) productBasePrice / stdLen else productBasePrice
        }

        return if (isMeterMode) {
            // User entered in meters (e.g. 5m, 8m, 1m)
            val meters = enteredQty
            val piecesEquivalent = if (stdLen > 0) meters / stdLen else 1.0
            val totalPrice = meters * pricePerMeter
            val explanation = if (meters % stdLen == 0.0) {
                "${formatLength(meters)}m = ${formatLength(piecesEquivalent)} $pieceUnit"
            } else {
                "${formatLength(meters)}m = ${"%.2f".format(Locale.US, piecesEquivalent)} $pieceUnit (Cắt từ $pieceUnit ${formatLength(stdLen)}m)"
            }
            ConversionResult(
                enteredQty = enteredQty,
                enteredUnit = "m",
                lengthInMeters = meters,
                quantityInPieces = piecesEquivalent,
                calculatedPrice = totalPrice,
                conversionExplanation = explanation
            )
        } else {
            // User entered in pieces (e.g. 1 cây, 2 cây)
            val pieces = enteredQty
            val metersEquivalent = pieces * stdLen
            val totalPrice = pieces * pricePerPiece
            ConversionResult(
                enteredQty = enteredQty,
                enteredUnit = pieceUnit,
                lengthInMeters = metersEquivalent,
                quantityInPieces = pieces,
                calculatedPrice = totalPrice,
                conversionExplanation = "${formatLength(pieces)} $pieceUnit = ${formatLength(metersEquivalent)}m"
            )
        }
    }

    fun formatLength(length: Double): String {
        return if (length % 1.0 == 0.0) length.toLong().toString() else "%.1f".format(Locale.US, length)
    }
}
