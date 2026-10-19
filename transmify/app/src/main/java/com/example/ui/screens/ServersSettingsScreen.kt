package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.db.ServerEntity
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.RadiantRed
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.VibrantAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersSettingsScreen(
    servers: List<ServerEntity>,
    activeServer: ServerEntity?,
    themeMode: ThemeMode,
    notificationsEnabled: Boolean,
    rutrackerMirror: String,
    onSelectServer: (ServerEntity) -> Unit,
    onSaveServer: (ServerEntity) -> Unit,
    onDeleteServer: (ServerEntity) -> Unit,
    onTestConnection: suspend (ServerEntity) -> Result<String>,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    onUpdateRutrackerMirror: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverToEdit by remember { mutableStateOf<ServerEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    // Notification permission launcher for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            onNotificationsChange(true)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Серверы и настройки",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Transmission Servers
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Серверы Transmission",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            serverToEdit = ServerEntity(
                                name = "Новый сервер",
                                host = "198.51.100.10",
                                port = 9091,
                                path = "/transmission/rpc",
                                isActive = false,
                                isDemoMode = false
                            )
                            isCreatingNew = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Server items
            items(servers, key = { it.id }) { server ->
                ServerItemCard(
                    server = server,
                    isActive = server.id == activeServer?.id,
                    onSelect = { onSelectServer(server) },
                    onEdit = {
                        serverToEdit = server
                        isCreatingNew = false
                    },
                    onDelete = { onDeleteServer(server) }
                )
            }

            // Section: Appearance / Theme
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Тема оформления (MD3 Monet / Dynamic Color)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Выберите режим отображения",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeModeChip(
                                label = "Темная",
                                icon = Icons.Default.DarkMode,
                                isSelected = themeMode == ThemeMode.DARK,
                                onClick = { onThemeModeChange(ThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeChip(
                                label = "Светлая",
                                icon = Icons.Default.LightMode,
                                isSelected = themeMode == ThemeMode.LIGHT,
                                onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeModeChip(
                                label = "Система",
                                icon = Icons.Default.SettingsBrightness,
                                isSelected = themeMode == ThemeMode.SYSTEM,
                                onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section: Download Completion Notifications
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Уведомления о завершении",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (notificationsEnabled) ElectricCyan else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Уведомлять о завершении",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Показывать системное уведомление, когда торрент завершит загрузку",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onNotificationsChange(enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan)
                            )
                        }

                        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Предоставить разрешение на уведомления в Android")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FilledTonalButton(
                            onClick = onSendTestNotification,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Отправить тестовое уведомление")
                        }
                    }
                }
            }

            // Section: RuTracker Settings
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Настройки RuTracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                var tempMirror by remember(rutrackerMirror) { mutableStateOf(rutrackerMirror) }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "URL зеркала для поиска",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempMirror,
                            onValueChange = { tempMirror = it },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (tempMirror != rutrackerMirror) {
                                    IconButton(onClick = { onUpdateRutrackerMirror(tempMirror) }) {
                                        Icon(Icons.Default.Check, contentDescription = "Сохранить", tint = NeonEmerald)
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickMirrorChip(
                                label = "rutracker.org",
                                isCurrent = rutrackerMirror == "https://rutracker.org",
                                onClick = {
                                    tempMirror = "https://rutracker.org"
                                    onUpdateRutrackerMirror("https://rutracker.org")
                                }
                            )
                            QuickMirrorChip(
                                label = "rutracker.net",
                                isCurrent = rutrackerMirror == "https://rutracker.net",
                                onClick = {
                                    tempMirror = "https://rutracker.net"
                                    onUpdateRutrackerMirror("https://rutracker.net")
                                }
                            )
                            QuickMirrorChip(
                                label = "rutracker.org.app",
                                isCurrent = rutrackerMirror == "https://rutracker.org.app",
                                onClick = {
                                    tempMirror = "https://rutracker.org.app"
                                    onUpdateRutrackerMirror("https://rutracker.org.app")
                                }
                            )
                        }
                    }
                }
            }

            // About Footer
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Transmify Remote v1.0.0",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Material Design 3 Expressive • Transmission RPC 17+",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    // Edit / Create Server Dialog
    serverToEdit?.let { currentServer ->
        ServerEditDialog(
            server = currentServer,
            isNew = isCreatingNew,
            onDismiss = { serverToEdit = null },
            onSave = { updated ->
                onSaveServer(updated)
                serverToEdit = null
            },
            onTest = { s -> onTestConnection(s) }
        )
    }
}

@Composable
private fun ServerItemCard(
    server: ServerEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) ElectricCyan.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Dns,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = server.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonEmerald.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Активен", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                            }
                        }
                    }
                    Text(
                        text = server.displayUrl,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = RadiantRed.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun QuickMirrorChip(
    label: String,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ServerEditDialog(
    server: ServerEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (ServerEntity) -> Unit,
    onTest: suspend (ServerEntity) -> Result<String>
) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(server.name) }
    var host by remember { mutableStateOf(server.host) }
    var port by remember { mutableStateOf(server.port.toString()) }
    var path by remember { mutableStateOf(server.path) }
    var useHttps by remember { mutableStateOf(server.useHttps) }
    var username by remember { mutableStateOf(server.username) }
    var password by remember { mutableStateOf(server.password) }
    var isDemoMode by remember { mutableStateOf(server.isDemoMode) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testIsSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Добавить сервер" else "Редактировать сервер") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название сервера") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDemoMode = !isDemoMode },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isDemoMode,
                            onCheckedChange = { isDemoMode = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Режим песочницы (симуляция)", fontSize = 13.sp)
                    }
                }
                if (!isDemoMode) {
                    item {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("IP адрес или хост") },
                            placeholder = { Text("198.51.100.10") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = port,
                                onValueChange = { port = it },
                                label = { Text("Порт") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = path,
                                onValueChange = { path = it },
                                label = { Text("RPC путь") },
                                singleLine = true,
                                modifier = Modifier.weight(1.5f)
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { useHttps = !useHttps },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(checked = useHttps, onCheckedChange = { useHttps = it })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Использовать HTTPS (SSL)", fontSize = 13.sp)
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Имя пользователя (опционально)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Пароль (опционально)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Test Connection Button
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            val temp = server.copy(
                                name = name,
                                host = host,
                                port = port.toIntOrNull() ?: 9091,
                                path = path,
                                useHttps = useHttps,
                                username = username,
                                password = password,
                                isDemoMode = isDemoMode
                            )
                            isTesting = true
                            testResult = null
                            scope.launch {
                                val res = onTest(temp)
                                isTesting = false
                                if (res.isSuccess) {
                                    testResult = res.getOrNull()
                                    testIsSuccess = true
                                } else {
                                    testResult = res.exceptionOrNull()?.message ?: "Ошибка"
                                    testIsSuccess = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Проверка...")
                        } else {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Проверить соединение")
                        }
                    }
                }

                testResult?.let { result ->
                    item {
                        Text(
                            text = result,
                            color = if (testIsSuccess) NeonEmerald else RadiantRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = server.copy(
                        name = name.ifBlank { "Transmission Server" },
                        host = host.ifBlank { "127.0.0.1" },
                        port = port.toIntOrNull() ?: 9091,
                        path = path.ifBlank { "/transmission/rpc" },
                        useHttps = useHttps,
                        username = username,
                        password = password,
                        isDemoMode = isDemoMode
                    )
                    onSave(updated)
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
