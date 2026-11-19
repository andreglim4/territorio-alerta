package com.territorio.alerta.console;

import com.territorio.alerta.dominio.DadosTerritorio;
import com.territorio.alerta.dominio.Limiares;
import com.territorio.alerta.dominio.TipoEstrategia;
import com.territorio.alerta.dominio.TipoSensor;
import com.territorio.alerta.excecao.DadosInvalidosException;
import com.territorio.alerta.servico.ConfiguracaoSistema;
import com.territorio.alerta.util.Formatos;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MenuConfiguracao {

    private final ConfiguracaoSistema configuracao;
    private final EntradaConsole entrada;

    public MenuConfiguracao(ConfiguracaoSistema configuracao, EntradaConsole entrada) {
        this.configuracao = configuracao;
        this.entrada = entrada;
    }

    public void executar() {
        boolean voltar = false;
        while (!voltar) {
            Tela.titulo("DADOS DO TERRITÓRIO E LIMIARES");
            System.out.println("  1. Consultar dados do território, limiares e parâmetros");
            System.out.println("  2. Alterar limiares de um tipo de sensor");
            System.out.println("  3. Alterar dados do território (população, área, pontos vulneráveis)");
            System.out.println("  4. Alterar estratégia de previsão padrão");
            System.out.println("  5. Alterar parâmetros das estratégias (janela da média móvel, tamanho do período)");
            System.out.println("  6. Restaurar valores padrão de simulação");
            System.out.println("  0. Voltar ao menu principal");
            int opcao = entrada.lerInteiro("Opção: ", 0, 6);
            try {
                switch (opcao) {
                    case 1 -> exibirConfiguracao();
                    case 2 -> alterarLimiares();
                    case 3 -> alterarTerritorio();
                    case 4 -> alterarEstrategiaPadrao();
                    case 5 -> alterarParametrosEstrategias();
                    case 6 -> {
                        configuracao.restaurarPadroes();
                        Tela.sucesso("Valores padrão de simulação restaurados.");
                    }
                    default -> voltar = true;
                }
            } catch (DadosInvalidosException e) {
                Tela.erro("Alteração NÃO aplicada. " + e.getMessage());
            }
        }
    }

    private void exibirConfiguracao() {
        DadosTerritorio t = configuracao.getDadosTerritorio();
        System.out.println("  Região ..............: " + t.getCodigoRegiao() + " - " + t.getNomeRegiao());
        System.out.println("  População estimada ..: " + t.getPopulacaoEstimada() + " habitantes");
        System.out.println("  Área ................: " + Formatos.numero(t.getAreaKm2()) + " km²");
        System.out.println("  Pontos vulneráveis ..: " + t.getPontosVulneraveis());
        System.out.println("  Estratégia padrão ...: " + configuracao.getEstrategiaPadrao().getNome());
        System.out.println("  Janela média móvel ..: " + configuracao.getJanelaMediaMovel() + " medições");
        System.out.println("  Período comparação ..: " + configuracao.getTamanhoPeriodoComparacao()
                + " medições por período (" + 2 * configuracao.getTamanhoPeriodoComparacao() + " no total)");
        System.out.println();
        System.out.println("  Limiares (parâmetros ILUSTRATIVOS de simulação, não são limites oficiais):");
        System.out.printf("  %-36s %10s %10s %10s%n", "SENSOR", "ATENÇÃO", "ALERTA", "CRÍTICO");
        for (Map.Entry<TipoSensor, Limiares> e : t.getTodosLimiares().entrySet()) {
            String unidade = e.getKey().getUnidade();
            System.out.printf("  %-36s %10s %10s %10s%n", e.getKey().getDescricao(),
                    Formatos.valor(e.getValue().getAtencao(), unidade), Formatos.valor(e.getValue().getAlerta(), unidade),
                    Formatos.valor(e.getValue().getCritico(), unidade));
        }
    }

    private void alterarLimiares() {
        TipoSensor[] tipos = TipoSensor.values();
        for (int i = 0; i < tipos.length; i++) {
            Limiares atuais = configuracao.getLimiares(tipos[i]);
            System.out.printf("  %d. %s (%s) - atuais: %s / %s / %s%n", i + 1, tipos[i].getDescricao(), tipos[i].getUnidade(),
                    Formatos.numero(atuais.getAtencao()), Formatos.numero(atuais.getAlerta()), Formatos.numero(atuais.getCritico()));
        }
        TipoSensor tipo = tipos[entrada.lerInteiro("Tipo de sensor: ", 1, tipos.length) - 1];
        System.out.println("  Regra: 0 <= atenção < alerta < crítico <= " + Formatos.valor(tipo.getValorMaximo(), tipo.getUnidade()));
        double atencao = entrada.lerDecimal("  Novo limiar de ATENÇÃO (" + tipo.getUnidade() + "): ");
        double alerta = entrada.lerDecimal("  Novo limiar de ALERTA (" + tipo.getUnidade() + "): ");
        double critico = entrada.lerDecimal("  Novo limiar CRÍTICO (" + tipo.getUnidade() + "): ");
        configuracao.alterarLimiares(tipo, atencao, alerta, critico);
        Tela.sucesso("Limiares de " + tipo.getDescricao() + " atualizados.");
    }

    private void alterarTerritorio() {
        DadosTerritorio t = configuracao.getDadosTerritorio();
        System.out.println("  1. População estimada (atual: " + t.getPopulacaoEstimada() + ")");
        System.out.println("  2. Área em km² (atual: " + Formatos.numero(t.getAreaKm2()) + ")");
        System.out.println("  3. Pontos vulneráveis (atual: " + t.getPontosVulneraveis() + ")");
        System.out.println("  0. Voltar");
        switch (entrada.lerInteiro("Campo: ", 0, 3)) {
            case 1 -> t.setPopulacaoEstimada(entrada.lerInteiro("  Nova população estimada: "));
            case 2 -> t.setAreaKm2(entrada.lerDecimal("  Nova área (km²): "));
            case 3 -> t.setPontosVulneraveis(entrada.lerInteiro("  Nova quantidade de pontos vulneráveis: "));
            default -> {
                return;
            }
        }
        Tela.sucesso("Dados do território atualizados.");
    }

    private void alterarEstrategiaPadrao() {
        TipoEstrategia[] estrategias = TipoEstrategia.values();
        for (int i = 0; i < estrategias.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, estrategias[i].getNome());
        }
        configuracao.setEstrategiaPadrao(estrategias[entrada.lerInteiro("Estratégia padrão: ", 1, estrategias.length) - 1]);
        Tela.sucesso("Estratégia padrão: " + configuracao.getEstrategiaPadrao().getNome());
    }

    private void alterarParametrosEstrategias() {
        System.out.println("  1. Janela da média móvel (atual: " + configuracao.getJanelaMediaMovel() + ")");
        System.out.println("  2. Tamanho de cada período da comparação (atual: " + configuracao.getTamanhoPeriodoComparacao() + ")");
        System.out.println("  0. Voltar");
        switch (entrada.lerInteiro("Parâmetro: ", 0, 2)) {
            case 1 -> configuracao.setJanelaMediaMovel(entrada.lerInteiro("  Nova janela (5 a " + ConfiguracaoSistema.JANELA_MAXIMA + "): "));
            case 2 -> configuracao.setTamanhoPeriodoComparacao(entrada.lerInteiro("  Novo tamanho (3 a " + ConfiguracaoSistema.PERIODO_MAXIMO + "): "));
            default -> {
                return;
            }
        }
        Tela.sucesso("Parâmetro atualizado; vale a partir da próxima previsão.");
    }
}
