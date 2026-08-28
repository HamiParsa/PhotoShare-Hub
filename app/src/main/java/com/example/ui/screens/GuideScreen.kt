package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.ServerInfo
import com.example.ui.theme.SuccessGreen

@Composable
fun GuideScreen(
    serverInfo: ServerInfo,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isPersian = language == AppLanguage.FA

    val tabTitles = if (isPersian) {
        listOf("اتصال به کامپیوتر", "معماری و تکنولوژی", "دستورات ترمینال")
    } else {
        listOf("Desktop & Browser", "Tech & Architecture", "Terminal & Scripts")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isPersian) "راهنما و معماری سیستم" else "Setup Guide & Architecture",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Browser & Explorer Guide
                    item {
                        GuideSectionCard(
                            title = if (isPersian) "۱. دسترسی از طریق مرورگر وب" else "1. Web Browser Access",
                            subtitle = if (isPersian) "سریع‌ترین و آسان‌ترین روش برای تمام سیستم‌عامل‌ها (Windows, Mac, Linux)" else "Fastest method for all operating systems (Chrome, Edge, Safari, Firefox)",
                            icon = Icons.Default.Language,
                            iconTint = MaterialTheme.colorScheme.primary
                        ) {
                            GuideStep(
                                num = "1",
                                text = if (isPersian) "گوشی و کامپیوتر خود را به یک شبکه وای‌فای مشترک متصل کنید." else "Connect both your Android phone and desktop/laptop to the same Wi-Fi network."
                            )
                            GuideStep(
                                num = "2",
                                text = if (isPersian) "در برنامه، دکمه 'شروع سرور' را بزنید یا قابلیت همگام‌سازی خودکار را فعال بگذارید." else "Tap 'Start Server' on the app dashboard or keep Auto-Sync enabled."
                            )
                            GuideStep(
                                num = "3",
                                text = if (isPersian) "آدرس زیر را در نوار آدرس مرورگر رایانه وارد کنید:" else "Type this URL directly into your desktop browser address bar:"
                            )

                            CodeSnippetBox(code = serverInfo.fullUrl)

                            GuideStep(
                                num = "4",
                                text = if (isPersian) "گالری وب با قابلیت پیش‌نمایش، فیلتر آلبوم، جستجو و دانلود گروهی ZIP در اختیارتان خواهد بود." else "Enjoy responsive gallery grid, full-res lightbox previews, album filters, and 1-click ZIP downloads!"
                            )
                        }
                    }

                    item {
                        GuideSectionCard(
                            title = if (isPersian) "۲. اتصال به Windows File Explorer" else "2. Windows File Explorer & Scripts",
                            subtitle = if (isPersian) "مشاهده یا دانلود خودکار تصاویر از طریق ویندوز" else "Browse files directly or automate photo transfers on Windows",
                            icon = Icons.Default.Computer,
                            iconTint = MaterialTheme.colorScheme.primary
                        ) {
                            GuideStep(
                                num = "A",
                                text = if (isPersian) "دانلود فایل زیپ کامل آلبوم‌ها مستقیماً در دانلود منیجر یا مرورگر ویندوز:" else "Direct bulk download link for all photos via browser or download manager:"
                            )
                            CodeSnippetBox(code = "${serverInfo.fullUrl}/api/zip")

                            GuideStep(
                                num = "B",
                                text = if (isPersian) "مشاهده لیست JSON تصاویر با تمام مشخصات فنی جهت استفاده در اسکریپت‌ها:" else "Fetch raw photo metadata JSON array for custom scripting:"
                            )
                            CodeSnippetBox(code = "${serverInfo.fullUrl}/api/photos")
                        }
                    }
                }

                1 -> {
                    // Tech & Architecture
                    item {
                        GuideSectionCard(
                            title = if (isPersian) "پیشنهاد پشته تکنولوژی (Tech Stack)" else "Tech Stack & Framework Recommendation",
                            subtitle = if (isPersian) "بررسی عملکرد و معماری در پلتفرم‌های مختلف" else "Analysis of Native Android, Flutter, and React Native implementations",
                            icon = Icons.Default.Hub,
                            iconTint = MaterialTheme.colorScheme.primary
                        ) {
                            TechComparisonRow(
                                framework = "Native Kotlin + Jetpack Compose",
                                rating = "⭐⭐⭐⭐⭐ (Recommended)",
                                pros = "Direct MediaStore Cursor access without IPC bridges, zero overhead ServerSocket HTTP streaming, native Foreground Service dataSync lifecycle.",
                                isPersian = isPersian
                            )

                            TechComparisonRow(
                                framework = "Flutter (Cross-Platform)",
                                rating = "⭐⭐⭐⭐",
                                pros = "Single UI codebase, requires `flutter_foreground_task` and `shelf` embedded HTTP package. Good cross-platform performance.",
                                isPersian = isPersian
                            )

                            TechComparisonRow(
                                framework = "React Native",
                                rating = "⭐⭐⭐",
                                pros = "Requires native bridge (`react-native-http-bridge` or `react-native-background-actions`). High memory overhead when streaming large video/photo buffers across JS bridge.",
                                isPersian = isPersian
                            )
                        }
                    }

                    item {
                        GuideSectionCard(
                            title = if (isPersian) "جریان داده و معماری سرویس پس‌زمینه" else "Background Service & Data Flow Architecture",
                            subtitle = if (isPersian) "نحوه کارکرد بدون وقفه سرور و همگام‌سازی خودکار" else "How Foreground Service, NetworkCallback, and LocalHttpServer work together",
                            icon = Icons.Default.Code,
                            iconTint = MaterialTheme.colorScheme.primary
                        ) {
                            ArchitectureComponent(
                                title = "1. NetworkCallback Observer",
                                desc = "Continuously listens for Wi-Fi state transitions using Android ConnectivityManager. When the device joins a LAN, it detects IPv4 interface addresses and triggers auto-sync."
                            )
                            ArchitectureComponent(
                                title = "2. Foreground Service (dataSync)",
                                desc = "Runs `LocalSharingService` with a low-priority ongoing notification and partial CPU WakeLock, preventing Android OS Doze mode from freezing active transfers."
                            )
                            ArchitectureComponent(
                                title = "3. Embedded LocalHttpServer",
                                desc = "Coroutines-based multi-threaded ServerSocket handling HTTP/1.1 GET/HEAD, Range headers, mime types, JSON endpoints, and real-time ZipOutputStream compression on-the-fly."
                            )
                            ArchitectureComponent(
                                title = "4. GalleryRepository (MediaStore)",
                                desc = "Direct content resolver queries to `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` with indexed metadata, album bucketing, and on-demand thumbnail generation."
                            )
                        }
                    }
                }

                2 -> {
                    // Terminal & Automation Scripts
                    item {
                        GuideSectionCard(
                            title = if (isPersian) "اسکریپت دانلود سریع با PowerShell و cURL" else "Quick Automation & PowerShell Scripts",
                            subtitle = if (isPersian) "دستورات آماده برای دانلود خودکار تصاویر روی رایانه" else "Ready-to-use commands to backup photos to your PC via terminal",
                            icon = Icons.Default.Terminal,
                            iconTint = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Windows PowerShell: Download all photos as ZIP",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            CodeSnippetBox(
                                code = "Invoke-WebRequest -Uri \"${serverInfo.fullUrl}/api/zip\" -OutFile \"phone_photos.zip\""
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "cURL (macOS / Linux / Windows Terminal):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            CodeSnippetBox(
                                code = "curl -O -J \"${serverInfo.fullUrl}/api/zip\""
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Test Server Health Ping:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            CodeSnippetBox(
                                code = "curl \"${serverInfo.fullUrl}/api/ping\""
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideSectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
fun GuideStep(num: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = num,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CodeSnippetBox(code: String) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Command", code))
                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun TechComparisonRow(
    framework: String,
    rating: String,
    pros: String,
    isPersian: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = framework,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = rating,
                style = MaterialTheme.typography.labelSmall,
                color = SuccessGreen
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = pros,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ArchitectureComponent(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

