package ch.teamorg.infra

import ch.teamorg.domain.repositories.EventRepository
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours

private val logger = LoggerFactory.getLogger("EventMaterialisationJob")

fun Application.startMaterialisationJob() {
    val eventRepository by inject<EventRepository>()
    val backfillJob by inject<AbwesenheitBackfillJob>()

    launch(Dispatchers.IO) {
        try {
            val count = eventRepository.materialiseUpcomingOccurrences()
            logger.info("Initial materialisation complete: $count new occurrences created")
            if (count > 0) backfillJob.applyRulesToAllFutureEvents()
        } catch (e: Exception) {
            logger.error("Initial materialisation failed", e)
        }

        while (isActive) {
            delay(24.hours)
            try {
                val count = eventRepository.materialiseUpcomingOccurrences()
                logger.info("Daily materialisation complete: $count new occurrences created")
                if (count > 0) backfillJob.applyRulesToAllFutureEvents()
            } catch (e: Exception) {
                logger.error("Daily materialisation failed", e)
            }
        }
    }
}
