package com.healthcraft.adaptivehz

import com.healthcraft.adaptivehz.service.AdaptiveCoordinator
import com.healthcraft.adaptivehz.service.AdaptiveState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveCoordinatorTest {

    @Test
    fun testServiceActiveStateTransitions() {
        AdaptiveCoordinator.setServiceActive(false)
        assertFalse(AdaptiveCoordinator.isServiceActive.value)

        AdaptiveCoordinator.setServiceActive(true)
        assertTrue(AdaptiveCoordinator.isServiceActive.value)

        AdaptiveCoordinator.setServiceActive(false)
        assertFalse(AdaptiveCoordinator.isServiceActive.value)
    }

    @Test
    fun testAdaptiveStateTransitions() {
        AdaptiveCoordinator.setAdaptiveState(AdaptiveState.IDLE_TIMEOUT)
        assertEquals(AdaptiveState.IDLE_TIMEOUT, AdaptiveCoordinator.adaptiveState.value)

        AdaptiveCoordinator.setAdaptiveState(AdaptiveState.ACTIVE_TOUCH)
        assertEquals(AdaptiveState.ACTIVE_TOUCH, AdaptiveCoordinator.adaptiveState.value)

        AdaptiveCoordinator.setAdaptiveState(AdaptiveState.LOCKED_APP)
        assertEquals(AdaptiveState.LOCKED_APP, AdaptiveCoordinator.adaptiveState.value)
    }

    @Test
    fun testActivePackageNameState() {
        AdaptiveCoordinator.setActivePackageName("com.google.android.youtube")
        assertEquals("com.google.android.youtube", AdaptiveCoordinator.activePackageName.value)

        AdaptiveCoordinator.setActivePackageName("")
        assertEquals("", AdaptiveCoordinator.activePackageName.value)
    }

    @Test
    fun testUserInteractionEventEmitted() = runBlocking {
        val eventReceived = CompletableDeferred<Boolean>()
        val job = launch(Dispatchers.Default) {
            AdaptiveCoordinator.userInteractionEvents
                .onSubscription {
                    AdaptiveCoordinator.notifyUserInteraction()
                }
                .collect {
                    eventReceived.complete(true)
                }
        }

        val result = withTimeout(2000) {
            eventReceived.await()
        }
        job.cancel()
        assertTrue(result)
    }
}
