package org.example.project.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IntegerInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = {
                val newValue = (value - step).coerceIn(min, max)
                onValueChange(newValue)
            },
            enabled = value > min
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
        }

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.widthIn(min = 60.dp)
        )

        IconButton(
            onClick = {
                val newValue = (value + step).coerceIn(min, max)
                onValueChange(newValue)
            },
            enabled = value < max
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }
}
