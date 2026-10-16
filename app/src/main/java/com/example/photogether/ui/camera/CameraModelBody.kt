package com.example.photogether.ui.camera

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.photogether.core.moments.Moment
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes

private const val MODEL_SWAY_DEGREES = 25f

/**
 * Distance from the camera to the body. The farther the body, the flatter the perspective, so cards
 * on the near side of a ring do not grow much larger than those on the far side.
 */
private const val SCENE_DISTANCE = 10f

/** Ring radius in scene units: about 0.91 of the body's width. */
private const val CARD_RING_RADIUS = 3.0f

/** Largest side of the model in scene units. The camera is 3 units away, so this sets the body's size on screen. */
private const val MODEL_UNITS = 3.3f

/**
 * Colours the body material of the model with [argb]. The GLB has three materials: "body" (tinted),
 * "lens" (dark glass) and "chrome" (top details), which keep their own colours. glTF base colours are
 * linear, so the sRGB channels are converted first.
 */
private fun ModelNode.applyTint(argb: Int) {
    fun linear(shift: Int): Float = Math.pow(((argb shr shift) and 0xFF) / 255.0, 2.2).toFloat()
    val r = linear(16)
    val g = linear(8)
    val b = linear(0)
    materialInstances.flatten().forEach { material ->
        if (material.name == BODY_MATERIAL) {
            material.setParameter("baseColorFactor", r, g, b, 1f)
            material.setParameter("metallicFactor", TINT_METALLIC)
            material.setParameter("roughnessFactor", TINT_ROUGHNESS)
        }
    }
}

/** Name of the glTF material that takes the camera colour. Matches the material name in the GLB. */
private const val BODY_MATERIAL = "body"

/** Brass-like finish: mostly metal with a soft highlight. */
private const val TINT_METALLIC = 0.85f
private const val TINT_ROUGHNESS = 0.4f

/**
 * Camera body as a 3D model (glTF/GLB, rendered by SceneView/Filament). [modelAsset] is a path under
 * `assets/`. The body turns between -25 and +25 degrees around its vertical axis.
 *
 * Only the page centred on the picker should compose this: each composition owns a Filament engine,
 * and a crash in libgltfio was seen with one engine per pager page. The picker keeps a single
 * instance and changes [tint] as the user swipes between cameras.
 * The scene is opaque and sits under the Compose content (see the hole in the body below).
 */
@Composable
fun CameraModelBody(
    modelAsset: String,
    tint: Int,
    spin: PickerSpin,
    moments: List<Moment>,
    modifier: Modifier = Modifier,
) {
    key(modelAsset) {
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        // SceneView 2.1.1's own camera node throws a NullPointerException when it is destroyed
        // (CameraNode.destroy reads the camera after Node.destroy has already removed it), which
        // crashed the app on leaving the picker. This node is destroyed by hand and the error ignored.
        val materialLoader = rememberMaterialLoader(engine)
        val cameraNode = remember(engine) { CameraNode(engine).apply { far = 100f } }
        DisposableEffect(cameraNode) {
            onDispose { runCatching { cameraNode.destroy() } }
        }
        val modelInstance = remember(modelLoader) { modelLoader.createModelInstance(modelAsset) }
        val modelNode =
            remember(modelInstance) {
                ModelNode(
                    modelInstance = modelInstance,
                    autoAnimate = false,
                    scaleToUnits = MODEL_UNITS,
                    centerOrigin = Position(x = 0f, y = 0f, z = 0f),
                ).apply {
                    // The default camera looks down -Z from the origin, so the body sits 3 m ahead.
                    position = Position(z = -SCENE_DISTANCE)
                }
            }
        val cards =
            remember(engine, materialLoader) {
                PolaroidCards(engine, materialLoader, scale = CARD_RING_RADIUS, center = Position(z = -SCENE_DISTANCE))
            }
        val context = LocalContext.current
        LaunchedEffect(cards, moments) { cards.load(context, moments) }
        val childNodes = rememberNodes {
            add(modelNode)
            cards.roots.forEach { add(it) }
        }

        val transition = rememberInfiniteTransition(label = "camera_model_sway")
        val angle by transition.animateFloat(
            initialValue = -MODEL_SWAY_DEGREES,
            targetValue = MODEL_SWAY_DEGREES,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "camera_model_angle",
        )
        val sway = rememberUpdatedState(angle)
        LaunchedEffect(modelNode, tint) { modelNode.applyTint(tint) }

        // The scene is opaque so its surface sits under the Compose content: polaroids in front of the
        // body draw over it. A hole is cleared in the (offscreen) screen layer so the surface shows.
        Box(modifier = modifier.drawBehind { drawRect(Color.Transparent, blendMode = BlendMode.Clear) }) {
            Scene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                isOpaque = true,
                cameraNode = cameraNode,
                childNodes = childNodes,
                onFrame = { frameTimeNanos ->
                    // Set here, not in composition, so a drag turns the body on the very next frame.
                    modelNode.rotation = Rotation(x = spin.bodyPitch, y = sway.value + spin.bodyYaw)
                    cards.update(frameTimeNanos / 1_000_000L, spin)
                },
            )
        }
    }
}
