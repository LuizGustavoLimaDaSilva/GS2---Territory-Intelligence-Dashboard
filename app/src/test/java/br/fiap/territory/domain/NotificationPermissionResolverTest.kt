package br.fiap.territory.domain

import br.fiap.territory.domain.model.NotificationPermission.DENIED
import br.fiap.territory.domain.model.NotificationPermission.DENIED_FOREVER
import br.fiap.territory.domain.model.NotificationPermission.GRANTED
import br.fiap.territory.domain.notification.NotificationPermissionResolver.resolve
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionResolverTest {
    @Test fun concedidaQuandoPermissaoEHabilitadaNoSistema() =
        assertEquals(GRANTED, resolve(notificationsEnabled = true, runtimePermissionGranted = true, askedBefore = false, canAskAgain = true))

    @Test fun naoPedidaAindaEquivaleANegadaComPossibilidadeDePedir() =
        assertEquals(DENIED, resolve(false, false, askedBefore = false, canAskAgain = true))

    @Test fun negadaUmaVezPodePedirDeNovo() =
        assertEquals(DENIED, resolve(false, false, askedBefore = true, canAskAgain = true))

    @Test fun negadaParaSempreQuandoOSistemaNaoMostraMaisODialogo() =
        assertEquals(DENIED_FOREVER, resolve(false, false, askedBefore = true, canAskAgain = false))

    @Test fun androidAntigoComNotificacoesDesligadasSoResolvePorConfiguracoes() =
        // antes do Android 13 a permissão de execução não existe (runtimePermissionGranted = true)
        assertEquals(DENIED_FOREVER, resolve(false, true, askedBefore = false, canAskAgain = true))

    @Test fun permissaoConcedidaMasNotificacoesDesligadasNoSistema() =
        assertEquals(DENIED_FOREVER, resolve(false, true, askedBefore = true, canAskAgain = true))
}
