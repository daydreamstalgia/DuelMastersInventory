package ro.daydreamstalgia.duelmastersinventory.shared.utils.image

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.DMatch
import org.opencv.core.KeyPoint
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.features.BFMatcher
import org.opencv.features.ORB
import org.opencv.geometry.Geometry
import org.opencv.imgproc.Imgproc

/**
 * Local-feature (ORB) card matching. Replaces the whole-image perceptual
 * hash this app shipped with first (see decisions/0005) - a dHash compares
 * one global luminance pattern, so a glare patch or a blurred corner can
 * wreck the *entire* hash. ORB keypoints are local: a glint over one part of
 * the art only kills the features under it, leaving the rest of the card to
 * carry the match. Originally ported directly from `kb/inbox/baladvent/card_match.py`
 * (CLAHE + ORB(500) + a flat NORM_HAMMING distance<50 cutoff); the matching
 * stage was reworked per decisions/0009's update after that flat cutoff
 * proved too weak to separate a genuine match from noise on real handheld
 * captures (confidence stuck around 15-20% even for the correct card - see
 * "Garkago Dragon still not identified"). It's now a ratio test (Lowe,
 * distance(best) < 0.75 * distance(second-best) via `knnMatch(k=2)`) to pick
 * candidate correspondences, followed by RANSAC homography fitting - only
 * correspondences consistent with *one* coherent perspective transform
 * count as "good", which is what a real photograph of the same flat card
 * produces and random cross-card keypoint coincidences don't.
 */
object OrbMatcher {
    private const val MAX_FEATURES = 500

    /** Lowe's ratio test cutoff: a match only counts if its nearest neighbor is decisively closer than its second-nearest, not just closest-of-a-crowd. */
    private const val RATIO_THRESHOLD = 0.75

    /** `findHomography`'s RANSAC reprojection tolerance, in pixels on the (CLAHE-normalized, not full-res) matching frame. */
    private const val RANSAC_REPROJ_THRESHOLD = 5.0

    /** `findHomography` needs at least 4 point correspondences to fit a perspective transform at all. Below this, [MatchResult.inlierCount] is reported as 0 rather than attempting a meaningless fit. */
    private const val MIN_HOMOGRAPHY_POINTS = 4

    private val orb: ORB by lazy { ORB.create(MAX_FEATURES) }

    /** crossCheck=false: crossCheck and knnMatch(k=2) are mutually exclusive in OpenCV, and the ratio test below is what does crossCheck's job (rejecting ambiguous matches) instead. */
    private val matcher: BFMatcher by lazy { BFMatcher.create(Core.NORM_HAMMING, false) }

    /** [keypoints] are the same points [descriptors]' rows describe, in the same order - needed for RANSAC homography fitting, not just distance matching. */
    @Suppress("ArrayInDataClass")
    class DescriptorSet(val descriptors: Mat, val keypoints: Array<Point>) {
        val keypointCount: Int get() = keypoints.size
        fun release() = descriptors.release()
    }

    /** @param goodMatchCount ratio-test survivors - the accept/reject gate ([ro.daydreamstalgia.duelmastersinventory.shared.data.cards.repository]'s `MIN_MATCH_COUNT`). @param inlierCount of those, how many fit one consistent RANSAC homography - the confidence numerator. */
    data class MatchResult(val goodMatchCount: Int, val inlierCount: Int)

    /**
     * Grayscale + denoise + CLAHE (clipLimit 3.0, 8x8 tiles) before ORB. The
     * bundled reference art is a clean flatbed-quality scan; a handheld phone
     * capture carries sensor noise and JPEG blocking the reference never has,
     * which ORB happily turns into spurious keypoints that can't possibly
     * match the reference descriptors - diluting the good-match ratio even
     * when the real card art lines up fine. A light 3x3 Gaussian blur ahead of
     * CLAHE knocks that noise down before contrast enhancement amplifies it.
     * clipLimit is raised from card_match.py's original 2.0 to 3.0 - phone
     * captures of foil/holo cards see uneven glare CLAHE needs to work harder
     * to normalize than the fixed-lightbox rig card_match.py was tuned
     * against. See decisions/0009 update.
     */
    private fun normalizedGray(bitmap: Bitmap): Mat {
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        rgba.release()

        val denoised = Mat()
        Imgproc.GaussianBlur(gray, denoised, Size(3.0, 3.0), 0.0)
        gray.release()

        val equalized = Mat()
        Imgproc.createCLAHE(3.0, Size(8.0, 8.0)).apply(denoised, equalized)
        denoised.release()
        return equalized
    }

    /** Grayscale + keypoints + descriptors for [bitmap]. Caller owns and must release all three. */
    private class Detection(val gray: Mat, val keypoints: MatOfKeyPoint, val descriptors: Mat) {
        fun release() {
            gray.release()
            keypoints.release()
            descriptors.release()
        }
    }

    private fun detect(bitmap: Bitmap): Detection {
        val gray = normalizedGray(bitmap)
        val keypoints = MatOfKeyPoint()
        val descriptors = Mat()
        orb.detectAndCompute(gray, Mat(), keypoints, descriptors)
        return Detection(gray, keypoints, descriptors)
    }

    fun compute(bitmap: Bitmap): DescriptorSet {
        val d = detect(bitmap)
        val points = d.keypoints.toArray().map { it.pt }.toTypedArray()
        d.gray.release()
        d.keypoints.release()
        return DescriptorSet(d.descriptors, points)
    }

    /** Ratio-test survivors between two descriptor sets - see [MatchResult.goodMatchCount]. */
    private fun ratioMatch(queryDescriptors: Mat, referenceDescriptors: Mat): List<DMatch> {
        if (queryDescriptors.empty() || referenceDescriptors.empty()) return emptyList()
        val knnMatches = ArrayList<MatOfDMatch>()
        matcher.knnMatch(queryDescriptors, referenceDescriptors, knnMatches, 2)
        val good = ArrayList<DMatch>(knnMatches.size)
        for (pair in knnMatches) {
            val candidates = pair.toArray()
            if (candidates.size == 2 && candidates[0].distance < RATIO_THRESHOLD * candidates[1].distance) {
                good.add(candidates[0])
            }
            pair.release()
        }
        return good
    }

    /** Which of [matches] fit one consistent RANSAC homography between [queryPoints] and [referencePoints] - null if there weren't even enough matches to attempt a fit. Index-aligned with [matches]. */
    private fun homographyInlierMask(matches: List<DMatch>, queryPoints: Array<Point>, referencePoints: Array<Point>): BooleanArray? {
        if (matches.size < MIN_HOMOGRAPHY_POINTS) return null
        val src = MatOfPoint2f(*matches.map { queryPoints[it.queryIdx] }.toTypedArray())
        val dst = MatOfPoint2f(*matches.map { referencePoints[it.trainIdx] }.toTypedArray())
        val mask = Mat()
        Geometry.findHomography(src, dst, Geometry.RANSAC, RANSAC_REPROJ_THRESHOLD, mask)
        src.release()
        dst.release()
        if (mask.empty()) {
            mask.release()
            return null
        }
        val maskBytes = ByteArray(mask.rows())
        mask.get(0, 0, maskBytes)
        mask.release()
        return BooleanArray(maskBytes.size) { maskBytes[it].toInt() != 0 }
    }

    /** Matches a query descriptor/keypoint set against a reference one. See [MatchResult]. */
    fun match(queryDescriptors: Mat, queryPoints: Array<Point>, referenceDescriptors: Mat, referencePoints: Array<Point>): MatchResult {
        val good = ratioMatch(queryDescriptors, referenceDescriptors)
        val inliers = homographyInlierMask(good, queryPoints, referencePoints)?.count { it } ?: 0
        return MatchResult(good.size, inliers)
    }

    /**
     * Renders the RANSAC-inlier match set as a side-by-side visualization
     * (query left, reference art right, lines connecting corresponding
     * keypoints) - only the geometrically-consistent matches, not every
     * ratio-test survivor, so the picture matches what [match] actually
     * counted as confidence rather than including ambiguous leftovers.
     * Drawn on the original color bitmaps, not the CLAHE-normalized
     * grayscale Mats fed to ORB - CLAHE grayscale is what the matcher
     * actually compares, but it reads far worse to a user judging "did this
     * find the right card" than the real photo does. Keypoint coordinates
     * are identical either way (grayscale conversion doesn't move pixels),
     * so this only changes what's displayed, not what was matched. Both
     * panels are scaled to a common height with no letterboxing - a query
     * capture and a much taller bundled reference scan naturally differ in
     * resolution, and naively pairing them at native size pads the shorter
     * one with a black region that reads as a meaningless third area in the
     * result. Returns null if either image yielded no keypoints.
     */
    fun drawGoodMatches(queryBitmap: Bitmap, referenceBitmap: Bitmap): Bitmap? {
        val query = detect(queryBitmap)
        val reference = detect(referenceBitmap)
        try {
            if (query.descriptors.empty() || reference.descriptors.empty()) return null

            val queryPoints = query.keypoints.toArray().map { it.pt }.toTypedArray()
            val referencePoints = reference.keypoints.toArray().map { it.pt }.toTypedArray()
            val good = ratioMatch(query.descriptors, reference.descriptors)
            val inlierMask = homographyInlierMask(good, queryPoints, referencePoints)
            val toDraw = if (inlierMask != null) good.filterIndexed { i, _ -> inlierMask[i] } else good

            return renderMatchesOnColor(
                queryBitmap, query.keypoints.toArray(),
                referenceBitmap, reference.keypoints.toArray(),
                toDraw,
            )
        } finally {
            query.release()
            reference.release()
        }
    }

    private fun renderMatchesOnColor(
        queryBitmap: Bitmap,
        queryKeypoints: Array<KeyPoint>,
        referenceBitmap: Bitmap,
        referenceKeypoints: Array<KeyPoint>,
        goodMatches: List<DMatch>,
        targetHeight: Int = 640,
    ): Bitmap {
        val queryMat = Mat(); Utils.bitmapToMat(queryBitmap, queryMat)
        val referenceMat = Mat(); Utils.bitmapToMat(referenceBitmap, referenceMat)

        val queryScale = targetHeight.toDouble() / queryMat.rows()
        val referenceScale = targetHeight.toDouble() / referenceMat.rows()
        val queryResized = Mat()
        Imgproc.resize(queryMat, queryResized, Size(queryMat.cols() * queryScale, targetHeight.toDouble()))
        val referenceResized = Mat()
        Imgproc.resize(referenceMat, referenceResized, Size(referenceMat.cols() * referenceScale, targetHeight.toDouble()))
        queryMat.release(); referenceMat.release()

        val canvas = Mat(targetHeight, queryResized.cols() + referenceResized.cols(), queryResized.type())
        queryResized.copyTo(canvas.submat(Rect(0, 0, queryResized.cols(), targetHeight)))
        referenceResized.copyTo(canvas.submat(Rect(queryResized.cols(), 0, referenceResized.cols(), targetHeight)))
        val xOffset = queryResized.cols()
        queryResized.release(); referenceResized.release()

        val matchColor = Scalar(0.0, 200.0, 60.0, 255.0) // RGBA - Utils.bitmapToMat/matToBitmap use RGBA order
        for (m in goodMatches) {
            val q = queryKeypoints[m.queryIdx].pt
            val r = referenceKeypoints[m.trainIdx].pt
            val p1 = Point(q.x * queryScale, q.y * queryScale)
            val p2 = Point(r.x * referenceScale + xOffset, r.y * referenceScale)
            Imgproc.line(canvas, p1, p2, matchColor, 1, Imgproc.LINE_AA)
            Imgproc.circle(canvas, p1, 3, matchColor, 1, Imgproc.LINE_AA)
            Imgproc.circle(canvas, p2, 3, matchColor, 1, Imgproc.LINE_AA)
        }

        val bmp = Bitmap.createBitmap(canvas.cols(), canvas.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(canvas, bmp)
        canvas.release()
        return bmp
    }

    /** Flattens a CV_8UC1 descriptor Mat (one 32-byte row per keypoint) to a Room-storable BLOB. */
    fun serialize(descriptors: Mat): ByteArray {
        if (descriptors.empty()) return ByteArray(0)
        val bytes = ByteArray(descriptors.rows() * descriptors.cols())
        descriptors.get(0, 0, bytes)
        return bytes
    }

    /** Inverse of [serialize] - [keypointCount] is stored alongside the BLOB since a flat byte array doesn't carry its own row count. */
    fun deserialize(bytes: ByteArray, keypointCount: Int): Mat {
        if (bytes.isEmpty() || keypointCount == 0) return Mat()
        val descriptorSize = bytes.size / keypointCount
        val mat = Mat(keypointCount, descriptorSize, CvType.CV_8UC1)
        mat.put(0, 0, bytes)
        return mat
    }

    /** Flattens keypoint (x, y) coordinates to a Room-storable BLOB - 8 bytes/point (two little-endian float32s), same row order as [serialize]'s descriptors so index `i` in both refers to the same keypoint. */
    fun serializeKeypoints(keypoints: Array<Point>): ByteArray {
        val buffer = ByteBuffer.allocate(keypoints.size * 8).order(ByteOrder.LITTLE_ENDIAN)
        for (p in keypoints) {
            buffer.putFloat(p.x.toFloat())
            buffer.putFloat(p.y.toFloat())
        }
        return buffer.array()
    }

    /** Inverse of [serializeKeypoints]. */
    fun deserializeKeypoints(bytes: ByteArray, keypointCount: Int): Array<Point> {
        if (bytes.isEmpty() || keypointCount == 0) return emptyArray()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return Array(keypointCount) { Point(buffer.float.toDouble(), buffer.float.toDouble()) }
    }
}
