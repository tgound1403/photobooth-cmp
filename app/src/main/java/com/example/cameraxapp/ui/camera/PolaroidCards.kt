package com.example.cameraxapp.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.cameraxapp.core.moments.Moment
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ImageNode
import io.github.sceneview.node.Node
import com.google.android.filament.Engine
import com.google.android.filament.utils.TextureType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Most photos shown at once. They share one ring, spaced evenly. */
const val MAX_CARDS = 10

/** The single ring: tipped toward the viewer so a card sweeps from low in front of the body to high behind it. */
private const val RING_ELEVATION_DEG = 20f

/** One lap of the ring. */
private const val RING_PERIOD_MS = 40_000

/** Each card's own fixed roll alternates by this much, so the row does not look ruled. */
private const val CARD_TILT_DEG = 6f

/** Card size relative to the ring radius. */
private const val CARD_WIDTH_RATIO = 0.46f
private const val CARD_HEIGHT_RATIO = 0.56f

/** Gap between a card's front and back planes, so they never fight for depth. */
private const val FACE_GAP = 0.004f

private const val BITMAP_W = 416
private const val BITMAP_H = 504

/**
 * Photo cards that circle the body inside the 3D scene on one ring, so the body hides cards behind it
 * by depth rather than by a 2D cut-out. Each card is a front plane (photo and date) and a back plane
 * (plain paper) turned 180 degrees. Up to [MAX_CARDS] cards exist; only as many as there are photos
 * are shown.
 *
 * [scale] is the ring radius in scene units, [center] is where the body sits.
 */
class PolaroidCards(
    engine: Engine,
    private val materialLoader: MaterialLoader,
    private val scale: Float,
    center: Position,
) {
    /** The ring the cards hang on. */
    private val ring: Node = Node(engine).apply { position = center }

    private val cards: List<Node> =
        List(MAX_CARDS) { Node(engine).also { ring.addChildNode(it) } }

    private var shown = 0

    private val fronts: MutableList<ImageNode> =
        MutableList(MAX_CARDS) { index -> newFront(polaroidBitmap(null, "")).also { cards[index].addChildNode(it) } }

    private fun newFront(bitmap: Bitmap): ImageNode =
        ImageNode(
            materialLoader = materialLoader,
            bitmap = bitmap,
            size = Position(CARD_WIDTH_RATIO * scale, CARD_HEIGHT_RATIO * scale, 0f),
            type = TextureType.COLOR,
        )

    init {
        val w = CARD_WIDTH_RATIO * scale
        val h = CARD_HEIGHT_RATIO * scale
        val gap = FACE_GAP * scale
        cards.forEach { card ->
            ImageNode(
                materialLoader = materialLoader,
                bitmap = paperBitmap(),
                size = Position(w, h, 0f),
                type = TextureType.COLOR,
            ).apply {
                position = Position(0f, 0f, -gap)
                rotation = Rotation(y = 180f)
            }.also { card.addChildNode(it) }
            card.isVisible = false
        }
    }

    /** The nodes to add to the scene; the cards hang off the ring. */
    val roots: List<Node> get() = listOf(ring)

    /** Replaces the placeholder cards with photos. Call off the render loop; reading photos takes time. */
    suspend fun load(context: Context, moments: List<Moment>) {
        shown = moments.size.coerceAtMost(MAX_CARDS)
        cards.forEachIndexed { index, card -> card.isVisible = index < shown }
        for (index in 0 until shown) {
            val moment = moments[index]
            val request =
                ImageRequest.Builder(context)
                    .data(moment.uri)
                    .size(BITMAP_W)
                    .allowHardware(false)
                    .build()
            val photo = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable)?.let { drawable ->
                (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            }
            val caption = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(moment.takenAt))
            // Setting ImageNode.bitmap on a live node did not refresh its texture, so the front plane is replaced.
            val fresh = newFront(polaroidBitmap(photo, caption))
            cards[index].addChildNode(fresh)
            // Destroying a node the renderer may still draw aborts Filament, so the placeholder is only hidden.
            fronts[index].isVisible = false
            fronts[index] = fresh
        }
    }

    /** Moves every card to its place [timeMs] into the animation, plus the user's [spin]. */
    fun update(timeMs: Long, spin: PickerSpin) {
        ring.rotation = Rotation(x = RING_ELEVATION_DEG + spin.tiltOffset)
        val count = shown
        if (count == 0) return
        val lap = (timeMs % RING_PERIOD_MS).toFloat() / RING_PERIOD_MS + spin.lapOffset
        val r = scale
        for (index in 0 until count) {
            val theta = 2f * PI.toFloat() * (lap + index.toFloat() / count)
            cards[index].position = Position(r * cos(theta), 0f, r * sin(theta))
            // The card's front faces outward from the ring centre: the viewer at the near side of the
            // ring, edge-on at the sides, and its back at the far side.
            val yaw = (PI.toFloat() / 2f - theta) * 180f / PI.toFloat()
            cards[index].rotation = Rotation(y = yaw, z = if (index % 2 == 0) CARD_TILT_DEG else -CARD_TILT_DEG)
        }
    }
}

private fun paperBitmap(): Bitmap =
    Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(0xED, 0xED, 0xED)) }

/** White polaroid with [photo] centre-cropped in the upper part and [caption] under it. */
private fun polaroidBitmap(photo: Bitmap?, caption: String): Bitmap {
    val bitmap = Bitmap.createBitmap(BITMAP_W, BITMAP_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    val inset = 24f
    val area = RectF(inset, inset, BITMAP_W - inset, BITMAP_H - 112f)
    if (photo == null) {
        canvas.drawRect(area, Paint().apply { color = Color.rgb(0xBD, 0xBD, 0xBD) })
    } else {
        // Centre crop: scale the photo to cover the area and cut the overflow.
        val s = maxOf(area.width() / photo.width, area.height() / photo.height)
        val w = photo.width * s
        val h = photo.height * s
        val dst = RectF(area.centerX() - w / 2f, area.centerY() - h / 2f, area.centerX() + w / 2f, area.centerY() + h / 2f)
        canvas.save()
        canvas.clipRect(area)
        canvas.drawBitmap(photo, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }
    if (caption.isNotEmpty()) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(0x55, 0x55, 0x55)
                textSize = 40f
                typeface = Typeface.MONOSPACE
            }
        canvas.drawText(caption, inset, BITMAP_H - 44f, paint)
    }
    return bitmap
}
