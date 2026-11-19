package com.territorio.alerta;

import com.territorio.alerta.dominio.EstrategiaPrevisao;
import com.territorio.alerta.dominio.PrevisaoComparacao;
import com.territorio.alerta.dominio.PrevisaoMediaMovel;
import com.territorio.alerta.dominio.TipoEstrategia;
import com.territorio.alerta.excecao.DadosInvalidosException;
import com.territorio.alerta.servico.ConfiguracaoSistema;
import com.territorio.alerta.servico.FabricaPrevisao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PadroesCriacionaisTest {

    private final ConfiguracaoSistema configuracao = ConfiguracaoSistema.getInstancia();
    private final FabricaPrevisao fabrica = new FabricaPrevisao(configuracao);

    @BeforeEach
    @AfterEach
    void restaurarConfiguracao() {
        configuracao.restaurarPadroes();
    }

    @Test
    @DisplayName("Singleton: sempre a mesma instância e construtor privado")
    void configuracaoDeveSerSingleton() {
        Constructor<?>[] construtores = ConfiguracaoSistema.class.getDeclaredConstructors();

        ConfiguracaoSistema primeira = ConfiguracaoSistema.getInstancia();
        ConfiguracaoSistema segunda = ConfiguracaoSistema.getInstancia();

        assertSame(primeira, segunda);
        assertEquals(1, construtores.length);
        assertTrue(Modifier.isPrivate(construtores[0].getModifiers()));
    }

    @Test
    @DisplayName("Factory cria a estratégia pedida já com os parâmetros atuais da configuração")
    void fabricaDeveUsarParametrosDaConfiguracao() {
        configuracao.setJanelaMediaMovel(7);
        configuracao.setTamanhoPeriodoComparacao(4);

        EstrategiaPrevisao media = fabrica.criar(TipoEstrategia.MEDIA_MOVEL);
        EstrategiaPrevisao comparacao = fabrica.criar(TipoEstrategia.COMPARACAO);

        assertInstanceOf(PrevisaoMediaMovel.class, media);
        assertInstanceOf(PrevisaoComparacao.class, comparacao);
        assertEquals(7, media.quantidadeNecessaria());
        assertEquals(8, comparacao.quantidadeNecessaria());
    }

    @Test
    @DisplayName("Factory segue a estratégia padrão definida no Singleton e recusa tipo nulo")
    void fabricaDeveSeguirEstrategiaPadrao() {
        configuracao.setEstrategiaPadrao(TipoEstrategia.COMPARACAO);

        EstrategiaPrevisao padrao = fabrica.criarPadrao();

        assertInstanceOf(PrevisaoComparacao.class, padrao);
        assertThrows(DadosInvalidosException.class, () -> fabrica.criar(null));
        assertThrows(DadosInvalidosException.class, () -> configuracao.setJanelaMediaMovel(3));
    }
}
