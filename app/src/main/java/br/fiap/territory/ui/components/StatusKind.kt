package br.fiap.territory.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import br.fiap.territory.ui.theme.LocalStatusColors
import br.fiap.territory.ui.theme.StatusPalette

/** Estado visual de um componente. A cor nunca comunica sozinha: cada tipo tem ícone e rótulo. */
enum class StatusKind(val icon: ImageVector) {
    NORMAL(Icons.Default.CheckCircle),
    INFO(Icons.Default.Info),
    ATTENTION(Icons.Default.Warning),
    CRITICAL(Icons.Default.Error),
    NEUTRAL(Icons.Default.Info),
}

@Composable
fun StatusKind.palette(): StatusPalette {
    val colors = LocalStatusColors.current
    return when (this) {
        StatusKind.NORMAL -> colors.normal
        StatusKind.INFO, StatusKind.NEUTRAL -> colors.info
        StatusKind.ATTENTION -> colors.attention
        StatusKind.CRITICAL -> colors.critical
    }
}
