package com.relentnet.cuttracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** 24-unit stroke icons lifted from the prototype's inline SVGs. Tint them with Icon(tint = ...). */
private fun stroke(name: String, width: Float, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        for (p in paths) addPath(
            pathData = addPathNodes(p), fill = null, stroke = SolidColor(Color.Black),
            strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} $cy a$r $r 0 1 0 ${2 * r} 0 a$r $r 0 1 0 ${-2 * r} 0"

object CutIcons {
    val Today = stroke("today", 2f, "M5 4h14a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M3 9h18M8 2v4M16 2v4")
    val Weight = stroke("weight", 2f, "M3 17l5-6 4 4 5-8 4 5")
    val Lift = stroke("lift", 2f, "M2 12h3M19 12h3M6 8v8M18 8v8M9 6v12M15 6v12M9 12h6")
    val Route = stroke("route", 2f, "M4 21l4-7 4 3 4-8 4 4", circle(20f, 5f, 2f))
    val Gear = stroke(
        "gear", 2f, circle(12f, 12f, 3f),
        "M19.4 15a1.7 1.7 0 00.3 1.8l.1.1a2 2 0 11-2.8 2.8l-.1-.1a1.7 1.7 0 00-1.8-.3 1.7 1.7 0 00-1 1.5V21a2 2 0 11-4 0v-.1a1.7 1.7 0 00-1.1-1.5 1.7 1.7 0 00-1.8.3l-.1.1a2 2 0 11-2.8-2.8l.1-.1a1.7 1.7 0 00.3-1.8 1.7 1.7 0 00-1.5-1H3a2 2 0 110-4h.1a1.7 1.7 0 001.5-1.1 1.7 1.7 0 00-.3-1.8l-.1-.1a2 2 0 112.8-2.8l.1.1a1.7 1.7 0 001.8.3H9a1.7 1.7 0 001-1.5V3a2 2 0 114 0v.1a1.7 1.7 0 001 1.5 1.7 1.7 0 001.8-.3l.1-.1a2 2 0 112.8 2.8l-.1.1a1.7 1.7 0 00-.3 1.8V9a1.7 1.7 0 001.5 1H21a2 2 0 110 4h-.1a1.7 1.7 0 00-1.5 1z",
    )
    val Back = stroke("back", 2.4f, "M15 5l-7 7 7 7")
    val Forward = stroke("forward", 2.6f, "M9 5l7 7-7 7")
    val Check = stroke("check", 2.5f, "M4 12.5l5 5 10-12")
    val Link = stroke("link", 2.4f, "M10 13a5 5 0 007 0l3-3a5 5 0 00-7-7l-1 1", "M14 11a5 5 0 00-7 0l-3 3a5 5 0 007 7l1-1")
    val Climb = stroke("climb", 2.2f, "M4 20l6-14 4 7 2-3 4 10z")
    val Off = stroke("off", 2.2f, "M3 12h4l3-7 4 14 3-7h4")
    val Rest = stroke("rest", 2.2f, "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z")
    val Shield = stroke("shield", 2.6f, "M12 2l8 4v6c0 5-3.5 8.5-8 10-4.5-1.5-8-5-8-10V6z", "M8.5 12l2.5 2.5 4.5-5")
    val Camera = stroke("camera", 2f, "M4 8h3l2-3h6l2 3h3v11H4z", circle(12f, 13f, 3.5f))
    val Anchor = stroke("anchor", 2.4f, circle(12f, 5f, 3f), "M12 22V8M5 12H2a10 10 0 0020 0h-3")
    val Star = stroke("star", 2.4f, "M12 3l2.5 6 6.5.6-5 4.3 1.6 6.4L12 17l-5.6 3.3L8 13.9 3 9.6l6.5-.6z")
    val Layers = stroke("layers", 2.4f, "M12 2v20M4 7l8 5 8-5M4 17l8-5 8 5")
    val Plus = stroke("plus", 2.4f, "M12 5v14M5 12h14")
    val Close = stroke("close", 2.4f, "M6 6l12 12M18 6L6 18")
    val Trash = stroke("trash", 2f, "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3")
    val Flag = stroke("flag", 2.4f, "M5 21V4h12l-2 4 2 4H5")
    val Zap = stroke("zap", 2.4f, "M13 2L4 14h7l-1 8 9-12h-7z")
}
