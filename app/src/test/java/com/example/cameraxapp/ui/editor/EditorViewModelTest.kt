package com.example.cameraxapp.ui.editor

import android.graphics.Bitmap
import com.example.cameraxapp.core.render.CropAspect
import com.example.cameraxapp.core.render.CropParams
import com.example.cameraxapp.core.render.EditParams
import com.example.cameraxapp.core.render.FilterInfo
import com.example.cameraxapp.core.render.EditRenderer
import com.example.cameraxapp.core.render.EditSnapshot
import com.example.cameraxapp.core.render.RecipeCode
import com.example.cameraxapp.data.FakeRecipeDao
import com.example.cameraxapp.data.dao.RecipeDao
import com.example.cameraxapp.data.repository.RecipeRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorViewModelTest {

    /** Never called: these tests never load a bitmap, so the renderer is not reached. */
    private class UnusedRenderer : EditRenderer {
        override suspend fun render(
            source: Bitmap,
            snapshot: EditSnapshot,
            background: Bitmap?,
            density: Float,
            maxSize: Int?,
        ): Bitmap = throw IllegalStateException("renderer should not run in these tests")

        override fun close() = Unit
    }

    private object NoImageIo : EditorImageIo {
        override fun decode(uri: String, maxSide: Int): Bitmap? = null
        override fun saveJpeg(bitmap: Bitmap): String? = null
    }

    private fun newViewModel(recipes: RecipeDao = FakeRecipeDao()) =
        EditorViewModel(
            io = NoImageIo,
            renderer = UnusedRenderer(),
            recipeRepository = RecipeRepository(recipes, File(System.getProperty("java.io.tmpdir"), "editor-test")),
            uiDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun sliderDragIsOneUndoStepOnRelease() {
        val vm = newViewModel()
        // Five drag frames, no release yet: nothing recorded.
        for (i in 1..5) vm.setParams(EditParams(exposure = i / 10f))
        assertFalse(vm.state.value.canUndo)
        assertEquals(0.5f, vm.state.value.snapshot.params.exposure, 0f)

        vm.commitParams()
        assertTrue(vm.state.value.canUndo)

        vm.undo()
        assertEquals(EditParams(), vm.state.value.snapshot.params)
        assertFalse(vm.state.value.canUndo)
    }

    @Test
    fun undoThenRedoRestoresTheEdit() {
        val vm = newViewModel()
        vm.selectFilter("warm")
        vm.selectFilter("cool")

        vm.undo()
        assertEquals("warm", vm.state.value.snapshot.params.filterId)
        assertTrue(vm.state.value.canRedo)

        vm.redo()
        assertEquals("cool", vm.state.value.snapshot.params.filterId)
        assertFalse(vm.state.value.canRedo)
    }

    @Test
    fun historyIsCappedAtThirtySteps() {
        val vm = newViewModel()
        for (i in 1..35) vm.selectFilter("f$i")

        for (step in 1..30) {
            vm.undo()
        }
        assertFalse("31st undo must have nothing to restore", vm.state.value.canUndo)
        // 35 changes with a cap of 30 leaves the 5 earliest states unreachable: the oldest reachable is f5.
        assertEquals("f5", vm.state.value.snapshot.params.filterId)
    }

    @Test
    fun resetRestoresDefaultsAndCanBeUndone() {
        val vm = newViewModel()
        vm.setCrop(CropParams(aspect = CropAspect.SQUARE))
        vm.selectFilter("warm")

        vm.reset()
        assertEquals(EditSnapshot(), vm.state.value.snapshot)

        vm.undo()
        assertEquals("warm", vm.state.value.snapshot.params.filterId)
        assertEquals(CropAspect.SQUARE, vm.state.value.snapshot.params.crop.aspect)
    }

    @Test
    fun pressAndHoldDoesNotTouchHistory() {
        val vm = newViewModel()
        vm.selectFilter("warm")

        vm.setShowOriginal(true)
        assertTrue(vm.state.value.showOriginal)
        assertTrue(vm.state.value.canUndo)
        assertEquals("warm", vm.state.value.snapshot.params.filterId)

        vm.setShowOriginal(false)
        assertFalse(vm.state.value.showOriginal)
    }

    @Test
    fun committingWithoutChangesAddsNoStep() {
        val vm = newViewModel()
        vm.commitParams()
        assertFalse(vm.state.value.canUndo)
        assertNull(vm.state.value.preview)
    }

    @Test
    fun applyingRecipeIsOneUndoStep() {
        val vm = newViewModel()
        vm.applyRecipe(EditParams(filterId = "kodak", exposure = 0.3f))
        assertEquals("kodak", vm.state.value.snapshot.params.filterId)
        assertTrue(vm.state.value.canUndo)

        vm.undo()
        assertEquals(EditParams(), vm.state.value.snapshot.params)
        vm.redo()
        assertEquals(0.3f, vm.state.value.snapshot.params.exposure, 0f)
    }

    @Test
    fun badImportReportsTheErrorAndSavesNothing() {
        val vm = newViewModel()
        val notices = mutableListOf<String>()
        val collector = CoroutineScope(Dispatchers.Unconfined).launch { vm.notices.collect { notices += it } }

        vm.importRecipeCode("not a code")

        assertEquals(listOf("Not a recipe code"), notices)
        assertTrue(vm.recipes.value.isEmpty())
        collector.cancel()
    }

    @Test
    fun goodImportSavesTheRecipeWithoutApplyingIt() {
        val vm = newViewModel()
        vm.importRecipeCode(RecipeCode.encode(EditParams(filterId = "kodak", grain = 0.2f)))

        assertEquals(1, vm.recipes.value.size)
        assertEquals("kodak", vm.recipes.value.single().params.filterId)
        assertEquals(EditParams(), vm.state.value.snapshot.params)
    }

    private val premiumFilters = listOf(
        FilterInfo(id = "kodak", name = "Kodak", group = "Classic", premium = false),
        FilterInfo(id = "rose_skin", name = "Rose Skin", group = "Portrait", premium = true),
    )

    private fun gatedViewModel(premium: Boolean) =
        EditorViewModel(
            io = NoImageIo,
            renderer = UnusedRenderer(),
            filters = premiumFilters,
            recipeRepository = RecipeRepository(FakeRecipeDao(), File(System.getProperty("java.io.tmpdir"), "editor-test")),
            isPremium = MutableStateFlow(premium),
            uiDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun previewingPremiumFilter_isAllowedForFreeUser() {
        val vm = gatedViewModel(premium = false)
        vm.selectFilter("rose_skin")
        assertEquals("rose_skin", vm.state.value.snapshot.params.filterId)
        assertFalse(vm.state.value.showPaywall)
    }

    @Test
    fun saveWithPremiumFilter_forFreeUser_opensPaywallAndDoesNotSave() {
        val vm = gatedViewModel(premium = false)
        vm.selectFilter("rose_skin")
        vm.save()
        assertTrue(vm.state.value.showPaywall)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun saveWithFreeFilter_neverOpensPaywall() {
        val vm = gatedViewModel(premium = false)
        vm.selectFilter("kodak")
        vm.save()
        assertFalse(vm.state.value.showPaywall)
    }

    @Test
    fun saveWithPremiumFilter_forPremiumUser_doesNotOpenPaywall() {
        val vm = gatedViewModel(premium = true)
        vm.selectFilter("rose_skin")
        vm.save()
        assertFalse(vm.state.value.showPaywall)
    }

    @Test
    fun saveRecipeWithPremiumFilter_forFreeUser_opensPaywall() {
        val vm = gatedViewModel(premium = false)
        vm.selectFilter("rose_skin")
        vm.saveRecipe()
        assertTrue(vm.state.value.showPaywall)
    }

    @Test
    fun dismissPaywall_closesIt() {
        val vm = gatedViewModel(premium = false)
        vm.selectFilter("rose_skin")
        vm.save()
        vm.dismissPaywall()
        assertFalse(vm.state.value.showPaywall)
    }
}
