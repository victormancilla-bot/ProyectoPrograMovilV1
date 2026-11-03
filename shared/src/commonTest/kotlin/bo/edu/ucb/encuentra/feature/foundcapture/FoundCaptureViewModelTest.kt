package bo.edu.ucb.encuentra.feature.foundcapture

import bo.edu.ucb.encuentra.core.domain.repository.CapturedPhotosStore
import bo.edu.ucb.encuentra.core.presentation.CameraPermissionResult
import bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel.FoundCaptureEffect
import bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel.FoundCaptureEvent
import bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel.FoundCaptureState
import bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel.FoundCaptureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FoundCaptureViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val store = CapturedPhotosStore()

    @Test
    fun needsAtLeastOnePhotoToContinue() = runTest(dispatcher) {
        val vm = FoundCaptureViewModel(store)
        advanceUntilIdle()
        assertFalse(vm.state.value.canContinue)

        val effect = async { vm.effects.first() }
        vm.handleEvent(FoundCaptureEvent.OnContinue)
        vm.handleEvent(FoundCaptureEvent.OnPhotoAdded("file:///1.jpg"))
        advanceUntilIdle()
        assertTrue(vm.state.value.canContinue)
        vm.handleEvent(FoundCaptureEvent.OnContinue)
        advanceUntilIdle()

        // El primer "Continuar" (sin fotos) no navegó.
        assertEquals(FoundCaptureEffect.NavigateToForm, effect.await())
    }

    @Test
    fun photosCanBeRemovedAndAreLimited() = runTest(dispatcher) {
        val vm = FoundCaptureViewModel(store)
        repeat(FoundCaptureState.MAX_PHOTOS + 2) { vm.handleEvent(FoundCaptureEvent.OnPhotoAdded("file:///$it.jpg")) }
        advanceUntilIdle()
        assertEquals(FoundCaptureState.MAX_PHOTOS, vm.state.value.photos.size)
        assertFalse(vm.state.value.canAddPhoto)

        vm.handleEvent(FoundCaptureEvent.OnRemovePhoto("file:///0.jpg"))
        advanceUntilIdle()
        assertEquals(FoundCaptureState.MAX_PHOTOS - 1, vm.state.value.photos.size)
        assertTrue(vm.state.value.canAddPhoto)
    }

    @Test
    fun photosSurviveGoingBackFromTheFormButCancelDiscardsThem() = runTest(dispatcher) {
        store.add("file:///1.jpg")
        // Volver desde el formulario recrea la pantalla: las fotos siguen.
        val vm = FoundCaptureViewModel(store)
        advanceUntilIdle()
        assertEquals(listOf("file:///1.jpg"), vm.state.value.photos)

        val effect = async { vm.effects.first() }
        vm.handleEvent(FoundCaptureEvent.OnCancel)
        advanceUntilIdle()
        assertEquals(FoundCaptureEffect.NavigateBack, effect.await())
        assertTrue(store.photos.value.isEmpty())
    }

    @Test
    fun permissionResultsShowAndHideTheDialog() = runTest(dispatcher) {
        val vm = FoundCaptureViewModel(store)
        vm.handleEvent(FoundCaptureEvent.OnPermissionResult(CameraPermissionResult.PERMANENTLY_DENIED))
        assertEquals(CameraPermissionResult.PERMANENTLY_DENIED, vm.state.value.permission)
        vm.handleEvent(FoundCaptureEvent.OnDismissPermission)
        assertNull(vm.state.value.permission)
    }
}
