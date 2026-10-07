package br.com.felipejesus.controlegastos.ui.componentes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.felipejesus.controlegastos.data.AnexoStorage
import br.com.felipejesus.controlegastos.data.LancamentoDetalhado
import br.com.felipejesus.controlegastos.data.TipoLancamento
import br.com.felipejesus.controlegastos.ui.Icones
import br.com.felipejesus.controlegastos.ui.theme.EstiloValor
import br.com.felipejesus.controlegastos.ui.theme.LocalCoresFinanceiras
import br.com.felipejesus.controlegastos.util.Datas
import br.com.felipejesus.controlegastos.util.Dinheiro
import java.time.YearMonth

@Composable
fun SeletorMes(
    mes: YearMonth,
    onAnterior: () -> Unit,
    onProximo: () -> Unit,
    onAtual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onAnterior) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onAtual)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(Datas.mesAno(mes), style = MaterialTheme.typography.titleMedium)
            if (mes != YearMonth.now()) {
                Text(
                    "toque para voltar ao mês atual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onProximo) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
        }
    }
}

@Composable
fun IconeCategoria(icone: String, cor: Int, modifier: Modifier = Modifier, tamanho: Dp = 40.dp) {
    val c = Color(cor)
    Box(
        modifier = modifier
            .size(tamanho)
            .clip(CircleShape)
            .background(c.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icones.de(icone), contentDescription = null, tint = c, modifier = Modifier.size(tamanho * 0.55f))
    }
}

@Composable
fun corDoTipo(tipo: TipoLancamento): Color =
    if (tipo == TipoLancamento.RECEITA) LocalCoresFinanceiras.current.receita else LocalCoresFinanceiras.current.despesa

@Composable
fun ValorComSinal(centavos: Long, tipo: TipoLancamento, estilo: TextStyle = EstiloValor) {
    Text(
        Dinheiro.formatarComSinal(centavos, tipo == TipoLancamento.RECEITA),
        style = estilo,
        color = corDoTipo(tipo),
        maxLines = 1,
    )
}

@Composable
fun CartaoSecao(
    titulo: String,
    modifier: Modifier = Modifier,
    acao: (@Composable () -> Unit)? = null,
    conteudo: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                acao?.invoke()
            }
            Spacer(Modifier.height(12.dp))
            conteudo()
        }
    }
}

@Composable
fun LinhaLancamento(
    item: LancamentoDetalhado,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val l = item.lancamento
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconeCategoria(item.categoriaIcone, item.categoriaCor)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                l.descricao.ifBlank { item.categoriaNome },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.categoriaNome,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.qtdAnexos > 0) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.AttachFile,
                        contentDescription = "${item.qtdAnexos} anexo(s)",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        ValorComSinal(l.valorCentavos, l.tipo)
        trailing?.invoke()
    }
}

@Composable
fun EstadoVazio(icone: ImageVector, titulo: String, texto: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icone, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------------- Gráficos ----------------

data class Fatia(val cor: Color, val valor: Long)

/** Gráfico de rosca (donut) animado. */
@Composable
fun GraficoRosca(
    fatias: List<Fatia>,
    modifier: Modifier = Modifier,
    espessura: Dp = 26.dp,
    descricao: String = "Gráfico por categoria",
    centro: @Composable () -> Unit = {},
) {
    val progresso = remember { Animatable(0f) }
    LaunchedEffect(fatias) {
        progresso.snapTo(0f)
        progresso.animateTo(1f, tween(durationMillis = 700))
    }
    val corVazia = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier.semantics { contentDescription = descricao }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val traco = espessura.toPx()
            val diametro = size.minDimension - traco
            val topo = Offset((size.width - diametro) / 2f, (size.height - diametro) / 2f)
            val area = Size(diametro, diametro)
            val total = fatias.sumOf { it.valor }.toFloat()
            if (total <= 0f) {
                drawArc(
                    color = corVazia, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topo, size = area, style = Stroke(width = traco),
                )
                return@Canvas
            }
            val espaco = if (fatias.size > 1) 1.2f else 0f
            var inicio = -90f
            fatias.forEach { f ->
                val varredura = 360f * (f.valor / total) * progresso.value
                if (varredura > espaco) {
                    drawArc(
                        color = f.cor,
                        startAngle = inicio + espaco / 2f,
                        sweepAngle = varredura - espaco,
                        useCenter = false,
                        topLeft = topo,
                        size = area,
                        style = Stroke(width = traco, cap = StrokeCap.Butt),
                    )
                }
                inicio += varredura
            }
        }
        centro()
    }
}

data class BarraMes(val rotulo: String, val receitas: Long, val despesas: Long, val destaque: Boolean)

/** Barras lado a lado (receitas x despesas) dos últimos meses. */
@Composable
fun GraficoBarrasMensal(itens: List<BarraMes>, modifier: Modifier = Modifier) {
    val cores = LocalCoresFinanceiras.current
    val maximo = (itens.maxOfOrNull { maxOf(it.receitas, it.despesas) } ?: 0L).coerceAtLeast(1L)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .semantics { contentDescription = "Receitas e despesas dos últimos meses" },
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        itens.forEach { item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Barra(item.receitas.toFloat() / maximo, cores.receita)
                    Barra(item.despesas.toFloat() / maximo, cores.despesa)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    item.rotulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.destaque) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Barra(fracao: Float, cor: Color) {
    val animada by animateFloatAsState(targetValue = fracao.coerceIn(0f, 1f), animationSpec = tween(600), label = "barra")
    Box(
        Modifier
            .width(11.dp)
            .fillMaxHeight(animada.coerceAtLeast(0.01f))
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(cor)
    )
}

@Composable
fun Legenda(cor: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(cor)
        )
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelMedium)
    }
}

// ---------------- Anexos ----------------

fun abrirAnexo(context: Context, storage: AnexoStorage, arquivo: String, mimeType: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(storage.uri(arquivo), mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Abrir anexo com"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Nenhum app encontrado para abrir este arquivo.", Toast.LENGTH_SHORT).show()
    } catch (e: IllegalArgumentException) {
        Toast.makeText(context, "Arquivo não encontrado.", Toast.LENGTH_SHORT).show()
    }
}
