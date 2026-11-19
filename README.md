# Território Alerta — Monitoramento Preditivo de Alagamentos

## 1. Identificação do projeto

| Item              | Descrição                                                                                  |
| ----------------- | ------------------------------------------------------------------------------------------ |
| Projeto           | Território Alerta — Monitoramento Preditivo de Alagamentos                                 |
| Disciplina        | Programação Orientada a Objetos                                                            |
| Atividade         | Global Solution 2º/2026 — _Territory Predictive Insights_                                  |
| Tema              | CitizenTech — tecnologias para as necessidades reais das pessoas nas cidades e comunidades |
| Recorte escolhido | 🌊 Clima local: chuva intensa e risco de alagamento em pontos vulneráveis                  |
| Integrantes       | _(preencher — ver seção 19)_                                                               |

**Objetivo:** aplicação Java orientada a objetos, executada em um menu de console, que simula sensores ambientais de um ponto vulnerável, grava as medições em banco de dados, gera previsões simples a partir do histórico e emite alertas explicados quando os valores previstos atingem limiares configuráveis.

## 2. Problema identificado

Chuvas intensas em pouco tempo sobrecarregam bueiros, galerias e córregos. Em regiões de baixada, com ocupação densa e drenagem insuficiente, a água sobe rápido e atinge moradias, comércio, vias e equipamentos públicos (escolas, UBSs). Muitas vezes quem cuida do território só percebe o problema quando a rua já está alagada: as informações existem de forma dispersa (um morador que observa o córrego, uma régua de nível, a previsão do tempo), mas não estão organizadas em um histórico que permita perceber **a tendência de agravamento antes** do pior momento.

## 3. Público beneficiado

| Quem sofre o problema                                                                          | Quem usa o sistema                                                                            | Decisões que o sistema apoia                                                                                                                                                                                                          |
| ---------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Moradores de áreas sujeitas a alagamento; comércio local; usuários de escolas e UBSs da região | Associações comunitárias, Defesa Civil municipal, prefeituras e equipes de gestão territorial | Quando fazer inspeções preventivas; quando verificar bueiros, galerias e drenagem; quando intensificar o acompanhamento de uma região; quando preparar equipes; quando comunicar preventivamente a população; onde priorizar recursos |

O sistema **não decide** nada sozinho: ele organiza dados, mostra previsões, explica alertas e recomenda uma _avaliação_ — a decisão continua com as pessoas responsáveis (ver seção 17).

## 4. Solução proposta

1. Um **ponto de monitoramento** (`PM-01`) possui três sensores simulados: pluviômetro (mm/h), nível de água (cm) e umidade do solo (%).
2. A cada leitura, o valor é **validado** (faixa física, sinal, unidade, data). Leituras válidas são **gravadas no H2**; inválidas são rejeitadas com `DadosInvalidosException`; sensores inoperantes ou com falha de comunicação geram `FonteIndisponivelException`.
3. O usuário executa **previsões** com uma de duas estratégias polimórficas (média móvel ou comparação entre períodos), escolhidas por uma **Factory**. O histórico usado vem **do banco**.
4. O valor previsto é comparado com os **limiares** (atenção / alerta / crítico) mantidos pelo **Singleton** `ConfiguracaoSistema`. Se algum for atingido, um **alerta explicado** é gravado no banco.
5. Histórico, alertas e status ficam disponíveis no menu e **sobrevivem ao reinício** (H2 em modo arquivo). O histórico pode ser **exportado em CSV**.

### Decisões da equipe

Definidas no documento de requisitos da equipe:

- Recorte **clima local / alagamentos**, com três sensores coerentes (chuva, nível de água, umidade do solo).
- Caminho **Spring Boot + Spring Data JPA** (e não JDBC puro), com **H2 em modo arquivo** na aplicação — e não em memória — para cumprir "os dados sobrevivem ao reiniciar".
- **Dois padrões criacionais** (Factory e Singleton) e o bônus de **exportação CSV**.
- Menu de console, sem API REST e sem interface gráfica; território descrito por códigos de região/ponto/sensor, nunca por pessoas.

Decisões de implementação tomadas durante o desenvolvimento _(a equipe deve revisá-las e confirmar que concorda)_:

- Exceções como `RuntimeException` (motivos na seção 8).
- Mínimo de **5 medições** para qualquer previsão (janela mínima 5; comparação com 2 períodos de no mínimo 3) — ligado à pergunta "quando o sistema deve se calar?".
- Projeção da comparação **limitada à faixa física** do sensor (chuva não fica negativa, umidade não passa de 100%).
- Classe `SensorSimulado` intermediária para não repetir código entre os três sensores; falhas (5%) e leituras espúrias (2%) simuladas para exercitar as exceções no uso real.
- `AvaliadorAlertas` separado do `ServicoMonitoramento` para manter o serviço enxuto e a regra de alerta testável isoladamente.
- Limiares e estado operante dos sensores ficam **em memória** (voltam ao padrão ao reiniciar); medições e alertas ficam **no banco**.

## 5. Tecnologias utilizadas

| Tecnologia                  | Versão                                | Uso                                                           |
| --------------------------- | ------------------------------------- | ------------------------------------------------------------- |
| Java (JDK)                  | 21 (LTS)                              | Linguagem                                                     |
| Spring Boot                 | 4.1.1                                 | Container de injeção de dependências, configuração e execução |
| Spring Data JPA / Hibernate | gerenciadas pelo Spring Boot 4.1.1    | Entidades e repositórios                                      |
| H2 Database                 | 2.4.240 (gerenciada pelo Spring Boot) | Banco em arquivo (aplicação) e em memória (testes)            |
| Maven                       | 3.9.x (+ Maven Wrapper 3.3.4)         | Build e dependências                                          |
| JUnit 5 (Jupiter)           | gerenciada pelo Spring Boot           | Testes unitários                                              |
| Mockito                     | gerenciada pelo Spring Boot           | Mocks dos repositórios                                        |

As versões de Hibernate, H2, JUnit e Mockito **não** são fixadas manualmente no `pom.xml`: o `spring-boot-starter-parent` 4.1.1 define versões testadas em conjunto, o que evita incompatibilidades.

## 6. Arquitetura do sistema

```
territorio-alerta/
├── pom.xml, mvnw, mvnw.cmd, .mvn/          build Maven (+ wrapper)
├── data/                                   banco H2 em arquivo (criado na execução)
├── exportacoes/                            arquivos CSV exportados (criado na execução)
└── src/
    ├── main/java/com/territorio/alerta/
    │   ├── SistemaMonitoramento.java       main + inicialização do menu (Spring Boot)
    │   ├── dominio/                        regras e conceitos do problema (POO)
    │   │   ├── ComponenteTerritorial.java  classe abstrata (id, nome, região, status)
    │   │   ├── PontoMonitoramento.java     filha concreta: possui sensores e verifica o próprio status
    │   │   ├── Sensor.java                 interface
    │   │   ├── SensorSimulado.java         base abstrata comum dos sensores (sem código repetido)
    │   │   ├── PluviometroSensor.java      sensor de chuva (mm/h)
    │   │   ├── SensorNivelAgua.java        sensor de nível (cm)
    │   │   ├── SensorUmidadeSolo.java      sensor de saturação do solo (%)
    │   │   ├── TipoSensor.java             unidade e faixa válida de cada tipo
    │   │   ├── DadosTerritorio.java        dados da região + limiares (com validação)
    │   │   ├── Limiares.java               atenção < alerta < crítico (imutável)
    │   │   ├── EstrategiaPrevisao.java     classe abstrata das previsões
    │   │   ├── PrevisaoMediaMovel.java     estratégia 1
    │   │   ├── PrevisaoComparacao.java     estratégia 2
    │   │   ├── ResultadoPrevisao.java      resultado imutável da previsão
    │   │   ├── Tendencia.java, TipoEstrategia.java, StatusComponente.java
    │   │   ├── Medicao.java                entidade JPA
    │   │   ├── Alerta.java                 entidade JPA
    │   │   └── NivelAlerta.java            ATENÇÃO, ALERTA, CRÍTICO
    │   ├── excecao/                        DadosInvalidosException, FonteIndisponivelException
    │   ├── persistencia/                   MedicaoRepository, AlertaRepository (Spring Data JPA)
    │   ├── servico/                        ServicoMonitoramento, FabricaPrevisao (Factory),
    │   │                                   ConfiguracaoSistema (Singleton), AvaliadorAlertas,
    │   │                                   ConfiguracaoAplicacao (beans), records de retorno
    │   ├── console/                        MenuPrincipal, MenuConfiguracao, EntradaConsole, Tela
    │   └── util/                           ExportadorCSV, Formatos
    ├── main/resources/application.properties    H2 em arquivo
    └── test/
        ├── java/com/territorio/alerta/          testes (JUnit 5 + Mockito)
        └── resources/application.properties     H2 em memória, menu desligado
```

| Camada         | Responsabilidade                                                                      | Não faz                                 |
| -------------- | ------------------------------------------------------------------------------------- | --------------------------------------- |
| `dominio`      | Conceitos do problema, validações, cálculos das previsões, classificação por limiares | Não acessa banco nem console            |
| `excecao`      | Erros com nome e mensagem do domínio                                                  | —                                       |
| `persistencia` | Gravar/consultar medições e alertas                                                   | Não contém regra de negócio             |
| `servico`      | Orquestrar o fluxo: ler sensor → validar → gravar → prever → avaliar → alertar        | Não imprime nada                        |
| `console`      | Ler entradas, chamar o serviço, mostrar resultados, capturar exceções                 | Não calcula nem valida regra de negócio |
| `util`         | Exportação CSV e formatação pt-BR                                                     | —                                       |

**Fluxo de uma previsão:** `MenuPrincipal` → `ServicoMonitoramento.executarPrevisao()` → `FabricaPrevisao.criar(tipo)` → `MedicaoRepository.findBySensorIdOrderByDataHoraDescIdDesc(id, Limit)` → `estrategia.prever(historico)` → `AvaliadorAlertas.avaliar(resultado, limiares do Singleton)` → `AlertaRepository.save(alerta)`.

## 7. Conceitos de POO aplicados

| Conceito            | Onde aparece                                                                                                                                                                                                | Como                                                                                                                                                                                                                                                                                                                              |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Abstração**       | `ComponenteTerritorial`, `EstrategiaPrevisao`, `Sensor`, `SensorSimulado`                                                                                                                                   | Representam a ideia geral ("algo monitorável", "uma forma de prever", "uma fonte de leitura") sem detalhes concretos. O serviço trabalha com `Sensor` e `EstrategiaPrevisao`, não com as classes concretas.                                                                                                                       |
| **Encapsulamento**  | Todos os atributos são `private`; `Medicao`, `DadosTerritorio`, `Limiares`, `ConfiguracaoSistema`                                                                                                           | Os dados só mudam por métodos que validam. `Medicao` tem setters **privados** com validação e construtor JPA `protected`: não existe medição inválida. `PontoMonitoramento.getSensores()` devolve lista imutável. `ComponenteTerritorial.atualizarStatus()` é `protected final`: só subclasses mudam o status, e via verificação. |
| **Herança**         | `PontoMonitoramento extends ComponenteTerritorial`; `PluviometroSensor`/`SensorNivelAgua`/`SensorUmidadeSolo extends SensorSimulado`; `PrevisaoMediaMovel`/`PrevisaoComparacao extends EstrategiaPrevisao`  | Reaproveita id/nome/região/status, o fluxo comum de leitura e as validações comuns de previsão.                                                                                                                                                                                                                                   |
| **Polimorfismo**    | `estrategia.prever(historico)` em `ServicoMonitoramento`; `sensor.lerValor()`; `ponto.verificarStatus()`                                                                                                    | A mesma chamada executa algoritmos diferentes conforme o objeto real. Com o mesmo histórico (20,20,20,30,30,30) a média móvel prevê 26 e a comparação prevê 40 (teste `estrategiasDevemTerComportamentosDiferentes`).                                                                                                             |
| **Interface**       | `Sensor`                                                                                                                                                                                                    | Contrato (`lerValor`, `estaFuncionando`, `getTipo`, …) que qualquer fonte de leitura deve cumprir — inclusive um mock nos testes.                                                                                                                                                                                                 |
| **Classe abstrata** | `ComponenteTerritorial` (método abstrato `verificarStatus()`), `EstrategiaPrevisao` (método concreto `prever()` + abstrato `quantidadeNecessaria()`), `SensorSimulado` (abstrato `simularProximaLeitura()`) | Guarda estado e comportamento comuns, e obriga as filhas a completar o que é específico.                                                                                                                                                                                                                                          |
| **`super()`**       | Construtores de todas as filhas chamam `super(...)`; `PrevisaoMediaMovel.prever()` e `PrevisaoComparacao.prever()` chamam **`super.prever(historico)`**                                                     | A classe-base valida (histórico nulo, sensores misturados, unidades diferentes, quantidade mínima); as filhas só calculam. Por isso `prever()` é **concreto** na classe-base — um método abstrato não poderia ser chamado com `super`.                                                                                            |

**Escolha sem `if`/`instanceof`:** a estratégia é escolhida por um `EnumMap<TipoEstrategia, Function<…>>` na `FabricaPrevisao`. Não há `if (tipo == ...)` nem `instanceof` no código de produção para selecionar estratégias.

**Por que três classes de sensor e não uma só?** Cada uma tem comportamento físico próprio em `simularProximaLeitura()`:

- **Pluviômetro:** irregular — oscilação comum, "pancadas" (salto de +15 a +40 mm/h, 10% de chance) e estiagens (queda para 30%, 10%).
- **Nível de água:** inércia — enche aos poucos (+2 cm em média) e às vezes drena (−5 a −15 cm, 15%).
- **Umidade do solo:** saturação assintótica — o incremento diminui à medida que se aproxima de 100%.

O que é igual (id, tipo, falha simulada, estado operante) fica uma única vez em `SensorSimulado`.

## 8. Exceções customizadas

Ambas estendem `RuntimeException`. Motivos: o Spring só desfaz transações automaticamente para exceções não verificadas, e as validações acontecem em construtores/setters do domínio — obrigar `throws` em toda a cadeia não traria ganho. A captura com mensagem acontece no menu (`MenuPrincipal`, `MenuConfiguracao`). **Não há `catch` vazio** no projeto.

| Exceção                      | Quando é lançada (exemplos reais do código)                                                                                                                                                                                                                                                                                                                                                                                                        | Onde é capturada                                                                                                                                                                  |
| ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `DadosInvalidosException`    | Medição negativa (`Medicao.setValor`), fora da faixa (umidade 120%), NaN, unidade incompatível (`setUnidade`), data futura; limiares negativos ou fora de ordem (`Limiares`); limiar crítico acima da faixa do sensor e população/área negativas (`DadosTerritorio`); janela/período inválidos (`ConfiguracaoSistema`); histórico que mistura sensores (`EstrategiaPrevisao`); sensor inexistente (`ServicoMonitoramento`); exportar CSV sem dados | `MenuPrincipal.realizarLeituras()` (por sensor: "Leitura REJEITADA e não gravada"), laço do `MenuPrincipal.executar()` e `MenuConfiguracao.executar()` ("Alteração NÃO aplicada") |
| `FonteIndisponivelException` | Sensor inoperante/em manutenção (`SensorSimulado.lerValor`, `ServicoMonitoramento.realizarLeitura`); falha de comunicação simulada (5% de chance por leitura)                                                                                                                                                                                                                                                                                      | `MenuPrincipal.realizarLeituras()` (por sensor: os outros sensores continuam sendo lidos) e laço do menu                                                                          |

Exemplo real de saída (opção 2):

```
  [ERRO] Leitura REJEITADA e não gravada. Medição negativa rejeitada: o sensor NIV-01 (Nível de
         água) informou -1,0 cm; esse tipo de medição não pode ser negativo.
  [ERRO] Sem leitura. Falha de comunicação simulada com o sensor NIV-01 (Nível de água): a leitura
         não chegou ao sistema.
```

A leitura −1,0 é uma "leitura espúria" simulada de propósito (2% de chance) para representar defeito de calibração e demonstrar que a validação impede a gravação.

Além das duas exceções do domínio, o menu trata `DataAccessException` (falha de banco) e `IOException` (falha ao gravar CSV) com mensagens específicas, e `EntradaConsole.EntradaEncerradaException` para encerrar com segurança se a entrada do teclado acabar.

## 9. Persistência

- **Entidades JPA:** `Medicao` (tabela `medicao`) e `Alerta` (tabela `alerta`), com `@Entity`, `@Id`, `@GeneratedValue(IDENTITY)`, `@Column`, `@Enumerated(STRING)` e um índice `(sensor_id, data_hora)` em `medicao`.
- **Repositórios Spring Data JPA:** `MedicaoRepository` e `AlertaRepository` estendem `JpaRepository`. As consultas são **derivadas do nome do método** (ex.: `findBySensorIdOrderByDataHoraAscIdAsc`), e o Spring gera JPQL com parâmetros ligados — **nenhum SQL é montado por concatenação de Strings**.
- **Recursos:** conexões, transações e o `EntityManager` são gerenciados pelo Spring (pool HikariCP). Na exportação CSV o arquivo é fechado com _try-with-resources_.
- **O que é gravado:** toda medição válida (`ServicoMonitoramento.registrarMedicao`) e todo alerta emitido (`executarPrevisao` → `alertaRepository.save`).
- **Consultas no menu:** histórico por sensor ou completo em ordem cronológica (opção 4), alertas do mais recente para o mais antigo (opção 5), contagens por nível (opção 7).
- **Sobrevive ao reinício:** o H2 roda em **modo arquivo** (`./data/territorio-alerta.mv.db`) com `ddl-auto=update`, que cria as tabelas na primeira execução e as preserva nas seguintes. Verificado de duas formas: (a) teste automatizado `PersistenciaIntegracaoTest.dadosDevemSobreviverAoReinicio`, que sobe a aplicação, grava, encerra o contexto, sobe outro contexto e lê o dado; (b) execução manual: 1ª execução gravou 24 medições e 3 alertas; na 2ª execução as opções 4 e 5 listaram os 24/3 registros e a nova previsão gerou o alerta nº 4.

## 10. Padrões criacionais

### Factory — `FabricaPrevisao`

- **Problema:** existem várias estratégias, cada uma com parâmetros próprios (janela N, tamanho do período). Sem a fábrica, o serviço precisaria de `if (tipo == MEDIA_MOVEL) new PrevisaoMediaMovel(cfg.getJanela()) else ...` — e cada nova estratégia exigiria mexer no serviço.
- **Por que ali:** a criação de estratégias é o único ponto do sistema onde "qual classe instanciar" varia conforme a configuração/escolha do usuário. Sensores e entidades não precisam disso (são sempre os mesmos).
- **Como melhora:** a associação tipo → classe fica num único `EnumMap<TipoEstrategia, Function<ConfiguracaoSistema, EstrategiaPrevisao>>`. Nova estratégia = nova entrada no mapa. O `ServicoMonitoramento` só chama `fabricaPrevisao.criar(tipo)` e usa o resultado de forma polimórfica.
- **Uso real:** `ServicoMonitoramento.executarPrevisao()` chama `fabricaPrevisao.criar(tipoEstrategia)` em toda previsão; a opção "estratégia padrão" usa `configuracao.getEstrategiaPadrao()`. A fábrica cria uma instância nova a cada chamada, já com os parâmetros **atuais** (mudar a janela no menu vale na próxima previsão). Testado em `PadroesCriacionaisTest` e `ServicoMonitoramentoTest.deveUsarEstrategiaCriadaPelaFabrica`.

### Singleton — `ConfiguracaoSistema`

- **Problema:** limiares, dados do território e parâmetros de previsão precisam ser **os mesmos** para o serviço, a fábrica, o menu de configuração e os testes. Cópias diferentes gerariam alertas incoerentes (o menu muda um limiar e o serviço continua usando o antigo).
- **Implementação:** construtor `private`, instância criada pelo idiom _initialization-on-demand holder_ (classe interna `Portador`), que é preguiçoso e thread-safe garantido pela JVM; métodos `synchronized` para leitura/alteração; método `getInstancia()`.
- **Integração com Spring:** o bean é declarado em `ConfiguracaoAplicacao` como `return ConfiguracaoSistema.getInstancia();` — o Spring **injeta a mesma instância**, não cria outra. Assim o padrão funciona tanto dentro quanto fora do container (ex.: testes unitários sem Spring).
- **Uso real:** `ServicoMonitoramento.executarPrevisao()` busca `configuracao.getLimiares(tipo)`; `FabricaPrevisao` lê janela/período; `MenuConfiguracao` altera limiares e parâmetros.

## 11. Estratégias de previsão

Ambas usam **apenas medições do mesmo sensor**, lidas do banco, e se recusam a prever com histórico insuficiente (o resultado vem como "NÃO EMITIDO", sem valor e sem alerta).

**Média móvel** (`PrevisaoMediaMovel`, janela padrão N = 5, configurável de 5 a 50)

- Previsão = média das últimas N medições.
- Exemplo: 20, 25, 30, 35, 40 → (20+25+30+35+40)/5 = **30**.
- Indica o patamar recente e suaviza picos isolados; não projeta crescimento. Também informa a tendência dentro da janela (primeira → última leitura).

**Comparação entre períodos** (`PrevisaoComparacao`, período padrão P = 3, ou seja, 6 medições; configurável de 3 a 25)

- Média do período anterior (P medições) e do período recente (P medições).
- Previsão = média recente + (média recente − média anterior).
- Exemplo: anterior 20, recente 30 → tendência +10 → previsão **40**.
- Antecipa a continuidade de uma subida/descida. O resultado é limitado à faixa física do sensor: chuva 30→10 projetaria −10, mas a previsão é 0 mm/h; umidade 80→95 projetaria 110%, mas a previsão é 100% (a explicação informa o ajuste).

Cada resultado mostra: sensor, região, estratégia, quantidade de medições consideradas, valor previsto, unidade, tendência e explicação em linguagem simples.

## 12. Sistema de alertas

| Tipo de sensor      | ATENÇÃO | ALERTA | CRÍTICO | Faixa válida |
| ------------------- | ------: | -----: | ------: | ------------ |
| Pluviômetro (mm/h)  |    10,0 |   25,0 |    50,0 | 0 a 200      |
| Nível de água (cm)  |    30,0 |   50,0 |    80,0 | 0 a 300      |
| Umidade do solo (%) |    70,0 |   85,0 |    95,0 | 0 a 100      |

> ⚠️ Estes valores são **parâmetros ilustrativos de simulação**, escolhidos para demonstrar o funcionamento. **Não são limites oficiais de segurança nem recomendações da Defesa Civil.**

- **Classificação** (`Limiares.classificar`): verifica do mais grave para o menos grave; vale "igual ou superior". Prevalece sempre o nível mais alto atingido. Abaixo do limiar de atenção não há alerta.
- **Configuração:** opção 6 → 2. Regra validada: `0 ≤ atenção < alerta < crítico ≤ máximo do sensor`. Valores inválidos são recusados e os anteriores mantidos.
- **Sem previsão, sem alerta:** se o histórico é insuficiente, `AvaliadorAlertas` não emite nada.
- **Explicação** (`AvaliadorAlertas.explicar`), exemplo real:

> CRÍTICO: o sensor PLU-01 [Pluviômetro (intensidade de chuva)] do ponto PM-01, localizado em Região Baixada do Córrego (fictícia), apresentou PREVISÃO de 68,6 mm/h pela estratégia 'Comparação entre períodos', igual ou acima do limite crítico configurado de 50,0 mm/h. Observou-se tendência de AUMENTO. Trata-se de uma previsão baseada em medições simuladas, e não da confirmação de que um alagamento ocorreu. Recomendação para avaliação da equipe: Realizar avaliação preventiva imediata da região, priorizar recursos e avaliar o acionamento da Defesa Civil.

## 13. Testes unitários

**Resultado real da execução (`mvn test`): 41 testes, 0 falhas, 0 erros, 0 ignorados.** Todos seguem o padrão AAA (comentários `// Arrange`, `// Act`, `// Assert`).

| Classe de teste                                    | Qtde | O que verifica                                                                                                                                                                                                                                                                                                                                                                                                                                |
| -------------------------------------------------- | ---: | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `MedicaoTest`                                      |    5 | Medição válida; chuva negativa (`assertThrows`); umidade 120%; unidade incompatível; NaN e data futura                                                                                                                                                                                                                                                                                                                                        |
| `PrevisaoMediaMovelTest`                           |    5 | Média 20..40 = 30; usa só as N mais recentes e ordena cronologicamente; silêncio com histórico insuficiente; sensores misturados (`assertThrows`); janela < 5                                                                                                                                                                                                                                                                                 |
| `PrevisaoComparacaoTest`                           |    5 | 20→30 prevê 40 (aumento); 60→50 prevê 40 (redução); limite da faixa física (0 e 100); 5 medições não bastam; mesmo histórico → resultados diferentes nas duas estratégias                                                                                                                                                                                                                                                                     |
| `LimiaresETerritorioTest`                          |    3 | Nível mais alto prevalece e "igual ao limiar" conta; limiares fora de ordem/negativos; território recusa limiar acima da faixa e população negativa sem perder os valores anteriores                                                                                                                                                                                                                                                          |
| `PadroesCriacionaisTest`                           |    3 | Singleton (mesma instância, construtor privado); Factory usa parâmetros atuais; Factory segue a estratégia padrão                                                                                                                                                                                                                                                                                                                             |
| `SensorTest`                                       |    4 | Leituras dentro da faixa (semente fixa); sensor inoperante/falha → `FonteIndisponivelException`; leitura espúria barrada pela `Medicao`; status do ponto (operacional/degradado/inoperante)                                                                                                                                                                                                                                                   |
| `ServicoMonitoramentoTest` (**Mockito**)           |    9 | Repositórios mockados com `@ExtendWith(MockitoExtension.class)`: alerta CRÍTICO gerado e gravado (`ArgumentCaptor`); nível ALERTA quando entre alerta e crítico; **sem alerta** abaixo dos limiares (`verify(..., never())`); sem alerta com histórico insuficiente; Factory pede 6 medições ao repositório na comparação; leitura válida gravada; leitura inválida não gravada; sensor inoperante não é lido nem gravado; sensor inexistente |
| `ExportadorCSVTest`                                |    3 | Cabeçalho, BOM, valores, unidade e data; escape de `;` e aspas; histórico vazio recusado                                                                                                                                                                                                                                                                                                                                                      |
| `PersistenciaIntegracaoTest` (Spring + H2 memória) |    4 | Gravação e consulta por sensor em ordem cronológica; valor inválido não chega ao banco; fluxo completo previsão → alerta gravado; **dados sobrevivem ao reinício** (H2 arquivo, dois contextos Spring)                                                                                                                                                                                                                                        |

**Prova de que os testes detectam erros (teste de mutação manual, executado):**

1. A média em `EstrategiaPrevisao.media()` foi trocada temporariamente por `max()` → **6 testes falharam** (ex.: `expected: <30.0> but was: <40.0>`).
2. A projeção em `PrevisaoComparacao` foi trocada por apenas `mediaRecente` → **5 testes falharam** (ex.: `expected: <40.0> but was: <30.0>`).

Ambas as alterações foram desfeitas e a suíte voltou a 41/41.

## 14. Exportação CSV (bônus)

1. Realize leituras (opção 2).
2. Escolha a opção **8 — Exportar histórico em CSV**.
3. Escolha um sensor (1–3) ou **4 — Todos os sensores**.
4. O sistema consulta o banco e informa o caminho do arquivo, por exemplo `exportacoes/medicoes_todas_20261118_155156.csv`.

Formato: UTF-8 com BOM, separador `;`, decimal com vírgula (abre direto no Excel em português). Colunas: `id_medicao;id_sensor;tipo_sensor;ponto_monitorado;regiao;valor;unidade;data_hora`. Campos com `;`, aspas ou quebra de linha são colocados entre aspas (RFC 4180). Exemplo real:

```
id_medicao;id_sensor;tipo_sensor;ponto_monitorado;regiao;valor;unidade;data_hora
1;PLU-01;Pluviômetro (intensidade de chuva);PM-01;Região Baixada do Córrego (fictícia);8,2;mm/h;18/11/2026 15:51:56
```

## 15. Como executar

### Pré-requisitos

- **JDK 21** ou superior: verifique com `java -version`.
- **Maven 3.9+** (`mvn -version`) **ou** o Maven Wrapper incluído (`mvnw` / `mvnw.cmd`), que baixa o Maven 3.9.11 automaticamente no primeiro uso (precisa de internet). IDEs como IntelliJ/Eclipse/VS Code também importam o `pom.xml` diretamente.
- Internet na **primeira** compilação, para o Maven baixar as dependências.

### Passo a passo (terminal, a partir da pasta `territorio-alerta`)

```bash
# 1) Compilar, rodar os 41 testes e gerar o jar executável
mvn clean package

# 2) Executar a aplicação (menu interativo)
java -jar target/territorio-alerta.jar
```

Com o Maven Wrapper (sem Maven instalado):

```bash
./mvnw clean package          # Linux/macOS
mvnw.cmd clean package        # Windows
java -jar target/territorio-alerta.jar
```

Outros comandos úteis:

```bash
mvn test                      # somente os testes
mvn spring-boot:run           # executa sem gerar o jar (use em terminal interativo)
```

**Roteiro sugerido para a demonstração:** 1 (sensores) → 2 com 8 rodadas → 3 (cada estratégia) → 5 (alertas) → 7 (status) → 8 (CSV) → 0 (sair) → executar de novo e abrir 4 e 5 para mostrar que os dados continuam lá.

> Se os acentos aparecerem trocados no terminal do Windows, execute `chcp 65001` antes ou use `java -Dstdout.encoding=UTF-8 -jar target/territorio-alerta.jar`.

> Execute **uma instância por vez**: o H2 em modo arquivo bloqueia o banco enquanto a aplicação está aberta.

## 16. Configuração do banco

`src/main/resources/application.properties` (aplicação):

```properties
spring.datasource.url=jdbc:h2:file:./data/territorio-alerta
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
```

- O arquivo `data/territorio-alerta.mv.db` é criado na primeira execução, **relativo à pasta de onde o programa é executado** (execute sempre a partir da pasta `territorio-alerta`).
- `ddl-auto=update` cria/atualiza as tabelas sem apagar dados.
- Para **zerar** o histórico, feche a aplicação e apague a pasta `data/`.

`src/test/resources/application.properties` (testes): `jdbc:h2:mem:territorio-teste` com `create-drop` — banco em memória, isolado, que nunca toca nos dados da aplicação. O menu é desligado nos testes (`territorio.menu.habilitado=false`).

## 17. Reflexão crítica

**Quando o sistema deve se calar?** Um número com uma casa decimal passa uma sensação de precisão que a realidade não sustenta. Com duas ou três leituras, uma média ou uma "tendência" é praticamente o próprio ruído do sensor: uma pancada de chuva isolada faria a comparação entre períodos projetar um valor alarmante, e uma pausa momentânea faria o sistema prever que tudo está bem. Por isso, o Território Alerta se recusa a prever com menos de 5 medições (média móvel) ou 6 (comparação) e mostra "NÃO EMITIDO" em vez de um valor — e, sem previsão, não emite alerta. O silêncio, porém, também comunica: um sensor inoperante não significa "sem risco", e sim "sem informação". Por isso o status distingue _ponto degradado_ de _situação normal_. Uma limitação que reconhecemos: o critério "5 leituras" é quantitativo; o sistema ainda não verifica se as leituras são recentes ou se há lacunas longas no histórico, o que também deveria torná-lo mais cauteloso.

**O que os dados não representam?** Os dados são simulados: não refletem chuva real, nem o relevo, a impermeabilização do solo, o estado dos bueiros ou o lixo que entope uma galeria. Um único ponto com três sensores não descreve uma região inteira — a rua de baixo pode alagar enquanto o sensor, alguns metros acima, registra nível baixo. As previsões são tendências aritméticas simples, sem considerar a previsão meteorológica nem a maré ou a vazão de rios a montante. Os limiares são ilustrativos. E os dados não mostram quem é mais vulnerável: uma mesma lâmina d'água tem impacto muito diferente para uma pessoa com mobilidade reduzida ou para uma casa abaixo do nível da rua.

**Por que o alerta apoia a decisão, em vez de decidir por ela?** Porque quem decide conhece o que o sistema não vê: obras recentes, uma galeria que já foi limpa, um evento que concentra pessoas na região, a experiência de enchentes anteriores. Decisões como evacuar, interditar uma via ou acionar a Defesa Civil têm custos reais e responsabilidade legal e moral — um falso alarme desgasta a confiança da comunidade, e um alarme ignorado pode custar vidas. Por isso cada alerta explica qual sensor, qual valor previsto, qual limite e por quê, deixa claro que é uma _previsão_ e recomenda uma _avaliação_, não uma ação automática. O papel do sistema é antecipar a conversa e organizar a informação; a interpretação e a responsabilidade continuam humanas.

## 18. Uso de Inteligência Artificial

A inteligência artificial foi utilizada como ferramenta de apoio ao desenvolvimento do projeto, contribuindo para a geração inicial de código, implementação de funcionalidades, elaboração de testes e documentação técnica.

A equipe foi responsável por definir os requisitos, escolher as funcionalidades que seriam desenvolvidas, estabelecer a arquitetura do sistema e dividir as responsabilidades entre os integrantes.

Durante o processo, a IA foi utilizada como apoio na implementação e resolução de problemas técnicos, enquanto os integrantes ficaram responsáveis por revisar, estudar e compreender os componentes relacionados às suas atribuições, além de validar o funcionamento das funcionalidades desenvolvidas.

## 19. Divisão de tarefas

| Integrante (nome / RM)       | Responsabilidade            | Principais arquivos                                                                                                                                                                                                                               |
| ---------------------------- | --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| _André Gouveia RM564219_     | Domínio e exceções          | `pom.xml` e estrutura Maven, `excecao/*`, `ComponenteTerritorial`, `PontoMonitoramento`, `Sensor` e os 3 sensores, `DadosTerritorio`, `Limiares`, enums do domínio, menu do console (`console/*`, `SistemaMonitoramento`)                         |
| _Luiz Claro RM563014_        | Persistência                | `Medicao`, `Alerta`, `persistencia/*`, `application.properties`, `ExportadorCSV`, `Formatos`, `PersistenciaIntegracaoTest`, `ExportadorCSVTest`, README                                                                                           |
| _Gabriel Nacarelli RM565298_ | Previsões, alertas e testes | `ResultadoPrevisao`, `EstrategiaPrevisao` e filhas, `ConfiguracaoSistema`, `FabricaPrevisao`, `AvaliadorAlertas`, `ServicoMonitoramento` e records do serviço, `ConfiguracaoAplicacao`, testes de domínio/previsões/padrões e o teste com Mockito |

Todos os integrantes devem ter commits no GitHub, com mensagens claras.
