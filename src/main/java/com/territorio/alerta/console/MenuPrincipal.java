package com.territorio.alerta.console;

import com.territorio.alerta.dominio.Alerta;
import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.NivelAlerta;
import com.territorio.alerta.dominio.Sensor;
import com.territorio.alerta.dominio.TipoEstrategia;
import com.territorio.alerta.excecao.DadosInvalidosException;
import com.territorio.alerta.excecao.FonteIndisponivelException;
import com.territorio.alerta.servico.AnalisePrevisao;
import com.territorio.alerta.servico.ConfiguracaoSistema;
import com.territorio.alerta.servico.ServicoMonitoramento;
import com.territorio.alerta.servico.SituacaoSensor;
import com.territorio.alerta.servico.StatusMonitoramento;
import com.territorio.alerta.util.Formatos;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Component
public class MenuPrincipal {

    private static final int MAXIMO_RODADAS = 20;

    private final ServicoMonitoramento servico;
    private final ConfiguracaoSistema configuracao;
    private final MenuConfiguracao menuConfiguracao;
    private final EntradaConsole entrada;

    public MenuPrincipal(ServicoMonitoramento servico, ConfiguracaoSistema configuracao,
                         MenuConfiguracao menuConfiguracao, EntradaConsole entrada) {
        this.servico = servico;
        this.configuracao = configuracao;
        this.menuConfiguracao = menuConfiguracao;
        this.entrada = entrada;
    }

    public void executar() {
        boolean continuar = true;
        while (continuar) {
            exibirMenu();
            try {
                int opcao = entrada.lerInteiro("Escolha uma opção: ");
                continuar = executarOpcao(opcao);
            } catch (EntradaConsole.EntradaEncerradaException e) {
                System.out.println("\nEntrada do console encerrada. Finalizando o Território Alerta.");
                continuar = false;
            } catch (DadosInvalidosException e) {
                Tela.erro("Dados inválidos: " + e.getMessage());
            } catch (FonteIndisponivelException e) {
                Tela.erro("Fonte de dados indisponível: " + e.getMessage());
            } catch (DataAccessException e) {
                Tela.erro("Falha ao acessar o banco de dados: " + e.getMostSpecificCause().getMessage()
                        + " Verifique se outra instância do programa está usando o arquivo do banco.");
            }
        }
    }

    private void exibirMenu() {
        System.out.println();
        System.out.println("              TERRITÓRIO ALERTA");
        System.out.println("   MONITORAMENTO PREDITIVO DE ALAGAMENTOS");
        System.out.println("---------------------------------------------");
        System.out.println("1. Verificar sensores");
        System.out.println("2. Realizar leituras e registrar medições");
        System.out.println("3. Executar previsões");
        System.out.println("4. Consultar histórico de medições");
        System.out.println("5. Consultar alertas emitidos");
        System.out.println("6. Gerenciar dados do território e limiares");
        System.out.println("7. Consultar status completo do monitoramento");
        System.out.println("8. Exportar histórico em CSV");
        System.out.println("0. Sair");
        System.out.println();
    }

    private boolean executarOpcao(int opcao) {
        switch (opcao) {
            case 1 -> verificarSensores();
            case 2 -> realizarLeituras();
            case 3 -> executarPrevisoes();
            case 4 -> consultarHistorico();
            case 5 -> consultarAlertas();
            case 6 -> menuConfiguracao.executar();
            case 7 -> exibirStatus();
            case 8 -> exportarCsv();
            case 0 -> {
                System.out.println("Encerrando o Território Alerta. Os dados permanecem salvos no banco.");
                return false;
            }
            default -> Tela.aviso("Opção inexistente: " + opcao + ". Escolha um número entre 0 e 8.");
        }
        return true;
    }

    private void verificarSensores() {
        Tela.titulo("VERIFICAÇÃO DOS SENSORES");
        List<SituacaoSensor> situacoes = servico.verificarSensores();
        System.out.println("  " + servico.getPonto().descrever());
        System.out.println();
        for (SituacaoSensor s : situacoes) {
            System.out.printf("  %-7s | %-36s | unidade: %-5s | %-12s | %d medição(ões) no banco%n",
                    s.id(), s.tipo().getDescricao(), s.unidade(),
                    s.funcionando() ? "FUNCIONANDO" : "INOPERANTE", s.medicoesRegistradas());
        }
        System.out.println();
        System.out.println("  Deseja simular manutenção/falha de um sensor? (alterna entre FUNCIONANDO e INOPERANTE)");
        Sensor sensor = escolherSensor(true);
        if (sensor != null) {
            boolean funcionando = servico.alternarOperacaoSensor(sensor.getId());
            Tela.sucesso("Sensor " + sensor.getId() + " agora está " + (funcionando ? "FUNCIONANDO." : "INOPERANTE."));
            System.out.println("  Status do ponto: " + servico.getPonto().getStatus().getDescricao());
        }
    }

    private void realizarLeituras() {
        Tela.titulo("LEITURAS DOS SENSORES");
        int rodadas = entrada.lerInteiro("Quantas rodadas de leitura deseja realizar? (1 a " + MAXIMO_RODADAS + "): ",
                1, MAXIMO_RODADAS);
        int gravadas = 0;
        int rejeitadas = 0;
        for (int rodada = 1; rodada <= rodadas; rodada++) {
            System.out.println("  Rodada " + rodada + ":");
            for (Sensor sensor : servico.listarSensores()) {
                try {
                    Medicao medicao = servico.realizarLeitura(sensor.getId());
                    gravadas++;
                    Tela.sucesso(String.format("%s: %s registrado (medição #%d).", sensor.getId(),
                            Formatos.valor(medicao.getValor(), medicao.getUnidade()), medicao.getId()));
                } catch (DadosInvalidosException e) {
                    rejeitadas++;
                    Tela.erro("Leitura REJEITADA e não gravada. " + e.getMessage());
                } catch (FonteIndisponivelException e) {
                    rejeitadas++;
                    Tela.erro("Sem leitura. " + e.getMessage());
                }
            }
        }
        System.out.println();
        System.out.printf("  Resumo: %d medição(ões) gravada(s) no banco; %d leitura(s) sem registro.%n", gravadas, rejeitadas);
    }

    private void executarPrevisoes() {
        Tela.titulo("PREVISÕES");
        TipoEstrategia[] estrategias = TipoEstrategia.values();
        for (int i = 0; i < estrategias.length; i++) {
            System.out.printf("  %d. %s (%s)%n", i + 1, estrategias[i].getNome(), estrategias[i].getDescricao());
        }
        System.out.printf("  %d. Usar a estratégia padrão configurada (%s)%n", estrategias.length + 1,
                configuracao.getEstrategiaPadrao().getNome());
        System.out.println("  0. Voltar");
        int escolha = entrada.lerInteiro("Estratégia: ", 0, estrategias.length + 1);
        if (escolha == 0) {
            return;
        }
        List<AnalisePrevisao> analises = escolha == estrategias.length + 1
                ? servico.executarPrevisoesComEstrategiaPadrao()
                : servico.executarPrevisoes(estrategias[escolha - 1]);
        analises.forEach(Tela::analise);
        long emitidos = analises.stream().filter(a -> a.alertaEmitido().isPresent()).count();
        System.out.println();
        System.out.println("  Previsões executadas: " + analises.size() + " | Alertas emitidos e gravados: " + emitidos);
        Tela.aviso("As previsões são estimativas simples baseadas em medições SIMULADAS. Elas apoiam, mas não substituem, "
                + "a avaliação das equipes responsáveis.");
    }

    private void consultarHistorico() {
        Tela.titulo("HISTÓRICO DE MEDIÇÕES (BANCO DE DADOS)");
        Sensor sensor = escolherSensorOuTodos();
        List<Medicao> medicoes = sensor == null ? servico.consultarHistoricoCompleto() : servico.consultarHistorico(sensor.getId());
        Tela.tabelaMedicoes(medicoes);
        System.out.println("  Total: " + medicoes.size() + " medição(ões).");
    }

    private void consultarAlertas() {
        Tela.titulo("ALERTAS EMITIDOS (BANCO DE DADOS)");
        List<Alerta> alertas = servico.consultarAlertas();
        if (alertas.isEmpty()) {
            System.out.println("  Nenhum alerta foi emitido até agora.");
            return;
        }
        alertas.forEach(Tela::alerta);
        System.out.println("  Total: " + alertas.size() + " alerta(s).");
    }

    private void exibirStatus() {
        StatusMonitoramento status = servico.obterStatus();
        Tela.titulo("STATUS COMPLETO DO MONITORAMENTO");
        System.out.println("  Região monitorada ....: " + status.codigoRegiao() + " - " + status.nomeRegiao());
        System.out.println("  Ponto ................: " + status.pontoDescricao());
        System.out.println("  Status do ponto ......: " + status.statusPonto().getDescricao());
        System.out.println("  Sensores:");
        for (SituacaoSensor s : status.sensores()) {
            System.out.printf("    - %-7s %-36s %-12s %d medição(ões)%n", s.id(), s.tipo().getDescricao(),
                    s.funcionando() ? "FUNCIONANDO" : "INOPERANTE", s.medicoesRegistradas());
        }
        System.out.println("  Medições registradas .: " + status.totalMedicoes());
        System.out.println("  Últimas medições:");
        Tela.tabelaMedicoes(status.ultimasMedicoes());
        System.out.println("  Últimas previsões (nesta sessão):");
        if (status.ultimasPrevisoes().isEmpty()) {
            System.out.println("    Nenhuma previsão executada nesta sessão.");
        }
        for (AnalisePrevisao a : status.ultimasPrevisoes()) {
            var r = a.resultado();
            System.out.printf("    - %-7s %-26s %s%s%n", r.getSensorId() == null ? "-" : r.getSensorId(), r.getEstrategia(),
                    r.isConclusivo() ? Formatos.valor(r.getValorPrevisto(), r.getUnidade()) : "sem previsão (histórico insuficiente)",
                    a.alertaEmitido().map(al -> "  -> " + al.getNivel().getRotulo()).orElse(""));
        }
        System.out.println("  Alertas emitidos .....: " + status.totalAlertas());
        for (NivelAlerta nivel : NivelAlerta.values()) {
            System.out.printf("    - %-8s %d%n", nivel.getRotulo(), status.alertasPorNivel().get(nivel));
        }
        Tela.paragrafo("  Situação geral .......: ", status.situacaoGeral());
    }

    private void exportarCsv() {
        Tela.titulo("EXPORTAR HISTÓRICO EM CSV");
        Sensor sensor = escolherSensorOuTodos();
        try {
            Path arquivo = servico.exportarHistoricoCsv(sensor == null ? null : sensor.getId());
            Tela.sucesso("Arquivo CSV gerado em: " + arquivo);
        } catch (IOException e) {
            Tela.erro("Não foi possível gravar o arquivo CSV: " + e.getMessage());
        }
    }
    
    private Sensor escolherSensor(boolean permitirNenhum) {
        List<Sensor> sensores = servico.listarSensores();
        for (int i = 0; i < sensores.size(); i++) {
            System.out.printf("  %d. %s - %s%n", i + 1, sensores.get(i).getId(), sensores.get(i).getTipo().getDescricao());
        }
        if (permitirNenhum) {
            System.out.println("  0. Não alterar / voltar");
        }
        int escolha = entrada.lerInteiro("Sensor: ", permitirNenhum ? 0 : 1, sensores.size());
        return escolha == 0 ? null : sensores.get(escolha - 1);
    }

    private Sensor escolherSensorOuTodos() {
        List<Sensor> sensores = servico.listarSensores();
        for (int i = 0; i < sensores.size(); i++) {
            System.out.printf("  %d. %s - %s%n", i + 1, sensores.get(i).getId(), sensores.get(i).getTipo().getDescricao());
        }
        System.out.printf("  %d. Todos os sensores%n", sensores.size() + 1);
        int escolha = entrada.lerInteiro("Sensor: ", 1, sensores.size() + 1);
        return escolha == sensores.size() + 1 ? null : sensores.get(escolha - 1);
    }
}
