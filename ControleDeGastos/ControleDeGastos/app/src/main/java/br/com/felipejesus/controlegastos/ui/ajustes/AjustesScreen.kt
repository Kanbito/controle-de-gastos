package br.com.felipejesus.controlegastos.ui.ajustes

import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.felipejesus.controlegastos.BuildConfig
import br.com.felipejesus.controlegastos.R
import br.com.felipejesus.controlegastos.data.Tema
import br.com.felipejesus.controlegastos.notificacoes.CapturaNotificacoesService
import br.com.felipejesus.controlegastos.ui.MainViewModel
import br.com.felipejesus.controlegastos.util.ExportadorExcel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(vm: MainViewModel, onPendentes: () -> Unit) {
    val tema by vm.tema.collectAsStateWithLifecycle()
    val captura by vm.capturaNotificacoes.collectAsStateWithLifecycle()
    val qtdPendentes by vm.qtdPendentes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var acessoConcedido by remember { mutableStateOf(CapturaNotificacoesService.acessoConcedido(context)) }
    // Ao voltar das Configurações do Android, verifica de novo se o acesso foi liberado.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        acessoConcedido = CapturaNotificacoesService.acessoConcedido(context)
    }

    val exportarTudo = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportadorExcel.MIME_XLSX)
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        escopo.launch {
            val ok = runCatching {
                val dados = vm.dadosExportacao(null)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { ExportadorExcel.escrever(it, dados) }
                        ?: error("sem acesso")
                }
            }.isSuccess
            snackbar.showSnackbar(if (ok) "Histórico exportado com sucesso." else "Não foi possível exportar.")
        }
    }

    fun abrirConfiguracoesNotificacao() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            CapturaNotificacoesService.intentConfiguracoes(context)
        } else {
            CapturaNotificacoesService.intentConfiguracoesGeral()
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(CapturaNotificacoesService.intentConfiguracoesGeral())
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text("Ajustes") }) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Secao("Aparência")
            listOf(
                Triple(Tema.SISTEMA, "Seguir o sistema", Icons.Filled.BrightnessAuto),
                Triple(Tema.CLARO, "Tema claro", Icons.Filled.LightMode),
                Triple(Tema.ESCURO, "Tema escuro", Icons.Filled.DarkMode),
            ).forEach { (t, rotulo, icone) ->
                ListItem(
                    headlineContent = { Text(rotulo) },
                    leadingContent = { Icon(icone, contentDescription = null) },
                    trailingContent = { RadioButton(selected = tema == t, onClick = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(selected = tema == t, role = Role.RadioButton, onClick = { vm.definirTema(t) }),
                )
            }

            Secao("Notificações do celular")
            ListItem(
                headlineContent = { Text("Detectar valores nas notificações") },
                supportingContent = {
                    Text(
                        "Quando um banco avisar uma compra, Pix ou depósito, o valor aparece em \"Pendentes\" para você confirmar. " +
                            "Nada é lançado sozinho e nenhum dado sai do celular."
                    )
                },
                leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(checked = captura, onCheckedChange = {
                        vm.definirCaptura(it)
                        if (it && !acessoConcedido) abrirConfiguracoesNotificacao()
                    })
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            )
            if (captura && !acessoConcedido) {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        "Falta liberar o acesso às notificações para este app nas Configurações do Android.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "Se a opção aparecer bloqueada (\"configuração restrita\"), abra Configurações > Apps > Controle de Gastos, " +
                            "toque nos três pontos e escolha \"Permitir configurações restritas\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = ::abrirConfiguracoesNotificacao) { Text("Conceder acesso") }
                }
            }
            ListItem(
                headlineContent = { Text("Valores pendentes") },
                supportingContent = { Text(if (qtdPendentes == 0) "Nenhum aguardando revisão" else "$qtdPendentes aguardando revisão") },
                leadingContent = { Icon(Icons.Filled.NotificationsActive, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPendentes),
            )

            Secao("Dados")
            ListItem(
                headlineContent = { Text("Exportar tudo para Excel") },
                supportingContent = { Text("Gera um .xlsx com todos os lançamentos e o resumo por categoria.") },
                leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { exportarTudo.launch("controle-de-gastos-${LocalDate.now()}.xlsx") },
            )
            Text(
                "Seus dados ficam apenas neste aparelho. O backup automático do Google (se ativado no celular) também inclui os lançamentos e anexos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Secao("Sobre")
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.logo_completo),
                    contentDescription = "Logo Felipe Jesus",
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Controle de Gastos · versão ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Secao(titulo: String) {
    Text(
        titulo,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
    )
}
