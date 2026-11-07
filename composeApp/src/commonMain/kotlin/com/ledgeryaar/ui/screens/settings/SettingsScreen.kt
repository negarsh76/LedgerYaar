package com.ledgeryaar.ui.screens.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgeryaar.data.repository.AppTheme
import com.ledgeryaar.data.repository.CurrencyOption
import com.ledgeryaar.domain.model.CustomCategory
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.i18n.AppLanguage
import com.ledgeryaar.i18n.LocalStrings
import com.ledgeryaar.i18n.t
import com.ledgeryaar.ui.components.ConfirmDeleteDialog
import com.ledgeryaar.ui.components.LedgerCard
import com.ledgeryaar.ui.components.NewCategoryDialog
import com.ledgeryaar.ui.components.RowSpaceBetween
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val strings = LocalStrings.current
    val settings by viewModel.settings.collectAsState()
    var newCategoryType by remember { mutableStateOf<TxType?>(null) }
    var deleting by remember { mutableStateOf<CustomCategory?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(strings.navSettings, style = MaterialTheme.typography.headlineMedium)

        LedgerCard {
            Text(strings.language, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = settings.language == AppLanguage.FA,
                    onClick = { viewModel.setLanguage(AppLanguage.FA) },
                    label = { Text("فارسی") }
                )
                FilterChip(
                    selected = settings.language == AppLanguage.EN,
                    onClick = { viewModel.setLanguage(AppLanguage.EN) },
                    label = { Text("English") }
                )
            }
        }

        LedgerCard {
            Text(strings.t("تم", "Theme"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    AppTheme.LIGHT to strings.t("کاغذی روشن", "Paper light"),
                    AppTheme.DARK to strings.t("تیره", "Dark"),
                    AppTheme.FOREST to strings.t("🌿 جنگلی", "🌿 Forest")
                ).forEach { (th, label) ->
                    FilterChip(selected = settings.theme == th, onClick = { viewModel.setTheme(th) }, label = { Text(label) })
                }
            }
        }

        BankConnectionCard(viewModel)

        EmailBackupCard(viewModel)

        LedgerCard {
            Text(strings.t("واحد پول", "Currency"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CurrencyOption.values().forEach { opt ->
                    FilterChip(
                        selected = settings.currency == opt,
                        onClick = { viewModel.setCurrency(opt) },
                        label = { Text(strings.t(opt.fa, opt.en)) }
                    )
                }
            }
        }

        LedgerCard {
            Text(strings.t("واحد پیش‌فرض ورود مبلغ", "Default amount unit"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                strings.t("با انتخاب «میلیون»، اگر ۵ بنویسی ۵,۰۰۰,۰۰۰ ثبت می‌شود (صفرها خودکار اضافه می‌شوند). داخل هر فرم هم می‌شود عوضش کرد.",
                    "With “Million”, typing 5 saves 5,000,000 (zeros are added automatically). You can also change it inside each form."),
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
            )
            Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    1L to strings.t("عادی", "×1"),
                    1_000L to strings.t("هزار", "Thousand"),
                    1_000_000L to strings.t("میلیون", "Million"),
                    1_000_000_000L to strings.t("میلیارد", "Billion")
                ).forEach { (mult, label) ->
                    FilterChip(
                        selected = settings.inputUnit == mult,
                        onClick = { viewModel.setInputUnit(mult) },
                        label = { Text(label) }
                    )
                }
            }
        }

        LedgerCard {
            Text(strings.t("دسته‌بندی‌های من", "My categories"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                strings.t("دسته‌های دلخواه کنار دسته‌های پیش‌فرض در همهٔ بخش‌ها (درآمد، هزینه، بودجه) دیده می‌شوند.",
                    "Custom categories appear next to the defaults everywhere (income, expenses, budgets)."),
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
            )
            listOf(TxType.EXPENSE, TxType.INCOME).forEach { type ->
                Spacer(Modifier.height(10.dp))
                RowSpaceBetween {
                    Text(
                        if (type == TxType.EXPENSE) strings.t("هزینه", "Expense") else strings.t("درآمد", "Income"),
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                    )
                    TextButton(onClick = { newCategoryType = type }) { Text(strings.t("+ افزودن", "+ Add")) }
                }
                val list = settings.customCategories.filter { it.type == type }
                if (list.isEmpty()) {
                    Text(strings.t("هنوز دسته‌ای نساخته‌ای.", "No custom categories yet."), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    list.forEach { cat ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cat.name, fontSize = 13.sp)
                            TextButton(onClick = { deleting = cat }) { Text(strings.delete, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }

        LedgerCard {
            Text(strings.aboutApp, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(strings.aboutAppBody, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }

        LedgerCard {
            Text(strings.calendarInfo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(strings.calendarInfoBody, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }

    newCategoryType?.let { type ->
        NewCategoryDialog(
            type = type, onDismiss = { newCategoryType = null },
            onConfirm = { name -> viewModel.addCategory(name, type); newCategoryType = null }
        )
    }
    deleting?.let { cat ->
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deleteCategory(cat.id); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun StatusLine(ok: Boolean, text: String) {
    Text(
        (if (ok) "✓ " else "✗ ") + text,
        fontSize = 12.sp,
        color = if (ok) com.ledgeryaar.ui.theme.AppColors.LedgerGreen else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** اتصال به پیامک و نوتیف بانک‌ها (بلو، ملت، ملی، …) */
@Composable
private fun BankConnectionCard(viewModel: SettingsViewModel) {
    val strings = LocalStrings.current
    val settings by viewModel.settings.collectAsState()
    val perms by viewModel.permissions.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshPermissions() }

    LedgerCard {
        RowSpaceBetween {
            Text(strings.t("ثبت خودکار از پیامک و نوتیف بانک", "Auto-import from bank SMS & notifications"), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Switch(checked = settings.autoImport, onCheckedChange = { viewModel.setAutoImport(it) })
        }
        if (!perms.smsSupported) {
            Text(
                strings.t("روی آیفون، اپل اجازهٔ خواندن پیامک یا نوتیف برنامه‌های دیگر را نمی‌دهد؛ این قابلیت فقط روی اندروید کار می‌کند.",
                    "iOS doesn't allow reading SMS or other apps' notifications; this works on Android only."),
                fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)
            )
            return@LedgerCard
        }
        Text(
            strings.t("هر پیامک یا نوتیف برداشت/واریز که برسد، خودش ثبت می‌شود و یک نوتیف می‌آید که بنویسی بابت چی بوده.",
                "Every withdrawal/deposit SMS or bank notification is recorded automatically, and a notification asks what it was for."),
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
        )
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RowSpaceBetween {
                StatusLine(perms.smsGranted, strings.t("خواندن پیامک بانک‌ها", "Read bank SMS"))
                if (!perms.smsGranted) TextButton(onClick = { viewModel.requestSms() }) { Text(strings.t("اجازه", "Allow")) }
            }
            RowSpaceBetween {
                StatusLine(perms.notificationListenerEnabled, strings.t("نوتیف اپ‌های بانکی (بلو و …)", "Bank app notifications (Blu, …)"))
                if (!perms.notificationListenerEnabled) TextButton(onClick = { viewModel.openNotificationAccess() }) { Text(strings.t("روشن کن", "Enable")) }
            }
            RowSpaceBetween {
                StatusLine(perms.postNotificationsGranted, strings.t("نمایش نوتیف روی گوشی", "Show notifications"))
                if (!perms.postNotificationsGranted) TextButton(onClick = { viewModel.requestPostNotifications() }) { Text(strings.t("اجازه", "Allow")) }
            }
        }
        Text(
            strings.t("اگر اندروید گفت «تنظیمات محدود شده»: تنظیمات گوشی ← برنامه‌ها ← دفتر یار ← منوی ⋮ بالا ← «اجازه به تنظیمات محدود».",
                "If Android says \"Restricted setting\": Settings → Apps → LedgerYaar → ⋮ menu → \"Allow restricted settings\"."),
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)
        )
        RowSpaceBetween(Modifier.padding(top = 8.dp)) {
            Text(strings.t("اگر واحد در پیامک نبود، ریال فرض کن", "Assume Rial when SMS has no unit"), fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(checked = settings.bankDefaultRial, onCheckedChange = { viewModel.setBankDefaultRial(it) })
        }
        if (settings.learnedRules.isNotEmpty()) {
            Text(strings.t("چیزهایی که یاد گرفته", "What it has learned"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            settings.learnedRules.entries.take(30).forEach { (kw, cat) ->
                RowSpaceBetween {
                    Text("$kw ← ${com.ledgeryaar.ui.categoryName(cat)}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.forgetRule(kw) }) { Text(strings.delete, fontSize = 11.sp) }
                }
            }
        }
    }
}

/** پشتیبان‌گیری به ایمیل و بازیابی از فایل */
@Composable
private fun EmailBackupCard(viewModel: SettingsViewModel) {
    val strings = LocalStrings.current
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.backupMessage.collectAsState()
    var email by remember(settings.backupEmail) { mutableStateOf(settings.backupEmail) }
    var confirmRestore by remember { mutableStateOf(false) }

    LedgerCard {
        Text(strings.t("پشتیبان روی ایمیل", "Backup to email"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            strings.t("یک فایل از همهٔ اطلاعاتت ساخته می‌شود و برنامهٔ ایمیل با همین فایل پیوست باز می‌شود؛ فقط «ارسال» را بزن. هر وقت گوشی عوض شد، فایل را از ایمیلت بردار و «بازیابی» بزن.",
                "A file with all your data is attached to a new email — just hit send. On a new phone, download it from your inbox and tap Restore."),
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
        )
        OutlinedTextField(
            value = email, onValueChange = { email = it }, singleLine = true,
            label = { Text(strings.t("ایمیل", "Email")) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = email.contains("@"),
                onClick = {
                    viewModel.sendBackup(
                        email,
                        strings.t("پشتیبان دفتر یار", "LedgerYaar backup"),
                        strings.t("فایل پیوست پشتیبان کامل دفتر یار است. برای بازیابی: تنظیمات ← بازیابی از فایل.",
                            "The attached file is a full LedgerYaar backup. To restore: Settings → Restore from file.")
                    )
                }
            ) { Text(strings.t("ارسال پشتیبان", "Send backup")) }
            OutlinedButton(onClick = { confirmRestore = true }) { Text(strings.t("بازیابی از فایل", "Restore from file")) }
        }
        if (settings.lastBackupAt > 0) {
            val d = kotlinx.datetime.Instant.fromEpochMilliseconds(settings.lastBackupAt)
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
            Text(
                strings.t("آخرین پشتیبان: ", "Last backup: ") + com.ledgeryaar.domain.model.PersianCalendarConverter.toJalali(d).displayFull,
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)
            )
        }
        message?.let { Text(it, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(strings.t("بازیابی پشتیبان", "Restore backup")) },
            text = { Text(strings.t("همهٔ اطلاعات فعلی با محتوای فایل جایگزین می‌شود. ادامه می‌دهی؟", "All current data will be replaced by the file's content. Continue?")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    viewModel.restoreFromFile(
                        ok = { n -> strings.t("بازیابی شد ✓ ($n تراکنش)", "Restored ✓ ($n transactions)") },
                        failed = strings.t("این فایل پشتیبان دفتر یار نیست یا خراب است.", "Not a valid LedgerYaar backup file.")
                    )
                }) { Text(strings.t("بازیابی", "Restore")) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text(strings.cancel) } }
        )
    }
}
