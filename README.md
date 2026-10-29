# Reconcilia AI

Reconcilia AI é uma ferramenta profissional para auditoria e conciliação financeira de transações. O sistema permite o upload de dois arquivos CSV (por exemplo, exportações de vendas de um sistema ERP e extratos de liquidação de operadoras de cartão/bancos), correlacionando as informações para identificar automaticamente transações coincidentes, divergências de taxas, defasagens de datas e lançamentos não localizados.

---

## Sumário

- [Visão Geral e Contexto](#visão-geral-e-contexto)
- [Arquitetura e Princípios de Engenharia](#arquitetura-e-princípios-de-engenharia)
- [Algoritmo de Conciliação Determinística](#algoritmo-de-conciliação-determinística)
- [Análise Explicável e IA Responsável](#análise-explicável-e-ia-responsável)
- [Stack Tecnológica](#stack-tecnológica)
- [Estrutura do Repositório](#estrutura-do-repositório)
- [Instalação e Execução Local](#instalação-e-execução-local)
- [Modo de Demonstração (Sem Chave de API)](#modo-de-demonstração-sem-chave-de-api)
- [Documentação da API REST](#documentação-da-api-rest)
- [Formato dos Arquivos CSV Suportados](#formato-dos-arquivos-csv-suportados)
- [Cobertura de Testes Automatizados](#cobertura-de-testes-automatizados)
- [Limitações Conhecidas](#limitações-conhecidas)
- [Capturas de Tela (Screenshots)](#capturas-de-tela-screenshots)

---

## Visão Geral e Contexto

No ecossistema financeiro brasileiro, empresas varejistas e prestadores de serviços frequentemente enfrentam desafios ao fechar o caixa mensal:
1. As vendas são registradas pelo valor bruto no ERP (ex.: R$ 200,00).
2. A adquirente ou banco credita o valor líquido com o desconto da taxa de administração (MDR) ou antecipação (ex.: R$ 193,00 com retenção de 3,5%).
3. Vendas efetuadas em finais de semana ou feriados são liquidadas com defasagem de dias úteis (D+1, D+2, D+30).
4. Vendas canceladas na máquina de cartão podem permanecer abertas no ERP, ou transações avulsas passadas diretamente no terminal POS não chegam a ser registradas no sistema de gestão.

O **Reconcilia AI** resolve esse atrito operacional processando os arquivos em memória, aplicando regras determinísticas de reconciliação 1-para-1 e fornecendo diagnósticos explicativos para cada discrepância.

---

## Arquitetura e Princípios de Engenharia

```
                      +------------------------------------------+
                      |         React + TypeScript (Vite)        |
                      |   Dashboard SaaS / Tailwind CSS / Lucide |
                      +--------------------+---------------------+
                                           | HTTP (REST JSON / Multipart)
                                           v
                      +------------------------------------------+
                      |       Spring Boot 3.5 (Java 17/21)       |
                      |            ReconciliationController      |
                      +----+--------------------------------+----+
                           |                                |
                           v                                v
            +------------------------------+  +-------------------------------+
            |      CsvParserService        |  |     ReconciliationService     |
            | Apache Commons CSV +         |  | Algoritmo 1-para-1            |
            | CurrencyParser & DateParser  |  | BigDecimal + Tolerâncias      |
            +------------------------------+  +---------------+---------------+
                                                              |
                                                              v
                                              +-------------------------------+
                                              |  DiscrepancyAnalysisService   |
                                              | - Motor de Regras Financeiras |
                                              | - Integração LLM Opcional     |
                                              +---------------+---------------+
                                                              |
                                                              v
                                              +-------------------------------+
                                              |       CsvExportService        |
                                              | Exportação de Resultados CSV  |
                                              +-------------------------------+
```

### Decisões Técnicas de Engenharia:
- **Cálculo Monetário com `BigDecimal`**: Nenhum cálculo financeiro utiliza ponto flutuante binário (`double`/`float`), evitando erros cumulativos de arredondamento IEEE 754. Todos os valores utilizam precisão de duas casas decimais com arredondamento `HALF_EVEN` / `HALF_UP`.
- **Determinismo Absoluto**: A inteligência artificial (LLM) **nunca** decide se duas transações coincidem ou não. O pareamento é realizado por um motor de regras determinístico e matematicamente auditável.
- **Processamento em Memória (Privacidade por Design)**: O MVP não persiste arquivos enviados em disco nem armazena transações em banco de dados, eliminando riscos de vazamento de dados de cartão ou informações financeiras confidenciais.
- **Normalização Robusta**: Tratamento de separadores de milhar e decimal brasileiros (`R$ 1.250,50`, `1250,50`, `1.500`), remoção de BOM UTF-8 (`\uFEFF`), detecção dinâmica de delimitadores (`,` ou `;` ou `\t`) e múltiplos padrões de data (`DD/MM/AAAA`, `AAAA-MM-DD`, com ou sem timestamp).
- **Tratamento Centralizado de Exceções**: `@RestControllerAdvice` padronizando respostas de erro com códigos HTTP semânticos (400, 413, 500).

---

## Algoritmo de Conciliação Determinística

Para prevenir o problema clássico de falsos positivos onde uma mesma transação é associada a múltiplos lançamentos, o motor adota uma **estratégia de múltiplos passos com restrição estrita 1-para-1**:

1. **Passo 1 (Identificador Exato)**: Cruzamento prioritário por ID/NSU/TID não nulo.
   - Data dentro da tolerância e valor dentro da tolerância $\rightarrow$ `MATCHED`.
   - Data dentro da tolerância e valor fora $\rightarrow$ `AMOUNT_MISMATCH`.
   - Data fora da tolerância e valor idêntico $\rightarrow$ `DATE_MISMATCH`.
   - Ambos fora $\rightarrow$ `AMOUNT_MISMATCH`.
2. **Passo 2 (Valor Exato e Data Idêntica)**: Pareamento dos registros restantes com valores monetários idênticos ocorridos no mesmo dia.
3. **Passo 3 (Valor Exato com Tolerância de Data)**: Pareamento de valores idênticos com defasagem de até $N$ dias (configurável pelo usuário, ex.: 1 a 7 dias).
4. **Passo 4 (Valor e Data dentro das Tolerâncias)**: Pareamento acomodando pequenas margens de arredondamento de centavos.
5. **Passo 5 (Detecção de Discrepância de Valor)**: Identificação de transações na mesma data com descrições compatíveis e divergência de valor.
6. **Passo 6 (Detecção de Discrepância de Data)**: Identificação de transações de mesmo valor com defasagem temporal moderada (compensação bancária).
7. **Passo 7 e 8 (Não Conciliados)**: Transações restantes são classificadas como `UNMATCHED`, discriminando se a ausência ocorre na adquirente ou no ERP.

---

## Análise Explicável e IA Responsável

O Reconcilia AI oferece hipóteses para apoiar o operador financeiro a compreender rapidamente a causa raiz de cada inconsistência:

- **Motor Heurístico de Regras (100% Offline)**:
  - **Desconto de Taxa MDR**: Detecta retenções percentuais típicas de adquirentes (0,5% a 7%) e calcula o percentual de desconto sobre a venda bruta.
  - **Janela de Compensação (D+1/D+2)**: Analisa defasagens temporais decorrentes de finais de semana ou compensação interbancária.
  - **Cancelamento/Estorno**: Detecta pedidos cancelados ou estornos avulsos.
  - **Vendas Diretas em Balcão (POS)**: Identifica créditos na adquirente sem pedido correspondente no ERP.
- **Integração Opcional com LLM (Server-side)**:
  - Quando a variável `RECONCILIA_AI_API_KEY` ou `OPENAI_API_KEY` é configurada no servidor, o sistema pode enriquecer a hipótese via chamada à API com prompt rigorosamente delimitado aos metadados da transação.
  - As chaves de API **nunca** são expostas ao frontend.
  - Todas as análises são explicitamente rotuladas na interface como **"Hipótese (Aviso de Auditoria)"**, deixando claro que se trata de uma inferência interpretativa, e não de um fato contábil comprovado.

---

## Stack Tecnológica

- **Backend**:
  - Java 17+ (compatível com Java 21)
  - Spring Boot 3.5.0
  - Spring Web, Spring Validation
  - Apache Commons CSV 1.11.0
  - Jackson Datatype JSR-310
  - JUnit 5 & Mockito (Testes unitários e integração)
  - Maven 3.9+ com Maven Wrapper
- **Frontend**:
  - React 18.3 com TypeScript 5.7
  - Vite 6
  - Tailwind CSS 3.4
  - Lucide React (Ícones consistentes)

---

## Estrutura do Repositório

```
reconcilia-ai/
├── backend/                               # Aplicação Spring Boot
│   ├── src/main/java/com/reconcilia/
│   │   ├── controller/                    # Endpoints REST e Multipart
│   │   ├── dto/                           # DTOs de entrada e saída
│   │   ├── exception/                     # Tratamento global de exceções
│   │   ├── model/                         # Modelos de domínio e enums
│   │   └── service/                       # Parsers, motor de reconciliação e IA
│   ├── src/test/java/com/reconcilia/      # Suíte de testes automatizados
│   ├── pom.xml                            # Dependências e build Maven
│   ├── mvnw / mvnw.cmd                    # Maven Wrapper multiplataforma
│   └── src/main/resources/application.yml
├── frontend/                              # Aplicação React SPA
│   ├── src/
│   │   ├── components/                    # Cards de upload, tabela, modais e KPIs
│   │   ├── services/                      # Cliente HTTP da API REST
│   │   ├── types/                         # Interfaces TypeScript
│   │   ├── utils/                         # Formatadores de moeda (BRL) e data
│   │   ├── App.tsx                        # Layout principal e dashboard
│   │   └── main.tsx
│   ├── package.json
│   ├── vite.config.ts                     # Configuração do Vite com proxy para :8080
│   └── tailwind.config.js
├── samples/                               # Arquivos de exemplo para teste
│   ├── erp_sales.csv                      # Relatório de vendas ERP
│   └── acquirer_transactions.csv          # Extrato de liquidação da adquirente
├── .gitignore                             # Configuração de exclusão Git
└── README.md                              # Documentação do projeto
```

---

## Instalação e Execução Local

### Pré-requisitos
- **Java**: JDK 17 ou JDK 21 instalado (`java -version`).
- **Node.js**: v18+ ou v20+ (`node -v` e `npm -v`).
- **Git**

### Passo 1: Iniciar o Backend (Spring Boot)

Abra um terminal na pasta `backend`:

```bash
cd backend
./mvnw spring-boot:run
```

*(No Windows PowerShell, você pode executar `.\mvnw.cmd spring-boot:run`)*

O servidor iniciará em `http://localhost:8080`.
Você pode verificar a saúde da API acessando:
`http://localhost:8080/api/reconciliation/status`

### Passo 2: Iniciar o Frontend (Vite + React)

Em outro terminal, acesse a pasta `frontend`:

```bash
cd frontend
npm install
npm run dev
```

Acesse a aplicação no navegador em:
**`http://localhost:5173`**

*(O Vite já está configurado com proxy reverso redirecionando chamadas `/api/*` para `http://localhost:8080`).*

---

## Modo de Demonstração (Sem Chave de API)

O Reconcilia AI foi projetado para demonstrações imediatas e portfólio sem requerer nenhuma configuração externa:

1. Acesse `http://localhost:5173`.
2. Clique no botão **"Carregar Demo (1-Clique)"** no topo da página.
3. O sistema carregará instantaneamente uma massa de dados sintéticos baseada em casos reais de varejo brasileiro:
   - Pagamentos via **PIX** conciliados com exatidão;
   - Venda parcelada com **retenção de taxa MDR de 3%** (`AMOUNT_MISMATCH`);
   - Venda em débito com **defasagem de liquidação financeira de 5 dias** (`DATE_MISMATCH`);
   - Pedido registrado no ERP mas cancelado no gateway (`UNMATCHED`);
   - Venda presencial realizada diretamente no terminal POS sem registro no ERP (`UNMATCHED`).
4. Clique em qualquer linha para abrir o modal de detalhes e conferir a **hipótese formulada pelo motor de regras financeiras**, mesmo sem chave de API cadastrada.

### Testando o Fluxo de Upload Manual

Caso queira testar o fluxo de envio de arquivos:
1. Baixe os arquivos prontos da pasta `samples/` (`erp_sales.csv` e `acquirer_transactions.csv`) ou clique nos links **"CSV ERP"** e **"CSV Adquirente"** no topo da barra de navegação.
2. Arraste cada arquivo para o card correspondente na tela inicial.
3. Observe a prévia automática das primeiras 5 linhas e a sugestão inteligente do mapeamento de colunas.
4. Ajuste a tolerância de data se desejar e clique em **"Reconciliar Transações"**.

---

## Documentação da API REST

### 1. Status do Servidor
- **Método**: `GET /api/reconciliation/status`
- **Descrição**: Retorna o status operacional do backend e disponibilidade de recursos.
- **Resposta**:
  ```json
  {
    "status": "UP",
    "appName": "Reconcilia AI",
    "version": "1.0.0",
    "aiAvailable": false,
    "defaultDateToleranceDays": 1,
    "defaultAmountTolerance": "0.00"
  }
  ```

### 2. Prévia de Arquivo CSV
- **Método**: `POST /api/reconciliation/preview`
- **Consumes**: `multipart/form-data`
- **Parâmetros**: `file` (MultipartFile), `maxRows` (int, default: 5)
- **Resposta**: Retorna nome do arquivo, total de linhas estimadas, cabeçalhos encontrados, mapeamento sugerido e prévia das primeiras linhas.

### 3. Processamento de Conciliação
- **Método**: `POST /api/reconciliation/process`
- **Consumes**: `multipart/form-data`
- **Parâmetros**:
  - `fileA`: Arquivo CSV do ERP.
  - `fileB`: Arquivo CSV da Adquirente.
  - `mappingAJson`: Mapeamento de colunas do Arquivo A (JSON string).
  - `mappingBJson`: Mapeamento de colunas do Arquivo B (JSON string).
  - `dateToleranceDays`: Janela de tolerância em dias (ex.: 1).
  - `amountTolerance`: Tolerância em valor monetário (ex.: 0.00).
- **Resposta**: `ReconciliationResultDto` contendo resumo numérico consolidado (`ReconciliationSummary`) e a lista de itens com seus respectivos diagnósticos e hipóteses.

### 4. Obter Dados de Demonstração
- **Método**: `GET /api/reconciliation/demo`
- **Descrição**: Retorna o resultado completo de conciliação sintética pré-processada.

### 5. Exportar Resultados para CSV
- **Método**: `POST /api/reconciliation/export`
- **Consumes**: `application/json`
- **Corpo**: Lista de `ReconciliationItemDto`.
- **Resposta**: Arquivo CSV codificado em UTF-8 para download.

---

## Formato dos Arquivos CSV Suportados

O parser aceita formatos flexíveis comuns em exportações de sistemas comerciais:

| Campo | Nomes Sugeridos de Cabeçalho | Formatos Aceitos |
| :--- | :--- | :--- |
| **Identificador** | `id`, `transacao`, `codigo`, `nsu`, `tid`, `referencia`, `pedido` | Texto alfanumérico (`TX-1001`, `849201`) |
| **Data** | `data`, `data_venda`, `data_transacao`, `dt_liquidacao` | `DD/MM/AAAA`, `AAAA-MM-DD`, `DD/MM/AAAA HH:mm:ss` |
| **Valor** | `valor`, `valor_bruto`, `valor_creditado`, `total`, `amount` | `R$ 1.250,50`, `1250,50`, `1250.50` |
| **Descrição** | `descricao`, `produto`, `tipo`, `forma_pagamento`, `detalhe` | Texto descritivo |

---

## Cobertura de Testes Automatizados

O backend conta com uma suíte de 40 testes automatizados cobrindo os cenários críticos de negócio:

- **`CurrencyParserTest` (13 testes)**: Validação de moeda brasileira com vírgula, notação internacional com ponto, valores negativos com sinal e parênteses contábeis, e rejeição de entradas inválidas.
- **`DateParserTest` (7 testes)**: Validação de formatos ISO, brasileiros, e carimbos de data/hora.
- **`CsvParserServiceTest` (6 testes)**: Detecção automática de separadores (vírgula, ponto e vírgula, tabulação), geração de prévia e validação de colunas faltantes.
- **`ReconciliationServiceTest` (7 testes)**:
  - Pareamento exato por identificador;
  - Detecção de divergência de valor (`AMOUNT_MISMATCH`);
  - Detecção de defasagem de repasse (`DATE_MISMATCH`);
  - Reconciliação dentro da tolerância configurada;
  - **Garantia de 1-para-1**: Evita duplicidade quando existem múltiplas vendas de mesmo valor;
  - Cálculo de métricas de conciliação com precisão decimal;
- **`DiscrepancyAnalysisServiceTest` (3 testes)**: Validação das hipóteses de taxa de administração (MDR) e defasagem temporal.
- **`ReconciliationControllerTest` (4 testes)**: Testes de integração com `MockMvc` validando status, upload de arquivo multipart, tratamento de arquivo vazio e endpoints de demonstração.

Para executar os testes:
```bash
cd backend
./mvnw test
```

---

## Limitações Conhecidas

1. **Volume em Memória**: Por processar os arquivos estritamente em memória RAM para garantir privacidade sem persistência em disco, o limite recomendado por arquivo é de aproximadamente 10MB (cerca de 50.000 a 100.000 linhas por arquivo).
2. **Reconciliação N-para-1 (Agrupamento em Lote)**: No MVP atual, o pareamento é estritamente 1-para-1. Vendas que são liquidadas pela adquirente em um único lote consolidado (ex.: 10 vendas somadas em um único depósito bancário) aparecem como itens não conciliados individuais no arquivo A e uma entrada única no arquivo B.
3. **Mapeamento Monofonte**: Espera-se que cada arquivo represente uma única fonte de verdade (ex.: Arquivo 1 = ERP, Arquivo 2 = Adquirente).

---

## Capturas de Tela (Screenshots)

### 1. Painel Principal com Upload e Configuração de Tolerâncias
```
+-------------------------------------------------------------------------------+
| Reconcilia AI   [Backend Online] [Demo 1-Clique] [Download CSVs]             |
+-------------------------------------------------------------------------------+
| +-----------------------------------+   +-----------------------------------+ |
| | Fonte A: Vendas ERP               |   | Fonte B: Extrato da Adquirente    | |
| | [ Arraste o arquivo vendas.csv ]  |   | [ Arraste o extrato.csv ]         | |
| | Mapeamento: ID | Data | Valor     |   | Mapeamento: NSU | Data | Valor    | |
| +-----------------------------------+   +-----------------------------------+ |
|                                                                               |
| [ Tolerâncias: Janela: ±1 dia | Valor: R$ 0,00 ]    [ Reconciliar Transações ]|
+-------------------------------------------------------------------------------+
```

### 2. Dashboard de Resultados e KPIs Financeiros
```
+-------------------------------------------------------------------------------+
| [ Volume ERP: R$ 10.970,40 ]      [ Volume Extrato: R$ 10.875,90 ]            |
| [ Taxa de Conciliação: 55.6% ]     [ Divergência Líquida: -R$ 94,50 ]         |
+-------------------------------------------------------------------------------+
| [Todas (9)] [Conciliadas (5)] [Dif. Valor (2)] [Dif. Data (1)] [Não Conc. (2)]|
|                                                                               |
| Status       | ERP               | Adquirente        | Dif.       | Ação      |
| CONCILIADO   | TX-1001 R$ 150,00 | TX-1001 R$ 150,00 | R$ 0,00    | [Detalhes]|
| DIF. VALOR   | TX-1003 R$2.500,00| TX-1003 R$2.425,00| -R$ 75,00  | [Detalhes]|
| DIF. DATA    | TX-1004 R$ 340,00 | TX-1004 R$ 340,00 | 5 dias     | [Detalhes]|
| NÃO CONCIL.  | TX-1006 R$ 450,00 | (Ausente)         | -R$ 450,00 | [Detalhes]|
+-------------------------------------------------------------------------------+
```

---

## Licença

Distribuído sob a licença MIT. Consulte `LICENSE` para mais informações.
