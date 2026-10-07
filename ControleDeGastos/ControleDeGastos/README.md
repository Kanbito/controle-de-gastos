# Controle de Gastos

App Android nativo para controle de receitas e despesas pessoais. 100% offline, sem login, com a identidade visual **Felipe Jesus** (preto e dourado).

## Funcionalidades

| # | Requisito | Onde está |
|---|-----------|-----------|
| 1 | Lista de lançamentos com editar e excluir | Aba **Lançamentos** (menu ⋮ em cada item, ou toque para editar) |
| 2 | Resumo do mês: total gasto, recebido e saldo | Aba **Início**, cartão "Saldo atual" |
| 3 | Gráfico por categoria | Aba **Início**, gráfico de rosca + barras dos últimos 6 meses |
| 4 | Dados no próprio celular, offline, sem login | Banco Room/SQLite local; o app nem pede permissão de internet |
| 5 | Saldo do mês anterior | Cartão "Saldo atual" mostra o saldo acumulado que veio dos meses anteriores |
| 6 | Definições de entradas (Salário, Férias…) | Aba **Controle** → Entradas (criar/editar/excluir) |
| 7 | Definições de saídas (Mercado, Aluguel…) | Aba **Controle** → Saídas |
| 8 | Controle das definições ("R$ X em Assinaturas") | Aba **Controle**: total, quantidade e % de cada categoria no mês; toque para ver os lançamentos |
| 9 | Sincronizar com notificações | **Ajustes** → "Detectar valores nas notificações". Valores de compras, Pix e depósitos viram *pendentes* para você confirmar |
| 10 | Boas práticas Android | Kotlin, Jetpack Compose, Material 3, MVVM, Room, valores em centavos, edge-to-edge, R8, backup |
| 11 | Tema claro e escuro | **Ajustes** → Aparência (Sistema / Claro / Escuro) |
| 12 | Exportar para Excel | Ícone ⬇ na aba Lançamentos (mês) ou **Ajustes** → Exportar tudo. Gera `.xlsx` com 2 abas |
| 13 | Anexar imagens e documentos | No formulário do lançamento: **Arquivo** (galeria, PDF, qualquer doc) ou **Foto** (câmera) |
| 14 | Logo | Ícone do app, tela de abertura, cabeçalho e tela Sobre |

## Como gerar o APK

### Opção A — GitHub (sem instalar nada)
1. Crie um repositório no GitHub (pode ser privado) e envie esta pasta.
2. Vá em **Actions** → o workflow **Gerar APK** roda sozinho a cada push (ou clique em *Run workflow*).
3. Ao terminar (~5 min), abra a execução e baixe o artefato **ControleDeGastos-apk**.
4. Dentro do zip há dois APKs: `app-debug.apk` e `app-release.apk` (menor e otimizado). Use o **release**.

### Opção B — Android Studio
1. Instale o [Android Studio](https://developer.android.com/studio).
2. *File → Open* → selecione a pasta `ControleDeGastos`. Aguarde o Gradle sincronizar.
3. *Build → Build App Bundle(s) / APK(s) → Build APK(s)*.
4. O APK fica em `app/build/outputs/apk/`.

### Instalar no celular
Envie o `.apk` para o celular, abra e permita "instalar apps de fontes desconhecidas" quando o Android pedir.

## Detecção por notificações (item 9)
- Ative em **Ajustes** e conceda o acesso às notificações quando o Android abrir a tela.
- Em Android 13+, apps instalados fora da Play Store podem ter essa opção bloqueada como "configuração restrita". Libere em *Configurações → Apps → Controle de Gastos → ⋮ → Permitir configurações restritas*.
- O app só considera notificações que tenham um valor em reais **e** uma palavra financeira (compra, Pix, pagamento, débito, depósito, recebeu…). Propagandas com "oferta", "cupom" etc. são ignoradas.
- Nada é lançado automaticamente: os valores vão para **Pendentes**, onde você toca em *Lançar* (abre o formulário já preenchido) ou *Ignorar*.

## Estrutura do código
```
app/src/main/java/br/com/felipejesus/controlegastos/
├── data/            Room (entidades, DAOs, banco), repositório, anexos, preferências
├── notificacoes/    Serviço que lê notificações + parser de valores
├── util/            Formatação BRL/datas, gerador .xlsx próprio (sem dependências)
└── ui/
    ├── inicio/      Resumo, gráficos, últimos lançamentos
    ├── lancamentos/ Lista com filtros, busca e exportação
    ├── controle/    Totais por categoria + gerenciamento das definições
    ├── editar/      Formulário de lançamento com anexos
    ├── pendentes/   Valores detectados nas notificações
    ├── ajustes/     Tema, notificações, exportação, sobre
    ├── componentes/ Seletor de mês, gráfico de rosca, barras etc.
    └── theme/       Cores dourado/preto (claro e escuro)
```

## Rumo à Play Store
1. **Chave de assinatura**: gere um keystore (Android Studio → *Build → Generate Signed Bundle*) e crie `keystore.properties` na raiz:
   ```
   storeFile=minha-chave.jks
   storePassword=...
   keyAlias=...
   keyPassword=...
   ```
   Guarde a chave com segurança — sem ela não é possível atualizar o app.
2. **Gerar AAB**: a Play Store exige `.aab` (`./gradlew bundleRelease`).
3. **Conta de desenvolvedor Google Play**: taxa única de US$ 25.
4. **Política de privacidade**: obrigatória (pode ser uma página simples dizendo que os dados ficam só no aparelho).
5. **Notificações**: o Google revisa apps que usam `NotificationListenerService`; será preciso explicar o uso na Play Console.
6. **Hospedagem mensal** só é necessária se o app passar a ter conta/login e sincronização na nuvem (ex.: Firebase, Supabase). Na versão atual, tudo fica no celular e não há custo de servidor.
