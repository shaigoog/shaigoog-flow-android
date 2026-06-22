package com.shaigoog.flow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shaigoog.flow.data.OperationDatabase
import com.shaigoog.flow.data.TradeOperation
import java.math.BigDecimal
import java.math.RoundingMode

private enum class Page { TODAY, OPERATIONS, VAULT }
private val currencies = listOf("AED", "USD", "SDG", "EGP", "SAR")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShaigoogFlowApp() {
    val context = LocalContext.current
    val database = remember(context) { OperationDatabase(context.applicationContext) }
    var operations by remember { mutableStateOf(database.list()) }
    var page by remember { mutableStateOf(Page.TODAY) }
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TradeOperation?>(null) }

    fun reload() { operations = database.list() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("سجل شيقوق", fontWeight = FontWeight.Bold)
                        Text("مسار العمليات", style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = page == Page.TODAY,
                    onClick = { page = Page.TODAY },
                    icon = { Icon(Icons.Outlined.Home, null) },
                    label = { Text("اليوم") }
                )
                NavigationBarItem(
                    selected = page == Page.OPERATIONS,
                    onClick = { page = Page.OPERATIONS },
                    icon = { Icon(Icons.Outlined.BusinessCenter, null) },
                    label = { Text("العمليات") }
                )
                NavigationBarItem(
                    selected = page == Page.VAULT,
                    onClick = { page = Page.VAULT },
                    icon = { Icon(Icons.Outlined.Lock, null) },
                    label = { Text("الخزنة") }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("عملية جديدة") }
            )
        }
    ) { padding ->
        when (page) {
            Page.TODAY -> TodayScreen(
                operations = operations,
                modifier = Modifier.padding(padding),
                onShowAll = { page = Page.OPERATIONS },
                onComplete = {
                    database.updateStatus(it.id, "COMPLETED")
                    reload()
                }
            )
            Page.OPERATIONS -> OperationsScreen(
                operations = operations,
                modifier = Modifier.padding(padding),
                onComplete = {
                    database.updateStatus(it.id, "COMPLETED")
                    reload()
                },
                onDelete = { deleteTarget = it }
            )
            Page.VAULT -> VaultScreen(Modifier.padding(padding))
        }
    }

    if (showAdd) {
        AddOperationDialog(
            onDismiss = { showAdd = false },
            onSave = {
                database.insert(it)
                reload()
                showAdd = false
                page = Page.OPERATIONS
            }
        )
    }

    deleteTarget?.let { operation ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("حذف العملية") },
            text = { Text("سيتم حذف عملية ${operation.partyName} من هذا الجهاز.") },
            confirmButton = {
                Button(onClick = {
                    database.delete(operation.id)
                    reload()
                    deleteTarget = null
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun TodayScreen(
    operations: List<TradeOperation>,
    modifier: Modifier,
    onShowAll: () -> Unit,
    onComplete: (TradeOperation) -> Unit
) {
    val active = operations.filter { it.status == "OPEN" }
    val weight = active.sumOf { it.weightGrams }
    val totals = active.groupBy { it.currency }.mapValues { (_, rows) -> rows.sumOf { it.amountMinor } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("مرحباً عبدالله شيقوق", color = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        "كل تجارة في ملف عملية واحد",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "الجهة والبضاعة والوزن والمال، ثم الشحن والمستندات في مسار واضح.",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("قيد التنفيذ", active.size.toString(), Modifier.weight(1f))
                MetricCard("إجمالي الوزن", formatWeight(weight), Modifier.weight(1f))
            }
        }

        item {
            Text("الأموال حسب العملة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("لا يتم جمع العملات المختلفة", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }

        if (totals.isEmpty()) {
            item { EmptyCard("لا توجد مبالغ مسجلة بعد") }
        } else {
            currencies.filter { totals.containsKey(it) }.forEach { currency ->
                item { MoneyCard(currency, totals.getValue(currency)) }
            }
        }

        item {
            Text("العمليات المفتوحة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (active.isEmpty()) {
            item { EmptyCard("لا توجد عمليات مفتوحة") }
        } else {
            items(active.take(3), key = { it.id }) { operation ->
                OperationCard(operation, onComplete = { onComplete(operation) })
            }
            item {
                OutlinedButton(onClick = onShowAll, modifier = Modifier.fillMaxWidth()) {
                    Text("عرض جميع العمليات")
                }
            }
        }
    }
}

@Composable
private fun OperationsScreen(
    operations: List<TradeOperation>,
    modifier: Modifier,
    onComplete: (TradeOperation) -> Unit,
    onDelete: (TradeOperation) -> Unit
) {
    var filter by remember { mutableStateOf("ALL") }
    val visible = operations.filter { filter == "ALL" || it.kind == filter }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = filter == "ALL", onClick = { filter = "ALL" }, label = { Text("الكل") })
            FilterChip(selected = filter == "PURCHASE", onClick = { filter = "PURCHASE" }, label = { Text("شراء") })
            FilterChip(selected = filter == "SALE", onClick = { filter = "SALE" }, label = { Text("بيع") })
        }

        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد عمليات") }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp, 0.dp, 12.dp, 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visible, key = { it.id }) { operation ->
                    OperationCard(
                        operation = operation,
                        onComplete = { onComplete(operation) },
                        onDelete = { onDelete(operation) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultScreen(modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 110.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Lock, null, modifier = Modifier.size(36.dp))
                    Text("الخزنة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("ستجمع المستندات والنسخ الاحتياطي والبصمة في مكان واحد.")
                    HorizontalDivider()
                    Text("المرحلة القادمة: المستندات، جهات الاتصال، البصمة والنسخ اليدوي.")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MoneyCard(currency: String, amountMinor: Long) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(currencyName(currency), fontWeight = FontWeight.Bold)
            Text("${formatMoney(amountMinor)} $currency", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Text(text, Modifier.fillMaxWidth().padding(18.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun OperationCard(
    operation: TradeOperation,
    onComplete: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(operation.partyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(operation.productName, color = MaterialTheme.colorScheme.secondary)
                }
                Text(if (operation.kind == "PURCHASE") "شراء" else "بيع", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text("${formatWeight(operation.weightGrams)}  •  ${formatMoney(operation.amountMinor)} ${operation.currency}")
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (operation.status == "COMPLETED") "مكتملة" else "مفتوحة", fontWeight = FontWeight.Bold)
                Row {
                    if (operation.status == "OPEN") {
                        TextButton(onClick = onComplete) {
                            Icon(Icons.Outlined.CheckCircle, null)
                            Text("إكمال")
                        }
                    }
                    onDelete?.let {
                        IconButton(onClick = it) { Icon(Icons.Outlined.Delete, "حذف") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddOperationDialog(
    onDismiss: () -> Unit,
    onSave: (TradeOperation) -> Unit
) {
    var kind by remember { mutableStateOf("PURCHASE") }
    var party by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("TON") }
    var bagWeight by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("AED") }
    var amount by remember { mutableStateOf("") }

    val weightGrams = calculateWeight(quantity, unit, bagWeight)
    val amountMinor = calculateMoney(amount)
    val valid = party.isNotBlank() && product.isNotBlank() && weightGrams != null && amountMinor != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ملف عملية جديدة") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == "PURCHASE", onClick = { kind = "PURCHASE" }, label = { Text("شراء") })
                    FilterChip(selected = kind == "SALE", onClick = { kind = "SALE" }, label = { Text("بيع") })
                }
                OutlinedTextField(party, { party = it }, label = { Text("اسم الجهة *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(product, { product = it }, label = { Text("البضاعة *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("وحدة الوزن", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = unit == "TON", onClick = { unit = "TON" }, label = { Text("طن") })
                    FilterChip(selected = unit == "KG", onClick = { unit = "KG" }, label = { Text("كجم") })
                    FilterChip(selected = unit == "BAG", onClick = { unit = "BAG" }, label = { Text("جوال") })
                }
                OutlinedTextField(quantity, { quantity = decimalInput(it) }, label = { Text(if (unit == "BAG") "عدد الجوالات *" else "الكمية *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (unit == "BAG") {
                    OutlinedTextField(bagWeight, { bagWeight = decimalInput(it) }, label = { Text("وزن الجوال بالكيلوجرام *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                Text("العملة", fontWeight = FontWeight.Bold)
                currencies.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { code ->
                            FilterChip(selected = currency == code, onClick = { currency = code }, label = { Text(code) })
                        }
                    }
                }
                OutlinedTextField(amount, { amount = decimalInput(it) }, label = { Text("قيمة العملية *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                weightGrams?.let { Text("الوزن المحسوب: ${formatWeight(it)}", color = MaterialTheme.colorScheme.secondary) }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        TradeOperation(
                            kind = kind,
                            partyName = party.trim(),
                            productName = product.trim(),
                            weightGrams = weightGrams!!,
                            currency = currency,
                            amountMinor = amountMinor!!
                        )
                    )
                }
            ) { Text("حفظ العملية") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

private fun calculateWeight(quantity: String, unit: String, bagWeight: String): Long? {
    val q = quantity.toBigDecimalOrNull() ?: return null
    if (q <= BigDecimal.ZERO) return null
    val grams = when (unit) {
        "TON" -> q * BigDecimal("1000000")
        "KG" -> q * BigDecimal("1000")
        "BAG" -> {
            val bag = bagWeight.toBigDecimalOrNull() ?: return null
            if (bag <= BigDecimal.ZERO) return null
            q * bag * BigDecimal("1000")
        }
        else -> return null
    }
    return grams.setScale(0, RoundingMode.HALF_UP).longValueExact()
}

private fun calculateMoney(amount: String): Long? {
    val value = amount.toBigDecimalOrNull() ?: return null
    if (value < BigDecimal.ZERO) return null
    return (value * BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact()
}

private fun decimalInput(value: String): String {
    val cleaned = value.replace('٫', '.').replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    return if (dot < 0) cleaned else cleaned.substring(0, dot + 1) + cleaned.substring(dot + 1).replace(".", "")
}

private fun formatMoney(value: Long): String = BigDecimal(value)
    .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()

private fun formatWeight(value: Long): String = BigDecimal(value)
    .divide(BigDecimal("1000000"), 3, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString() + " طن"

private fun currencyName(code: String): String = when (code) {
    "AED" -> "الدرهم الإماراتي"
    "USD" -> "الدولار الأمريكي"
    "SDG" -> "الجنيه السوداني"
    "EGP" -> "الجنيه المصري"
    "SAR" -> "الريال السعودي"
    else -> code
}
