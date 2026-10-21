package com.relentnet.cuttracker.data

/**
 * The words the app speaks. Climbing reads as the app always has; [Settings.training] set to the user's own word swaps
 * the climbing words for plain ones. The wall, the shapes, the rope, Route, Min day and the chain stay in both.
 * Screens read it from LocalVocab, the widget, the reminders and the engine from [Settings.vocab].
 */
data class Vocab(
    val climbing: Boolean,
    /** The session item on a training day, when it still has its default name. */
    val session: String,
    val sessionSub: String,
    /** The push block's sub after its lift names. */
    val blockSub: String,
    val blockWhere: String,
    val trainDay: String,
    /** The kind of day in a word, for keys and short lists. */
    val train: String,
    val trained: String,
    val sessionThenBlock: String,
    val sessionNoBlock: String,
    val noSession: String,
    val send: String,
    val sends: String,
    val sendsLower: String,
    val sent: String,
    /** The takeover's eyebrow over [sent]. */
    val sendTag: String,
    val fullSend: String,
    val hold: String,
    val holds: String,
    val beta: String,
    val newGrade: String,
    val firstSend: String,
    val chalkOwn: String,
    val chalkIt: String,
    val chalkSheetSub: String,
    val looksSub: String,
    val customLook: String,
    val kindPlaceholder: String,
    val wentGreen: String,
    val widgetGreen: String,
    val onboardHold: String,
    val wallInfo: String,
    val topOut: String,
    val toppedOut: String,
    /** The big words on the first landing after onboarding. */
    val startCall: String,
    /** What a slow cut protects, for onboarding's pace line. */
    val keeps: String,
    /** Look blurbs by palette id, where the climbing one would not read. */
    val looks: Map<String, String> = emptyMap(),
) {
    /** Climbing grades start at V0; plain levels start at 1. */
    fun day(mode: DayMode) = when (mode) { DayMode.CLIMB -> trainDay; DayMode.OFF -> "Off day"; DayMode.REST -> "Rest day" }
    fun level(n: Int) = if (climbing) "V$n" else "Level ${n + 1}"
    fun sendCount(n: Int) = "$n " + (if (n == 1) sendsLower.removeSuffix("s") else sendsLower)
    fun lookLine(id: String, line: String) = looks[id] ?: line.replace(CLIMBING.customLook, customLook)
    fun note(n: String) = if (n == CLIMBING.topOut) topOut else n
    /** A plan item as shown: the climbing session takes the user's word while it still has the default name. */
    fun item(p: PlanItem) = if (p.id != ProgramSpec.ITEM_CLIMB) p else p.copy(
        label = if (p.label == CLIMBING.session) session else p.label,
        sub = if (p.sub == CLIMBING.sessionSub) sessionSub else p.sub,
    )

    companion object {
        val CLIMBING = Vocab(
            climbing = true,
            session = "Climb", sessionSub = "Climbing session. Before lunch.", blockSub = "Last 15 min at the gym.", blockWhere = "at the gym, after the session",
            trainDay = "Climb day", train = "Climb", trained = "I climbed",
            sessionThenBlock = "Session, then the push block at the gym.", sessionNoBlock = "Session at the gym. No block.", noSession = "No climbing.",
            send = "Send", sends = "Sends", sendsLower = "sends", sent = "Sent", sendTag = "Send", fullSend = "xp for a full Send",
            hold = "hold", holds = "holds", beta = "Beta", newGrade = "New grade", firstSend = "First Send",
            chalkOwn = "Chalk your own", chalkIt = "Chalk it",
            chalkSheetSub = "One hue on one stone. The sends and the freeze pick themselves so nothing clashes.",
            looksSub = "The stone, the chalk and the sends change. The type and the layout never do.",
            customLook = "Your chalk.", kindPlaceholder = "New kind, like Pre-climb",
            wentGreen = "Every item ticked. Today's hold just went green.", widgetGreen = "Every item ticked. Hold went green.",
            onboardHold = "Every day is a hold. Pick which kind, finish the script, and the hold goes green.",
            wallInfo = "Every hold is one day. The shape is the kind of day you picked, the color is how it went. The rope is your chain. Tap any hold to open that day; late checks still count.",
            topOut = "Last checkpoint. Top out.", toppedOut = "Topped out.",
            startCall = "Climb on.", keeps = "your pull",
        )

        /** [name] is what the user calls their training: Run, Gym, Practice. */
        fun plain(name: String): Vocab {
            val n = name.trim().ifBlank { "Training" }
            return Vocab(
                climbing = false,
                session = n, sessionSub = "$n session. Before lunch.", blockSub = "15 min after the session.", blockWhere = "after the session",
                trainDay = "Training day", train = "Training", trained = "I trained",
                sessionThenBlock = "$n, then the push block.", sessionNoBlock = "$n. No block.", noSession = "No session.",
                send = "Full day", sends = "Full days", sendsLower = "full days", sent = "Full day", sendTag = "Today", fullSend = "xp for a Full day",
                hold = "day", holds = "days", beta = "Plan", newGrade = "New level", firstSend = "First Full day",
                chalkOwn = "Make your own", chalkIt = "Make it",
                chalkSheetSub = "One hue on one stone. The full days and the freeze pick themselves so nothing clashes.",
                looksSub = "The colors change. The type and the layout never do.",
                customLook = "Your colors.", kindPlaceholder = "New kind, like Pre-workout",
                wentGreen = "Every item ticked. Today just went green.", widgetGreen = "Every item ticked. Day went green.",
                onboardHold = "Every day is a shape on the wall. Pick which kind, finish the script, and it goes green.",
                wallInfo = "Every shape is one day. The shape is the kind of day you picked, the color is how it went. The rope is your chain. Tap any one to open that day; late checks still count.",
                topOut = "Last checkpoint. Finish line.", toppedOut = "Route complete.",
                startCall = "Day one.", keeps = "your strength",
                looks = mapOf(
                    "sandstone" to "Warm white on dark stone. The one the app was drawn in.",
                    "granite" to "Cold grey stone, ice blue accents. For the gym at 6 am.",
                    "moss" to "Forest floor. Amber and green.",
                    "ember" to "Red and gold. Hot days on the wall.",
                    "dusk" to "Purple stone after sundown. Mint full days.",
                    "glacier" to "Deep water and ice. Cyan and lime.",
                    "harvest" to "Pumpkin, gold, a purple freeze. Autumn on the wall.",
                    "halloween" to "Jack-o'-lantern orange on a purple-black wall. Every day gets a face.",
                    "winter" to "Pine and ember on dark ice, snow on every day. The last week of the year.",
                    "newyear" to "Gold on black, a spark on every day. One night a year.",
                    "rose" to "Rose and blush, a heart on every day. Mid-February.",
                    "fourth" to "Red, white and blue, bunting on every day. Early July.",
                ),
            )
        }

        fun of(training: String?) = if (training == null) CLIMBING else plain(training)
    }
}

val Settings.vocab: Vocab get() = Vocab.of(training)
