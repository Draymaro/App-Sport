package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, category: String, equipment: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Pectoraux") }
    var customCategory by remember { mutableStateOf("") }
    var isCustomCategory by remember { mutableStateOf(false) }

    var selectedEquipment by remember { mutableStateOf("Haltères") }
    var customEquipment by remember { mutableStateOf("") }
    var isCustomEquipment by remember { mutableStateOf(false) }

    var notes by remember { mutableStateOf("") }

    val categories = listOf("Pectoraux", "Dos", "Jambes", "Épaules", "Bras", "Fessiers", "Abdos", "Cardio", "Autre")
    val equipmentList = listOf("Haltères", "Barre", "Poulie", "Machine", "Poids du corps", "Élastique", "Autre")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nouvel Exercice", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de l'exercice (ex: Hip Thrust, Face Pull)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category selection
                Text(
                    text = "Catégorie musculaire :",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = (!isCustomCategory && selectedCategory == cat),
                            onClick = {
                                isCustomCategory = false
                                selectedCategory = cat
                            },
                            label = { Text(cat) }
                        )
                    }
                }
                if (selectedCategory == "Autre" || isCustomCategory) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = {
                            customCategory = it
                            isCustomCategory = true
                        },
                        label = { Text("Nom de la catégorie personnalisée") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Equipment selection
                Text(
                    text = "Équipement utilisé :",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(equipmentList) { eq ->
                        FilterChip(
                            selected = (!isCustomEquipment && selectedEquipment == eq),
                            onClick = {
                                isCustomEquipment = false
                                selectedEquipment = eq
                            },
                            label = { Text(eq) }
                        )
                    }
                }
                if (selectedEquipment == "Autre" || isCustomEquipment) {
                    OutlinedTextField(
                        value = customEquipment,
                        onValueChange = {
                            customEquipment = it
                            isCustomEquipment = true
                        },
                        label = { Text("Équipement personnalisé (ex: KB, TRX)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Conseils d'exécution (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalCategory = if (isCustomCategory || selectedCategory == "Autre") customCategory.ifBlank { "Autre" } else selectedCategory
                    val finalEquipment = if (isCustomEquipment || selectedEquipment == "Autre") customEquipment.ifBlank { "Libre" } else selectedEquipment
                    if (name.isNotBlank()) {
                        onCreate(name, finalCategory, finalEquipment, notes)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter l'exercice")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
