package br.com.felipejesus.controlegastos.util

import java.io.OutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Célula de planilha. */
sealed interface Celula {
    data class Cabecalho(val texto: String) : Celula
    data class Texto(val texto: String, val negrito: Boolean = false) : Celula
    data class Moeda(val centavos: Long, val negrito: Boolean = false) : Celula
    data class Data(val data: LocalDate) : Celula
    data class Numero(val valor: Long) : Celula
    data object Vazia : Celula
}

class Planilha(
    val nome: String,
    /** Largura de cada coluna em "caracteres" do Excel. */
    val larguras: List<Double>,
    val linhas: List<List<Celula>>,
    val congelarPrimeiraLinha: Boolean = true,
)

/**
 * Gerador mínimo de .xlsx (Office Open XML) sem dependências externas.
 * Um .xlsx é um ZIP com alguns XMLs; aqui geramos só o necessário para o
 * Excel, Google Planilhas e LibreOffice abrirem com formatação de moeda e data.
 */
object XlsxWriter {

    fun escrever(saida: OutputStream, planilhas: List<Planilha>) {
        require(planilhas.isNotEmpty())
        val zip = ZipOutputStream(saida)
        fun entrada(nome: String, conteudo: String) {
            zip.putNextEntry(ZipEntry(nome))
            zip.write(conteudo.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        entrada("[Content_Types].xml", contentTypes(planilhas.size))
        entrada("_rels/.rels", RELS)
        entrada("xl/workbook.xml", workbook(planilhas))
        entrada("xl/_rels/workbook.xml.rels", workbookRels(planilhas.size))
        entrada("xl/styles.xml", STYLES)
        planilhas.forEachIndexed { i, p -> entrada("xl/worksheets/sheet${i + 1}.xml", sheet(p)) }
        zip.finish()
        zip.flush()
    }

    // Índices de estilo definidos em STYLES (cellXfs)
    private const val S_PADRAO = 0
    private const val S_CABECALHO = 1
    private const val S_MOEDA = 2
    private const val S_DATA = 3
    private const val S_NEGRITO = 4
    private const val S_MOEDA_NEGRITO = 5

    private const val XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
    private const val NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    private const val RELS = XML +
        """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
        "</Relationships>"

    private const val STYLES = XML +
        """<styleSheet xmlns="$NS_MAIN">""" +
        """<numFmts count="2">""" +
        """<numFmt numFmtId="164" formatCode="&quot;R$&quot;\ #,##0.00;[Red]\-&quot;R$&quot;\ #,##0.00"/>""" +
        """<numFmt numFmtId="165" formatCode="dd/mm/yyyy"/>""" +
        "</numFmts>" +
        """<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>""" +
        """<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>""" +
        """<fill><patternFill patternType="solid"><fgColor rgb="FFF3E1BB"/><bgColor indexed="64"/></patternFill></fill></fills>""" +
        """<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>""" +
        """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
        """<cellXfs count="6">""" +
        """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
        """<xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>""" +
        """<xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
        """<xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>""" +
        """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
        """<xf numFmtId="164" fontId="1" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1"/>""" +
        "</cellXfs>" +
        """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>""" +
        "</styleSheet>"

    private fun contentTypes(qtd: Int) = buildString {
        append(XML)
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..qtd) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("</Types>")
    }

    private fun workbook(planilhas: List<Planilha>) = buildString {
        append(XML)
        append("""<workbook xmlns="$NS_MAIN" xmlns:r="$NS_REL"><sheets>""")
        planilhas.forEachIndexed { i, p ->
            append("""<sheet name="${escapar(nomeValido(p.nome))}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(qtd: Int) = buildString {
        append(XML)
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..qtd) {
            append("""<Relationship Id="rId$i" Type="$NS_REL/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("""<Relationship Id="rId${qtd + 1}" Type="$NS_REL/styles" Target="styles.xml"/>""")
        append("</Relationships>")
    }

    private fun sheet(p: Planilha) = buildString {
        append(XML)
        append("""<worksheet xmlns="$NS_MAIN" xmlns:r="$NS_REL">""")
        if (p.congelarPrimeiraLinha && p.linhas.isNotEmpty()) {
            append("""<sheetViews><sheetView workbookViewId="0">""")
            append("""<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>""")
            append("""</sheetView></sheetViews>""")
        }
        if (p.larguras.isNotEmpty()) {
            append("<cols>")
            p.larguras.forEachIndexed { i, w ->
                append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""")
            }
            append("</cols>")
        }
        append("<sheetData>")
        p.linhas.forEachIndexed { r, linha ->
            val numLinha = r + 1
            append("""<row r="$numLinha">""")
            linha.forEachIndexed { c, celula ->
                val ref = coluna(c) + numLinha
                when (celula) {
                    is Celula.Cabecalho -> texto(ref, celula.texto, S_CABECALHO)
                    is Celula.Texto -> texto(ref, celula.texto, if (celula.negrito) S_NEGRITO else S_PADRAO)
                    is Celula.Moeda -> append(
                        """<c r="$ref" s="${if (celula.negrito) S_MOEDA_NEGRITO else S_MOEDA}"><v>${
                            BigDecimal.valueOf(celula.centavos, 2).toPlainString()
                        }</v></c>"""
                    )
                    // Datas no Excel são dias desde 30/12/1899; epochDay 0 (01/01/1970) = 25569.
                    is Celula.Data -> append("""<c r="$ref" s="$S_DATA"><v>${celula.data.toEpochDay() + 25569}</v></c>""")
                    is Celula.Numero -> append("""<c r="$ref"><v>${celula.valor}</v></c>""")
                    Celula.Vazia -> Unit
                }
            }
            append("</row>")
        }
        append("</sheetData>")
        append("</worksheet>")
    }

    private fun StringBuilder.texto(ref: String, valor: String, estilo: Int) {
        append("""<c r="$ref" t="inlineStr" s="$estilo"><is><t xml:space="preserve">${escapar(valor)}</t></is></c>""")
    }

    /** 0 -> A, 25 -> Z, 26 -> AA ... */
    fun coluna(indice: Int): String {
        var n = indice + 1
        val sb = StringBuilder()
        while (n > 0) {
            val resto = (n - 1) % 26
            sb.insert(0, 'A' + resto)
            n = (n - 1) / 26
        }
        return sb.toString()
    }

    private fun nomeValido(nome: String): String =
        nome.replace(Regex("""[\[\]:*?/\\]"""), " ").take(31).ifBlank { "Planilha" }

    private fun escapar(s: String): String = buildString(s.length) {
        for (ch in s) {
            when {
                ch == '&' -> append("&amp;")
                ch == '<' -> append("&lt;")
                ch == '>' -> append("&gt;")
                ch == '"' -> append("&quot;")
                ch == '\t' || ch == '\n' || ch == '\r' -> append(ch)
                ch < ' ' -> Unit // caracteres de controle são inválidos em XML
                else -> append(ch)
            }
        }
    }
}
