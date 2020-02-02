package fr.spacefox.coree.scripts

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.io.FileReader

fun main() {
    val reader = FileReader("/home/spacefox/dev/hugo-sites/coree.spacefox.fr/static/ref.csv")
    CSVFormat.RFC4180.withFirstRecordAsHeader()
            .parse(reader)
            .forEach { it.toContentFile() }
}

private fun CSVRecord.toContentFile() {
    File("/home/spacefox/dev/hugo-sites/coree.spacefox.fr/content/${this.get("fileName").split(".")[0]}.md")
            .writeText("""---
title: "${this.get("title")}"
date: "${this.get("date")}"
img: ${this.get("fileName")}
speed: ${this.get("speed")}
iso: ${this.get("iso")}
aperture: ${this.get("aperture")}
focal: ${this.get("focalLength")}
picInstant: ${this.get("date")}
widthFull: ${this.get("widthFull")}
heightFull: ${this.get("heightFull")}
fileSizeFull: ${this.get("fileSizeFull")}
width2k: ${this.get("width2k")}
height2k: ${this.get("height2k")}
fileSize2k: ${this.get("fileSize2k")}
width3k: ${this.get("width3k")}
height3k: ${this.get("height3k")}
fileSize3k: ${this.get("fileSize3k")}
width4k: ${this.get("width4k")}
height4k: ${this.get("height4k")}
fileSize4k: ${this.get("fileSize4k")}
widthRaw: ${this.get("widthRaw")}
heightRaw: ${this.get("heightRaw")}
fileSizeRaw: ${this.get("fileSizeRaw")}
ratio:
- ${this.get("normalizedRatio")}
lieux:
- ${this.get("place")}
picdates:
- ${this.get("date").split("T")[0]}
themes:
- ${this.get("themes").split(";").joinToString("\n- ")}
---

${this.get("details")}""")
}
