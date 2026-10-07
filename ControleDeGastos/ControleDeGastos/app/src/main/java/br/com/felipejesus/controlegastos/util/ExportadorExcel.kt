package br.com.felipejesus.controlegastos.util

import br.com.felipejesus.controlegastos.data.FinanceRepository
import br.com.felipejesus.controlegastos.data.TipoLancamento
import java.io.OutputStream
import java.math.BigDecimal
import java.math.RoundingMode

object ExportadorExcel {

    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    fun escrever(saida: OutputStream, dados: FinanceRepository.DadosExportacao) {
        XlsxWriter.escrever(saida, listOf(planilhaLancamentos(dados), planilhaResumo(dados)))
    }

    private fun planilhaLancamentos(dados: FinanceRepository.DadosExportacao): Planilha {
        val linhas = mutableListOf<List<Celula>>()
        linhas += listOf("Data", "Tipo", "Categoria", "Descrição", "Valor", "Observação", "Anexos")
            .map { Celula.Cabecalho(it) }

        var receitas = 0L
        var despesas = 0L
        dados.lancamentos.forEach { d ->
            val l = d.lancamento
            val receita = l.tipo == TipoLancamento.RECEITA
            if (receita) receitas += l.valorCentavos else despesas += l.valorCentavos
            linhas += listOf(
                Celula.Data(l.data),
                Celula.Texto(if (receita) "Receita" else "Despesa"),
                Celula.Texto(d.categoriaNome),
                Celula.Texto(l.descricao),
                // Despesas negativas: a soma da coluna já dá o saldo.
                Celula.Moeda(if (receita) l.valorCentavos else -l.valorCentavos),
                Celula.Texto(l.observacao),
                if (d.qtdAnexos > 0) Celula.Numero(d.qtdAnexos.toLong()) else Celula.Vazia,
            )
        }
        linhas += emptyList<Celula>()
        linhas += totalLinha("Total de receitas", receitas)
        linhas += totalLinha("Total de despesas", -despesas)
        linhas += totalLinha("Saldo do período", receitas - despesas)

        return Planilha(
            nome = "Lançamentos",
            larguras = listOf(12.0, 10.0, 22.0, 36.0, 16.0, 36.0, 9.0),
            linhas = linhas,
        )
    }

    private fun totalLinha(rotulo: String, valor: Long): List<Celula> = listOf(
        Celula.Vazia, Celula.Vazia, Celula.Vazia,
        Celula.Texto(rotulo, negrito = true),
        Celula.Moeda(valor, negrito = true),
    )

    private fun planilhaResumo(dados: FinanceRepository.DadosExportacao): Planilha {
        val linhas = mutableListOf<List<Celula>>()
        linhas += listOf("Categoria", "Tipo", "Lançamentos", "Total", "% do tipo").map { Celula.Cabecalho(it) }

        val somaPorTipo = dados.totais.groupBy { it.tipo }.mapValues { (_, v) -> v.sumOf { it.total } }
        dados.totais
            .sortedWith(compareBy({ it.tipo }, { -it.total }))
            .forEach { t ->
                val base = somaPorTipo[t.tipo] ?: 0L
                val pct = if (base > 0) {
                    BigDecimal.valueOf(t.total * 100).divide(BigDecimal.valueOf(base), 1, RoundingMode.HALF_UP)
                        .toPlainString().replace('.', ',') + "%"
                } else "-"
                linhas += listOf(
                    Celula.Texto(t.nome),
                    Celula.Texto(if (t.tipo == TipoLancamento.RECEITA) "Receita" else "Despesa"),
                    Celula.Numero(t.quantidade.toLong()),
                    Celula.Moeda(t.total),
                    Celula.Texto(pct),
                )
            }
        return Planilha(
            nome = "Resumo por categoria",
            larguras = listOf(26.0, 10.0, 13.0, 16.0, 11.0),
            linhas = linhas,
        )
    }
}
