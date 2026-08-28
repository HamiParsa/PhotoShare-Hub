package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.NetworkStatus
import com.example.ui.theme.DarkRoseError
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.RoseError
import com.example.ui.theme.SuccessGreen

@Composable
fun NetworkBadge(
    networkStatus: NetworkStatus,
    modifier: Modifier = Modifier
) {
    val (icon, label, color) = when {
        networkStatus.isWifi -> Triple(
            Icons.Default.Wifi,
            networkStatus.ssid ?: stringResource(R.string.wifi_connected),
            SuccessGreen
        )
        networkStatus.isCellular -> Triple(
            Icons.Default.SignalCellularAlt,
            stringResource(R.string.cellular_connected),
            InfoBlue
        )
        else -> Triple(
            Icons.Default.WifiOff,
            stringResource(R.string.offline),
            RoseError
        )
    }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

