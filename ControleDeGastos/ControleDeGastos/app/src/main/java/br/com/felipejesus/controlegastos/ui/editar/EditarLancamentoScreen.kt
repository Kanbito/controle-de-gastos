package br.com.felipejesus.controlegastos.ui.editar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.felipejesus.controlegastos.data.AnexoStorage
import br.com.felipejesus.controlegastos.data.TipoLancamento
import br.com.felipejesus.controlegastos.ui.Icones
import br.com.felipejesus.controlegastos.ui.componentes.abrirAnexo
import br.com.felipejesus.controlegastos.ui.componentes.corDoTipo
import br.com.felipejesus.controlegastos.util.Datas
import br.com.felipejesus.controlegastos.util.Dinheiro
import br.com.felipejesus.controlegastos.util.formatarTamanho
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditarLancamentoScreen(
    anexos: AnexoStorage,
    onVoltar: () -> Unit,
    vm: EditarLancamentoViewModel = viewModel(factory = EditarLancamentoViewModel.Factory),
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var mostrarData by rememberSaveable { mutableStateOf(false) }
    var confirmarExclusao by rememberSaveable { mutableStateOf(false) }

    val escolherArquivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.adicionarAnexo(uri)
    }
    val tirarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        vm.resultadoFoto(ok)
    }

    LaunchedEffect(estado.concluido) { if (estado.concluido) onVoltar() }
    LaunchedEffect(estado.erro) {
        estado.erro?.let {
            snackbar.showSnackbar(it)
            vm.limparErro()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (estado.edicao) "Editar lançamento" else "Novo lançamento") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (estado.edicao) {
                        IconButton(onClick = { confirmarExclusao = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir lançamento")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (estado.carregando) {
            CircularProgressIndicator(Modifier.padding(padding).padding(32.dp))
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Tipo
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val opcoes = listOf(TipoLancamento.DESPESA to "Saída", TipoLancamento.RECEITA to "Entrada")
                opcoes.forEachIndexed { i, (tipo, rotulo) ->
                    SegmentedButton(
                        selected = estado.tipo == tipo,
                        onClick = { vm.alterarTipo(tipo) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = opcoes.size),
                    ) { Text(rotulo) }
                }
            }

            // Valor com máscara de moeda: o usuário digita só números.
            val textoValor = Dinheiro.formatar(estado.valorCentavos)
            OutlinedTextField(
                value = TextFieldValue(textoValor, selection = TextRange(textoValor.length)),
                onValueChange = { novo ->
                    val digitosAntes = estado.valorCentavos.toString()
                    val digitosDepois = novo.text.filter(Char::isDigit).trimStart('0')
                    // Backspace sobre um caractere não numérico (ex.: vírgula) remove o último dígito.
                    val ajustado = if (novo.text.length < textoValor.length && digitosDepois == digitosAntes.trimStart('0')) {
                        digitosAntes.dropLast(1)
                    } else digitosDepois
                    vm.alterarValor(ajustado)
                },
                label = { Text("Valor") },
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = corDoTipo(estado.tipo)),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = estado.descricao,
                onValueChange = vm::alterarDescricao,
                label = { Text("Descrição") },
                placeholder = { Text("Ex.: Netflix, Feira do mês, Salário") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            // Categoria
            Column {
                Text("Categoria", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categorias.filter { it.tipo == estado.tipo }.forEach { c ->
                        FilterChip(
                            selected = estado.categoriaId == c.id,
                            onClick = { vm.alterarCategoria(c.id) },
                            label = { Text(c.nome) },
                            leadingIcon = {
                                Icon(
                                    Icones.de(c.icone),
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color(c.cor),
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
                Text(
                    "Crie ou edite categorias na aba Controle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Data
            ListItem(
                headlineContent = { Text(Datas.diaCurto(estado.data)) },
                overlineContent = { Text("Data") },
                leadingContent = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mostrarData = true },
            )

            OutlinedTextField(
                value = estado.observacao,
                onValueChange = vm::alterarObservacao,
                label = { Text("Observação (opcional)") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            // Anexos
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Anexos (notas, recibos, comprovantes)", style = MaterialTheme.typography.titleSmall)
                estado.anexosVisiveis.forEach { a ->
                    LinhaAnexo(
                        nome = a.nomeOriginal,
                        detalhe = formatarTamanho(a.tamanhoBytes),
                        mime = a.mimeType,
                        onAbrir = { abrirAnexo(context, anexos, a.arquivo, a.mimeType) },
                        onRemover = { vm.removerAnexoExistente(a) },
                    )
                }
                estado.anexosNovos.forEach { a ->
                    LinhaAnexo(
                        nome = a.nomeOriginal,
                        detalhe = formatarTamanho(a.tamanho) + " · novo",
                        mime = a.mimeType,
                        onAbrir = { abrirAnexo(context, anexos, a.arquivo, a.mimeType) },
                        onRemover = { vm.removerAnexoNovo(a) },
                    )
                }
                if (estado.copiandoAnexo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("  Copiando arquivo…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { escolherArquivo.launch(arrayOf("image/*", "application/pdf", "*/*")) }) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Arquivo")
                    }
                    OutlinedButton(onClick = { tirarFoto.launch(vm.prepararFoto()) }) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Foto")
                    }
                }
            }

            Button(
                onClick = vm::salvar,
                enabled = !estado.salvando,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(if (estado.salvando) "Salvando…" else "Salvar")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (mostrarData) {
        val estadoData = rememberDatePickerState(
            initialSelectedDateMillis = estado.data.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarData = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoData.selectedDateMillis?.let {
                        vm.alterarData(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    mostrarData = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { mostrarData = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estadoData)
        }
    }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { confirmarExclusao = false },
            title = { Text("Excluir lançamento?") },
            text = { Text("O lançamento e seus anexos serão apagados. Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { confirmarExclusao = false; vm.excluir() }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun LinhaAnexo(
    nome: String,
    detalhe: String,
    mime: String,
    onAbrir: () -> Unit,
    onRemover: () -> Unit,
) {
    val icone = when {
        mime.startsWith("image/") -> Icons.Filled.Image
        mime == "application/pdf" -> Icons.Filled.PictureAsPdf
        else -> Icons.Filled.Description
    }
    ListItem(
        headlineContent = { Text(nome, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(detalhe) },
        leadingContent = { Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            IconButton(onClick = onRemover) { Icon(Icons.Filled.Close, contentDescription = "Remover anexo") }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAbrir),
    )
}
