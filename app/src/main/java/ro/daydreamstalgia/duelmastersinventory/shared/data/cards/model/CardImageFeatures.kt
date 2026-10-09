package ro.daydreamstalgia.duelmastersinventory.shared.data.cards.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Precomputed ORB descriptors for a print's art, keyed by CardPrint id
 * (source: `card_images/{cardPrintId}.jpg` - the full-resolution art, not
 * `card_images_low`; ORB needs real detail to find keypoints). Backs offline
 * camera scan matching via local-feature matching instead of a whole-image
 * hash - see decisions/0009-orb-feature-matching.md. Built lazily once on
 * first app open (see DatabaseModule) and cached here from then on.
 *
 * [descriptors] is the raw OpenCV descriptor Mat data (CV_8UC1, one 32-byte
 * row per keypoint, row-major) via [ro.daydreamstalgia.duelmastersinventory.shared.utils.image.OrbMatcher.serialize].
 * [keypoints] is the (x, y) location of each of those same rows, via
 * [ro.daydreamstalgia.duelmastersinventory.shared.utils.image.OrbMatcher.serializeKeypoints]
 * - needed for RANSAC homography verification, not just descriptor distance
 * matching (see decisions/0009 update). [keypointCount] is stored alongside
 * since neither flat BLOB carries its own row count.
 */
@Suppress("ArrayInDataClass")
@Entity(tableName = "CardImageFeatures")
data class CardImageFeatures(
    @PrimaryKey
    val cardPrintId: Int,
    val descriptors: ByteArray,
    val keypoints: ByteArray,
    val keypointCount: Int,
)
