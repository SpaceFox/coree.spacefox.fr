package fr.spacefox.coree.scripts

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.file.FileSystemDirectory
import com.drew.metadata.jpeg.JpegDirectory
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import java.io.FileWriter
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class ImgSizes(val width: Int, val height: Int, val fileSize: String)

data class Entry(
        val fileName: String,
        val normalizedRatio: String,
        val date: OffsetDateTime,
        val speed: String,
        val iso: Int,
        val aperture: Float,
        val focalLength: Float,
        val sizeFull: ImgSizes,
        val size2k: ImgSizes?,
        val size3k: ImgSizes?,
        val size4k: ImgSizes?,
        val sizeRaw: ImgSizes?
)

val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
fun main() {
    val out = FileWriter("/home/spacefox/dev/hugo-sites/coree.spacefox.fr/static/ref.csv")
    val printer = CSVPrinter(out, CSVFormat.DEFAULT.withHeader(
            "fileName",
            "normalizedRatio",
            "date",
            "speed",
            "iso",
            "aperture",
            "focalLength",
            "widthFull",
            "heightFull",
            "fileSizeFull",
            "width2k",
            "height2k",
            "fileSize2k",
            "width3k",
            "height3k",
            "fileSize3k",
            "width4k",
            "height4k",
            "fileSize4k",
            "widthRaw",
            "heightRaw",
            "fileSizeRaw",
            "place",
            "themes",
            "title",
            "details"
    ))
    printer.use {
        File("/home/spacefox/dev/hugo-sites/static.coree.spacefox.fr/full")
                .walkTopDown()
                .filter { it.isFile }
                .toSortedSet()
                .map { toEntry(it) }
                .forEach {
                    printer.printRecord(
                            it.fileName,
                            it.normalizedRatio,
                            it.date.format(formatter),
                            it.speed,
                            it.iso,
                            it.aperture,
                            it.focalLength,
                            it.sizeFull.width,
                            it.sizeFull.height,
                            it.sizeFull.fileSize,
                            it.size2k?.width,
                            it.size2k?.height,
                            it.size2k?.fileSize,
                            it.size3k?.width,
                            it.size3k?.height,
                            it.size3k?.fileSize,
                            it.size4k?.width,
                            it.size4k?.height,
                            it.size4k?.fileSize,
                            it.sizeRaw?.width,
                            it.sizeRaw?.height,
                            it.sizeRaw?.fileSize,
                            toPlace(it),
                            "Test1;Test2;Autre Test",
                            "Title for ${it.fileName}",
                            null
                    )
                }
    }

}

fun toPlace(it: Entry): String? {
    val fileName = it.fileName
    if (!fileName.startsWith("D5600_")) {
        return null
    }
    val photoId = fileName.split("_")[1].toInt()
    return when {
        // isolated folders
        photoId in 2072..2108 -> "chuncheon"
        photoId in 2194..2464 -> "seoraksan"
        photoId in 2110..2474 -> "sokcho" // after seoraksan
        photoId in 2480..2711 -> "jeonju"
        photoId in 2716..2809 -> "wando"
        // Jeju
        photoId in 3110..3286 -> "hallasan"
        photoId in 3297..3459 -> "lavatunnel"
        photoId in 2832..3102 -> "seogwipo" // after hallasan and lavatunnel
        // Seoul
        photoId in 1776..1832 -> "nationalmuseum"
        photoId in 1844..1878 -> "deoksungung"
        photoId in 1886..1963 -> "gyeongbokgung"
        photoId in 1988..2000 -> "jongmyo"
        photoId in 2009..2054 -> "changdeokgung"
        photoId in 3464..3488 -> "warmemorial"
        photoId in 3499..3551 -> "hongik"
        photoId in 1985..3535 -> "marches"  // after all seoul but general
        photoId in 1756..3520 -> "seoul" // after everything else
        else -> null
    }
}

fun toEntry(file: File): Entry {
    val metadata = file.extractMetadata()

    val width = metadata.width()
    val height = metadata.height()
    val path = file.toPath()
    return Entry(
            file.nameWithoutExtension,
            normalizeRatio(width.toFloat() / height),
            metadata.date(file.name.startsWith("D5600_")),
            normalizeSpeed(metadata.speed()),
            metadata.iso(),
            metadata.aperture(),
            metadata.focalLength(),
            sizeFull = ImgSizes(width, height, metadata.fileSize().toSizeUnit()),
            size2k = toImgSizes(path, "2k"),
            size3k = toImgSizes(path, "3k"),
            size4k = toImgSizes(path, "4k"),
            sizeRaw = toImgSizes(path, "raw", "NEF")
    )
}

const val VAL_1K = 1 shl 10
const val VAL_1M = 1 shl 20
private fun Long.toSizeUnit(): String {
    return when {
        this < VAL_1K -> "$this octets"
        this < VAL_1M -> "${"%.2f".format(this.toFloat() / VAL_1K)} kio"
        else -> "${"%.2f".format(this.toFloat() / VAL_1M)} Mio"
    }
}

fun toImgSizes(path: Path, folder: String, extension: String = "jpg"): ImgSizes? {
    val fileName = path.fileName.toFile().nameWithoutExtension + "." + extension
    val root = path.parent.parent
    val fileToCheck = Path.of(root.toString(), folder, fileName).toFile()
    if (!fileToCheck.exists()) {
        return null
    }
    val metadata = fileToCheck.extractMetadata()
    return ImgSizes(metadata.width(), metadata.height(), metadata.fileSize().toSizeUnit())
}

fun normalizeRatio(ratio: Float): String {
    return when {
        0.66 < ratio && ratio < 0.67 -> "vertical"
        0.99 < ratio && ratio < 1.01 -> "carre"
        1.49 < ratio && ratio < 1.51 -> "standard"
        1.77 < ratio && ratio < 1.78 -> "ecran"
        ratio > 1.8 -> "panoramique"
        else -> "autre"
    }
}

fun normalizeSpeed(speed: Float): String {
    return if (speed >= 1) "$speed s" else "1/${(1 / speed).roundToInt()} s"
}

private fun File.extractMetadata(): Metadata {
    return ImageMetadataReader.readMetadata(this)
}

private fun Metadata.width(): Int {
    return getFirstDirectoryOfType(JpegDirectory::class.java)
            ?.imageWidth
            ?: return tagFromRaw(ExifSubIFDDirectory.TAG_IMAGE_WIDTH)
}

private fun Metadata.height(): Int {
    return getFirstDirectoryOfType(JpegDirectory::class.java)
            ?.imageHeight
            ?: return tagFromRaw(ExifSubIFDDirectory.TAG_IMAGE_HEIGHT)
}

private fun Metadata.tagFromRaw(tagImageWidth: Int): Int {
    val rawDirectories = this.getDirectoriesOfType(ExifSubIFDDirectory::class.java)
    val rawDirectory = rawDirectories.first { it.containsTag(tagImageWidth) }
    return rawDirectory.getInt(tagImageWidth)
}

val timeZone = ZoneId.of("UTC+8")
private fun Metadata.date(timeZoneOK: Boolean): OffsetDateTime {
    var instant = this.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
            .getDate(ExifIFD0Directory.TAG_DATETIME_ORIGINAL)
            .toInstant()
    if (!timeZoneOK) {
        instant = instant.minus(8, ChronoUnit.HOURS)
    }
    val toOffsetDateTime = instant.atZone(timeZone).toOffsetDateTime()
    println("$instant → $toOffsetDateTime")
    return toOffsetDateTime
}

private fun Metadata.speed(): Float {
    return this.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java).getFloat(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)
}

private fun Metadata.iso(): Int {
    return this.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java).getInt(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)
}

private fun Metadata.aperture(): Float {
    return this.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java).getFloat(ExifSubIFDDirectory.TAG_FNUMBER)
}

private fun Metadata.focalLength(): Float {
    return this.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java).getFloat(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)
}

private fun Metadata.fileSize(): Long {
    return this.getFirstDirectoryOfType(FileSystemDirectory::class.java).getLong(FileSystemDirectory.TAG_FILE_SIZE)
}