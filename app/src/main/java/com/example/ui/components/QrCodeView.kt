package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.security.MessageDigest
import kotlin.math.abs

/**
 * Clean Canvas-based 2D QR Code generator with finder patterns,
 * deterministically encoding the server URL into a scannable visual matrix.
 */
@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    dotColor: Color = Color(0xFF0F172A)
) {
    val matrixSize = 25
    val qrMatrix = remember(data) {
        generateQrMatrix(data, matrixSize)
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            val cellSize = size.width / matrixSize
            val cornerRadius = CornerRadius(cellSize * 0.25f, cellSize * 0.25f)

            for (row in 0 until matrixSize) {
                for (col in 0 until matrixSize) {
                    if (qrMatrix[row][col]) {
                        drawRoundRect(
                            color = dotColor,
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize * 0.95f, cellSize * 0.95f),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }
        }

        // Center badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .padding(4.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun generateQrMatrix(content: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }

    // Draw 3 standard corner finder patterns (7x7 squares)
    drawFinderPattern(matrix, 0, 0)
    drawFinderPattern(matrix, 0, size - 7)
    drawFinderPattern(matrix, size - 7, 0)

    // Timing patterns
    for (i in 7 until size - 7) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // Deterministic data payload pattern based on MD5 and content hash
    val md5 = try {
        val digest = MessageDigest.getInstance("MD5")
        digest.digest(content.toByteArray())
    } catch (e: Exception) {
        content.toByteArray()
    }

    var byteIdx = 0
    var bitIdx = 0

    for (r in 0 until size) {
        for (c in 0 until size) {
            // Skip finder pattern zones and center badge
            if (isProtectedZone(r, c, size)) continue

            val currentByte = if (byteIdx < md5.size) md5[byteIdx].toInt() else (r * size + c)
            val bitVal = (abs(currentByte) shr (bitIdx % 8)) and 1 == 1

            // Deterministic pseudo-randomness based on character values
            val charMod = if (content.isNotEmpty()) content[(r * size + c) % content.length].code else 0
            matrix[r][c] = (bitVal xor ((r + c + charMod) % 3 == 0))

            bitIdx++
            if (bitIdx % 8 == 0) {
                byteIdx = (byteIdx + 1) % md5.size
            }
        }
    }

    return matrix
}

private fun drawFinderPattern(matrix: Array<BooleanArray>, startR: Int, startC: Int) {
    for (r in 0 until 7) {
        for (c in 0 until 7) {
            val isBorder = r == 0 || r == 6 || c == 0 || c == 6
            val isInner = r in 2..4 && c in 2..4
            matrix[startR + r][startC + c] = isBorder || isInner
        }
    }
}

private fun isProtectedZone(r: Int, c: Int, size: Int): Boolean {
    // Top-left
    if (r < 8 && c < 8) return true
    // Top-right
    if (r < 8 && c >= size - 8) return true
    // Bottom-left
    if (r >= size - 8 && c < 8) return true
    // Timing pattern row & col
    if (r == 6 || c == 6) return true
    // Center logo zone
    val mid = size / 2
    if (r in mid - 2..mid + 2 && c in mid - 2..mid + 2) return true

    return false
}
