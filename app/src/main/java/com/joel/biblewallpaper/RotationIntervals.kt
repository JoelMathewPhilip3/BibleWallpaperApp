package com.joel.biblewallpaper

object RotationIntervals {
    data class Choice(val minutes: Int,val label: String)
    val choices = listOf(Choice(15,"15 minutes"),Choice(30,"30 minutes"),Choice(60,"1 hour"),
        Choice(120,"2 hours"),Choice(240,"4 hours"),Choice(480,"8 hours"),Choice(0,"Manual only"))
    fun valid(minutes: Int) = if (choices.any { it.minutes == minutes }) minutes else 30
    fun label(minutes: Int) = choices.first { it.minutes == valid(minutes) }.label
    fun flex(minutes: Int): Long = (valid(minutes)/4).coerceIn(5,60).toLong()
}
