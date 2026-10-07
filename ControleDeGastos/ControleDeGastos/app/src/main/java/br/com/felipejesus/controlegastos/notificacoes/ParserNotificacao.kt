package br.com.felipejesus.controlegastos.notificacoes

import br.com.felipejesus.controlegastos.data.TipoLancamento
import java.util.Locale

/**
 * Extrai valor e tipo (entrada/saída) do texto de uma notificação bancária.
 * Ex.: "Compra aprovada de R$ 52,90 no MERCADO X" -> 5290 centavos, DESPESA.
 *
 * Só retorna resultado quando encontra um valor em reais E uma palavra-chave
 * financeira, para não capturar propagandas ou mensagens de conversa.
 */
object ParserNotificacao {

    data class Resultado(val valorCentavos: Long, val tipo: TipoLancamento)

    private val regexValor = Regex("""R\$\s*(\d{1,3}(?:\.\d{3})+|\d+)(?:,(\d{2}))?""")

    private val palavrasReceita = listOf(
        "você recebeu", "voce recebeu", "recebeu um pix", "pix recebido", "recebido", "recebida",
        "transferência recebida", "transferencia recebida", "depósito", "deposito",
        "crédito em conta", "credito em conta", "salário", "salario", "estorno", "reembolso",
        "rendimento", "cashback",
    )

    private val palavrasDespesa = listOf(
        "compra", "pagamento", "pagou", "pago", "débito", "debito", "pix enviado", "enviou",
        "transferência enviada", "transferencia enviada", "transferência realizada",
        "transferencia realizada", "saque", "fatura", "cobrança", "cobranca", "aprovada",
        "boleto",
    )

    private val palavrasIgnorar = listOf(
        "oferta", "promoção", "promocao", "cupom", "desconto de", "limite disponível",
        "limite disponivel", "empréstimo pré-aprovado", "emprestimo pre-aprovado",
    )

    fun analisar(texto: String): Resultado? {
        if (texto.isBlank()) return null
        val minusculo = texto.lowercase(Locale.ROOT)
        if (palavrasIgnorar.any { it in minusculo }) return null

        val m = regexValor.find(texto) ?: return null
        val reais = m.groupValues[1].replace(".", "").toLongOrNull() ?: return null
        val centavos = m.groupValues[2].ifEmpty { "00" }.toLong()
        val valor = reais * 100 + centavos
        if (valor <= 0) return null

        val tipo = when {
            palavrasReceita.any { it in minusculo } -> TipoLancamento.RECEITA
            palavrasDespesa.any { it in minusculo } -> TipoLancamento.DESPESA
            else -> return null
        }
        return Resultado(valor, tipo)
    }
}
