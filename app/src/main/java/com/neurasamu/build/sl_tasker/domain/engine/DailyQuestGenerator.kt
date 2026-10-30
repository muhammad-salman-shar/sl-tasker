package com.neurasamu.build.sl_tasker.domain.engine

import com.neurasamu.build.sl_tasker.data.model.DailyQuestEntity
import java.util.Calendar

object DailyQuestGenerator {

    data class Template(
        val title: String,
        val description: String,
        val target: Int,
        val unit: String,
        val ep: Int,
        val hp: Int
    )

    private val TEMPLATES = listOf(
        Template("Read", "Read some pages to sharpen your mind.", 10, "pages", 15, 5),
        Template("Exercise", "Move your body — pushups, squats, or a quick workout.", 20, "minutes", 20, 10),
        Template("Meditate", "Sit in silence and breathe deeply.", 10, "minutes", 15, 5),
        Template("Hydrate", "Drink water throughout the day.", 8, "glasses", 10, 5),
        Template("Walk", "Walk around, get fresh air.", 5000, "steps", 25, 10),
        Template("Study", "Focus on learning something new.", 30, "minutes", 25, 10),
        Template("Journal", "Reflect on your day in writing.", 1, "entry", 15, 5),
        Template("Digital Detox", "Avoid social media for a while.", 2, "hours", 25, 10),
        Template("Sleep", "Sleep on time for recovery.", 7, "hours", 20, 10),
        Template("Cook", "Prepare a healthy meal.", 1, "meal", 15, 5),
        Template("Clean", "Tidy up your space.", 1, "room", 10, 5),
        Template("Practice Skill", "Sharpen a skill you care about.", 20, "minutes", 20, 10),
        Template("Call Family", "Connect with someone you love.", 1, "call", 10, 5),
        Template("Stretch", "Stretch your muscles.", 5, "minutes", 5, 3),
        Template("No Sugar", "Skip sugar for the day.", 1, "day", 20, 10),
        Template("Plan Tomorrow", "Write down tomorrow's tasks.", 1, "plan", 10, 5),
        Template("Gratitude", "Write 3 things you're grateful for.", 3, "items", 10, 5),
        Template("Deep Work", "Do focused work without distraction.", 45, "minutes", 30, 15)
    )

    fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun generateForToday(dayEpoch: Long, count: Int = 3): List<DailyQuestEntity> {
        val picked = TEMPLATES.shuffled().take(count)
        return picked.map { t ->
            DailyQuestEntity(
                title = "${t.title} ${t.target} ${t.unit}",
                description = t.description,
                targetCount = t.target,
                unit = t.unit,
                bonusEp = t.ep,
                bonusHp = t.hp,
                dayEpoch = dayEpoch
            )
        }
    }
}
