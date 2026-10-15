@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.agal.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agal.assistant.lifestyle.FinanceTransaction
import com.agal.assistant.lifestyle.FinanceType
import com.agal.assistant.lifestyle.TripDestination
import com.agal.assistant.state.AssistantViewModel
import com.agal.assistant.ui.theme.*

@Composable
fun LifestyleHubScreen(
    viewModel: AssistantViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Trips, 1: Finances
    val trips by viewModel.trips.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var showAddTripDialog by remember { mutableStateOf(false) }
    var showAddFinanceDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) "Trip Destinations" else "Finance Tracking",
                        color = TextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (selectedTab == 0) showAddTripDialog = true else showAddFinanceDialog = true
                    }) {
                        Icon(Icons.Rounded.AddCircle, contentDescription = "Add", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab Switcher
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .padding(4.dp)
                ) {
                    TabButton(
                        text = "✈️ Trips (${trips.size})",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "💰 Finances (${transactions.size})",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (selectedTab == 0) {
                // Trips Content
                item {
                    val totalTripBudget = trips.sumOf { it.estimatedExpense }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AppLightSurface, ElectricBlue.copy(alpha = 0.08f))
                                )
                            )
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Total Travel Budget", color = TextMuted, fontSize = 12.sp)
                                Text(
                                    text = "₹${totalTripBudget.toInt()}",
                                    color = TextDark,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Button(
                                onClick = { showAddTripDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                Text("+ Add Trip", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (trips.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.FlightTakeoff,
                            title = "No Trip Destinations Planned",
                            subtitle = "Say 'Add Goa to trip' or tap '+ Add Trip' to calculate estimated travel expenses."
                        )
                    }
                } else {
                    items(trips, key = { it.id }) { trip ->
                        TripCard(trip = trip, onDelete = { viewModel.deleteTrip(trip.id) })
                    }
                }

            } else {
                // Finance Tracking Content
                item {
                    val balance = viewModel.financeRepo.getTotalBalance()
                    val totalExpenses = viewModel.financeRepo.getTotalExpenses()
                    val totalIncome = viewModel.financeRepo.getTotalIncome()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, EmeraldGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(text = "Net Balance", color = TextMuted, fontSize = 12.sp)
                        Text(
                            text = "₹${balance.toInt()}",
                            color = if (balance >= 0) EmeraldGreen else Color(0xFFFF5555),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Income", color = TextMuted, fontSize = 11.sp)
                                Text(text = "+₹${totalIncome.toInt()}", color = EmeraldGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(text = "Expenses", color = TextMuted, fontSize = 11.sp)
                                Text(text = "-₹${totalExpenses.toInt()}", color = Color(0xFFFF5555), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showAddFinanceDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                            ) {
                                Text("+ Record", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (transactions.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            title = "No Expenses Logged",
                            subtitle = "Say 'Spent 500 on groceries' or 'How much did I spend' to track offline."
                        )
                    }
                } else {
                    items(transactions, key = { it.id }) { tx ->
                        FinanceTransactionCard(
                            transaction = tx,
                            onDelete = { viewModel.deleteFinanceTransaction(tx.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddTripDialog) {
        AddTripDialog(
            onDismiss = { showAddTripDialog = false },
            onAdd = { dest, cost ->
                viewModel.addTrip(dest, cost)
                showAddTripDialog = false
            }
        )
    }

    if (showAddFinanceDialog) {
        AddFinanceDialog(
            onDismiss = { showAddFinanceDialog = false },
            onAdd = { title, amt, cat, isInc ->
                viewModel.addFinanceTransaction(title, amt, cat, isInc)
                showAddFinanceDialog = false
            }
        )
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) ElectricBlue.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) ElectricBlue else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun TripCard(trip: TripDestination, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppLightSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Flight, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = trip.destination, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Estimated Budget: ₹${trip.estimatedExpense.toInt()}",
                        color = NeonAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFFF5555))
            }
        }
    }
}

@Composable
private fun FinanceTransactionCard(transaction: FinanceTransaction, onDelete: () -> Unit) {
    val isIncome = transaction.type == FinanceType.INCOME
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppLightSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderLight))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isIncome) EmeraldGreen.copy(alpha = 0.15f) else Color(0xFFFF5555).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = if (isIncome) EmeraldGreen else Color(0xFFFF5555),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = transaction.title, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = transaction.category, color = TextMuted, fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isIncome) "+" else "-"}₹${transaction.amount.toInt()}",
                    color = if (isIncome) EmeraldGreen else Color(0xFFFF5555),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppLightSurface)
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, color = TextMuted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun AddTripDialog(onDismiss: () -> Unit, onAdd: (String, Double?) -> Unit) {
    var dest by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Trip Destination", color = TextDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dest,
                    onValueChange = { dest = it },
                    label = { Text("Destination (e.g. Manali, Goa, Paris)", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderLight
                    )
                )
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Custom Expense (Optional, in ₹)", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderLight
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dest.isNotBlank()) {
                        val cost = costStr.toDoubleOrNull()
                        onAdd(dest, cost)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Save Trip", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        },
        containerColor = AppLightSurface
    )
}

@Composable
private fun AddFinanceDialog(onDismiss: () -> Unit, onAdd: (String, Double, String, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var isIncome by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Transaction", color = TextDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Expense") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFF5555).copy(alpha = 0.25f))
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Income") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldGreen.copy(alpha = 0.25f))
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Groceries, Dinner, Salary)", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderLight
                    )
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderLight
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (title.isNotBlank() && amt != null && amt > 0) {
                        onAdd(title, amt, category, isIncome)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Text("Record", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        },
        containerColor = AppLightSurface
    )
}
