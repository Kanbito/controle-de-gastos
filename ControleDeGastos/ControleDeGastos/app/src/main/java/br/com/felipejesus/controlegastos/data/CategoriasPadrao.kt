package br.com.felipejesus.controlegastos.data

import br.com.felipejesus.controlegastos.data.TipoLancamento.DESPESA
import br.com.felipejesus.controlegastos.data.TipoLancamento.RECEITA

object CategoriasPadrao {

    /** Paleta usada nos gráficos e oferecida ao criar categorias. */
    val cores: List<Int> = listOf(
        0xFFD4AF37, 0xFF2E7D32, 0xFF1565C0, 0xFFC62828, 0xFF6A1B9A, 0xFFEF6C00,
        0xFF00838F, 0xFFAD1457, 0xFF558B2F, 0xFF4E342E, 0xFF37474F, 0xFF8E24AA,
        0xFF00897B, 0xFFF9A825, 0xFF5D4037, 0xFF3949AB,
    ).map { it.toInt() }

    val lista: List<Categoria> = listOf(
        // Entradas
        Categoria(nome = "Salário", tipo = RECEITA, cor = 0xFF2E7D32.toInt(), icone = "trabalho"),
        Categoria(nome = "Férias", tipo = RECEITA, cor = 0xFF00838F.toInt(), icone = "ferias"),
        Categoria(nome = "13º salário", tipo = RECEITA, cor = 0xFF558B2F.toInt(), icone = "presente"),
        Categoria(nome = "Renda extra", tipo = RECEITA, cor = 0xFFD4AF37.toInt(), icone = "notebook"),
        Categoria(nome = "Investimentos", tipo = RECEITA, cor = 0xFF1565C0.toInt(), icone = "investimento"),
        Categoria(nome = "Reembolso", tipo = RECEITA, cor = 0xFF6A1B9A.toInt(), icone = "reembolso"),
        Categoria(nome = "Outras receitas", tipo = RECEITA, cor = 0xFF37474F.toInt(), icone = "dinheiro"),
        // Saídas
        Categoria(nome = "Mercado", tipo = DESPESA, cor = 0xFFEF6C00.toInt(), icone = "mercado"),
        Categoria(nome = "Aluguel", tipo = DESPESA, cor = 0xFFC62828.toInt(), icone = "casa"),
        Categoria(nome = "Contas da casa", tipo = DESPESA, cor = 0xFFF9A825.toInt(), icone = "energia"),
        Categoria(nome = "Assinaturas", tipo = DESPESA, cor = 0xFF8E24AA.toInt(), icone = "assinatura"),
        Categoria(nome = "Transporte", tipo = DESPESA, cor = 0xFF1565C0.toInt(), icone = "carro"),
        Categoria(nome = "Alimentação fora", tipo = DESPESA, cor = 0xFFAD1457.toInt(), icone = "restaurante"),
        Categoria(nome = "Saúde", tipo = DESPESA, cor = 0xFF00897B.toInt(), icone = "saude"),
        Categoria(nome = "Educação", tipo = DESPESA, cor = 0xFF3949AB.toInt(), icone = "educacao"),
        Categoria(nome = "Lazer", tipo = DESPESA, cor = 0xFF558B2F.toInt(), icone = "lazer"),
        Categoria(nome = "Compras", tipo = DESPESA, cor = 0xFFD4AF37.toInt(), icone = "compras"),
        Categoria(nome = "Cartão de crédito", tipo = DESPESA, cor = 0xFF5D4037.toInt(), icone = "cartao"),
        Categoria(nome = "Outras despesas", tipo = DESPESA, cor = 0xFF37474F.toInt(), icone = "outros"),
    )
}
