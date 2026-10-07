package br.com.felipejesus.controlegastos.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

val LocaleBR: Locale = Locale.forLanguageTag("pt-BR")

object Dinheiro {
    private val formato: NumberFormat = NumberFormat.getCurrencyInstance(LocaleBR)

    /** 123456 -> "R$ 1.234,56" */
    @Synchronized
    fun formatar(centavos: Long): String =
        formato.format(BigDecimal.valueOf(centavos, 2))

    /** Formata com sinal explícito: "+ R$ 10,00" / "− R$ 10,00". */
    fun formatarComSinal(centavos: Long, positivo: Boolean): String =
        (if (positivo) "+ " else "− ") + formatar(kotlin.math.abs(centavos))

    /** Mantém só os dígitos digitados (até 13, ~ R$ 100 bilhões) e converte para centavos. */
    fun digitosParaCentavos(digitos: String): Long =
        digitos.filter(Char::isDigit).take(13).toLongOrNull() ?: 0L
}

object Datas {
    private val mesAno = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", LocaleBR)
    private val mesCurto = DateTimeFormatter.ofPattern("MMM", LocaleBR)
    private val diaLongo = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", LocaleBR)
    private val diaCurto = DateTimeFormatter.ofPattern("dd/MM/yyyy", LocaleBR)
    private val mesNome = DateTimeFormatter.ofPattern("MMMM", LocaleBR)

    fun mesAno(mes: YearMonth): String = mes.format(mesAno).capitalizar()
    fun mesCurto(mes: YearMonth): String = mes.format(mesCurto).trimEnd('.').capitalizar()
    fun nomeMes(mes: YearMonth): String = mes.format(mesNome)
    fun diaLongo(data: LocalDate): String = when (data) {
        LocalDate.now() -> "Hoje"
        LocalDate.now().minusDays(1) -> "Ontem"
        else -> data.format(diaLongo).capitalizar()
    }
    fun diaCurto(data: LocalDate): String = data.format(diaCurto)
}

fun String.capitalizar(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(LocaleBR) else it.toString() }

fun formatarTamanho(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(LocaleBR, "%.1f MB", bytes / (1024.0 * 1024.0))
}
