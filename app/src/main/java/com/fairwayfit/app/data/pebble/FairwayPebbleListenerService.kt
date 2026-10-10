package com.fairwayfit.app.data.pebble

import com.fairwayfit.app.FairwayFitApp
import io.rebble.pebblekit2.client.BasePebbleListenerService
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.ReceiveResult
import io.rebble.pebblekit2.common.model.WatchIdentifier
import java.util.UUID

/** Bound by the Pebble phone app while the FairwayFit watchapp is open on the watch. */
class FairwayPebbleListenerService : BasePebbleListenerService() {

    private val pebble: PebbleManager
        get() = (application as FairwayFitApp).container.pebble

    override suspend fun onMessageReceived(
        watchappUUID: UUID,
        data: PebbleDictionary,
        watch: WatchIdentifier
    ): ReceiveResult {
        if (watchappUUID != PebbleProtocol.APP_UUID) return ReceiveResult.Nack
        return pebble.onWatchMessage(data)
    }

    override fun onAppOpened(watchappUUID: UUID, watch: WatchIdentifier) {
        if (watchappUUID == PebbleProtocol.APP_UUID) pebble.onWatchAppOpened()
    }
}
