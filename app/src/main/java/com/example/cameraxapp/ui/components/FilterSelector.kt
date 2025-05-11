package com.example.cameraxapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FilterSelector(onFilterSelected: (String) -> Unit) {
    val filterOptions = listOf("NONE", "MONO", "SEPIA")
    var selectedFilter by remember { mutableStateOf("NONE") }
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .background(color = Color.White, shape = RoundedCornerShape(16.dp))
            .padding(4.dp)
            .height(24.dp)
            .clickable {
                expanded = !expanded
            },
        contentAlignment = Alignment.Center
    ) {
        Row {
            Text("Filter: $selectedFilter", modifier = Modifier.padding(horizontal = 12.dp))
            DropdownMenu(
                containerColor = Color.White,
                modifier = Modifier
                    .height(200.dp),
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                filterOptions.forEach {
                    DropdownMenuItem(
                        onClick = {
                            selectedFilter = it
                            onFilterSelected(selectedFilter)
                            expanded = !expanded
                        },
                        text = { Text(it) },
                    )
                }
            }
        }
    }
}