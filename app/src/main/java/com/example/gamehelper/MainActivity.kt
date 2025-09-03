package com.example.gamehelper

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehelper.ui.theme.GameHelperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameHelperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AutoClickerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoClickerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 状态变量
    var xCoordinate by remember { mutableStateOf("500") }
    var yCoordinate by remember { mutableStateOf("500") }
    var clickInterval by remember { mutableStateOf("1000") }
    var isClicking by remember { mutableStateOf(false) }
    var isPreviewShowing by remember { mutableStateOf(false) }

    // 检查无障碍服务状态
    val isAccessibilityEnabled = remember {
        mutableStateOf(isAccessibilityServiceEnabled())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(
            text = "自动连点器",
            fontSize = 24.sp,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // X坐标输入
        OutlinedTextField(
            value = xCoordinate,
            onValueChange = { xCoordinate = it },
            label = { Text("X坐标") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Y坐标输入
        OutlinedTextField(
            value = yCoordinate,
            onValueChange = { yCoordinate = it },
            label = { Text("Y坐标") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // 点击间隔输入
        OutlinedTextField(
            value = clickInterval,
            onValueChange = { clickInterval = it },
            label = { Text("点击间隔(毫秒)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 设置位置按钮
        Button(
            onClick = {
                val x = xCoordinate.toFloatOrNull() ?: 500f
                val y = yCoordinate.toFloatOrNull() ?: 500f
                val interval = clickInterval.toLongOrNull() ?: 1000L

                AutoClickService.instance?.setClickPosition(x, y)
                AutoClickService.instance?.setClickInterval(interval)

                Toast.makeText(context, "位置已设置: ($x, $y)", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("设置点击位置")
        }

        // 预览按钮
        Button(
            onClick = {
                if (!isAccessibilityEnabled.value) {
                    Toast.makeText(context, "请先启用无障碍服务", Toast.LENGTH_LONG).show()
                    return@Button
                }

                if (AutoClickService.instance == null) {
                    Toast.makeText(context, "无障碍服务未连接", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (isPreviewShowing) {
                    AutoClickService.instance?.hidePreview()
                    isPreviewShowing = false
                    Toast.makeText(context, "已隐藏坐标预览", Toast.LENGTH_SHORT).show()
                } else {
                    // 先设置坐标
                    val x = xCoordinate.toFloatOrNull() ?: 500f
                    val y = yCoordinate.toFloatOrNull() ?: 500f
                    AutoClickService.instance?.setClickPosition(x, y)

                    // 显示预览
                    AutoClickService.instance?.showPreview()
                    isPreviewShowing = true
                    Toast.makeText(context, "已显示坐标预览，红色圆点即为点击位置", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPreviewShowing) MaterialTheme.colorScheme.secondary
                               else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isPreviewShowing) "隐藏坐标预览" else "显示坐标预览")
        }

        // 开始/停止按钮
        Button(
            onClick = {
                if (!isAccessibilityEnabled.value) {
                    Toast.makeText(context, "请先启用无障碍服务", Toast.LENGTH_LONG).show()
                    return@Button
                }

                if (AutoClickService.instance == null) {
                    Toast.makeText(context, "无障碍服务未连接", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (isClicking) {
                    AutoClickService.instance?.stopClicking()
                    isClicking = false
                } else {
                    // 更新参数
                    val x = xCoordinate.toFloatOrNull() ?: 500f
                    val y = yCoordinate.toFloatOrNull() ?: 500f
                    val interval = clickInterval.toLongOrNull() ?: 1000L

                    AutoClickService.instance?.setClickPosition(x, y)
                    AutoClickService.instance?.setClickInterval(interval)
                    AutoClickService.instance?.startClicking()
                    isClicking = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isClicking) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isClicking) "停止点击" else "开始点击")
        }

        // 启用无障碍服务按钮
        if (!isAccessibilityEnabled.value) {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("启用无障碍服务")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 使用说明
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "使用说明：",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. 首次使用需要启用无障碍服务\n" +
                           "2. 设置要点击的屏幕坐标(X, Y)\n" +
                           "3. 点击「显示坐标预览」查看点击位置\n" +
                           "4. 设置点击间隔时间(毫秒)\n" +
                           "5. 点击开始按钮开始自动点击\n" +
                           "6. 点击停止按钮停止自动点击",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    // 监听点击状态变化
    LaunchedEffect(Unit) {
        while (true) {
            isClicking = AutoClickService.isClicking
            isAccessibilityEnabled.value = isAccessibilityServiceEnabled()
            kotlinx.coroutines.delay(500)
        }
    }
}

/**
 * 检查无障碍服务是否已启用
 */
private fun isAccessibilityServiceEnabled(): Boolean {
    return AutoClickService.instance != null
}

@Preview(showBackground = true)
@Composable
fun AutoClickerScreenPreview() {
    GameHelperTheme {
        AutoClickerScreen()
    }
}