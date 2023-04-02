package ornicar.scalalib

import java.time.{ Instant, LocalDateTime, ZoneOffset }
import java.time.temporal.ChronoUnit

object time:

  extension (d: LocalDateTime) def toMillis: Long = d.toInstant(ZoneOffset.UTC).toEpochMilli

  def millisToDate(millis: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)

  def daysBetween(from: LocalDateTime, to: LocalDateTime): Int =
    ChronoUnit.DAYS.between(from, to).toInt