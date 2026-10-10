package com.fairwayfit.app.domain

import java.time.LocalDate

/**
 * All static training content lives here: the baseline tests, the exercise library and the workouts.
 * Edit this file to change what the app teaches; nothing else hard-codes exercises.
 */
object Content {

    // ---------------------------------------------------------------- tests

    val tests: List<FitnessTest> = listOf(
        FitnessTest(
            key = "bal_l", name = "Single-Leg Balance — Left", category = Category.STABILITY,
            unit = "sec", timed = true, min = 0.0, max = 60.0, step = 1.0, start = 0.0,
            floor = 3.0, target = 30.0,
            summary = "Eyes closed, standing on your left leg.",
            steps = listOf(
                "Stand barefoot on a hard, flat floor with your hands on your hips.",
                "Lift your right foot a few inches and close your eyes.",
                "Start the stopwatch the moment your foot leaves the floor.",
                "Stop when you open your eyes, hop, or the lifted foot touches down (cap: 60 s).",
                "Take the best of two attempts."
            ),
            why = "Ground-reaction balance on one leg mirrors your trail/lead leg load during the swing."
        ),
        FitnessTest(
            key = "bal_r", name = "Single-Leg Balance — Right", category = Category.STABILITY,
            unit = "sec", timed = true, min = 0.0, max = 60.0, step = 1.0, start = 0.0,
            floor = 3.0, target = 30.0,
            summary = "Eyes closed, standing on your right leg.",
            steps = listOf(
                "Stand barefoot on a hard, flat floor with your hands on your hips.",
                "Lift your left foot a few inches and close your eyes.",
                "Start the stopwatch the moment your foot leaves the floor.",
                "Stop when you open your eyes, hop, or the lifted foot touches down (cap: 60 s).",
                "Take the best of two attempts."
            ),
            why = "Comparing left and right shows asymmetries that cost you consistency."
        ),
        FitnessTest(
            key = "plank", name = "Front Plank Hold", category = Category.STABILITY,
            unit = "sec", timed = true, min = 0.0, max = 300.0, step = 1.0, start = 0.0,
            floor = 20.0, target = 120.0,
            summary = "How long you can hold a perfect plank.",
            steps = listOf(
                "Set up on your forearms with elbows under shoulders and legs straight.",
                "Brace your abs and squeeze your glutes. Keep a straight line from head to heels.",
                "Start the stopwatch and hold.",
                "Stop the moment your hips sag or pike — quality counts, not duration at any cost."
            ),
            why = "Trunk endurance keeps your spine posture steady through 18 holes."
        ),
        FitnessTest(
            key = "side_plank", name = "Side Plank Hold (weaker side)", category = Category.STABILITY,
            unit = "sec", timed = true, min = 0.0, max = 180.0, step = 1.0, start = 0.0,
            floor = 10.0, target = 60.0,
            summary = "Lateral core strength on your weaker side.",
            steps = listOf(
                "Lie on your side with your elbow under your shoulder and feet stacked.",
                "Lift your hips so your body forms a straight line.",
                "Test both sides if you like; record the shorter time.",
                "Stop when your hips drop."
            ),
            why = "Lateral trunk strength controls side-bend and posture through impact."
        ),
        FitnessTest(
            key = "broad_jump", name = "Standing Broad Jump", category = Category.POWER,
            unit = "in", timed = false, min = 0.0, max = 130.0, step = 1.0, start = 70.0,
            floor = 60.0, target = 110.0,
            summary = "Explosive lower-body power, a known marker for swing speed.",
            steps = listOf(
                "Mark a start line and stand with feet shoulder-width behind it.",
                "Swing your arms back, then jump forward as far as you can.",
                "Land on both feet and stay standing.",
                "Measure from the line to the back of your rear heel.",
                "Best of three attempts, with a short rest between."
            ),
            why = "Jump distance tracks how quickly you can push force into the ground."
        ),
        FitnessTest(
            key = "squats30", name = "30-Second Squat Count", category = Category.POWER,
            unit = "reps", timed = false, countdownSeconds = 30,
            min = 0.0, max = 60.0, step = 1.0, start = 15.0,
            floor = 12.0, target = 28.0,
            summary = "Bodyweight squats as fast as you can with full depth.",
            steps = listOf(
                "Set a chair behind you as a depth guide.",
                "Squat until your thighs are parallel (lightly touch the chair), then stand fully.",
                "Tap Start 30 s countdown and do as many clean reps as you can.",
                "Count only full-depth reps."
            ),
            why = "Repeatable leg speed and endurance for strong, fast legs late in a round."
        ),
        FitnessTest(
            key = "sit_reach", name = "Sit-and-Reach", category = Category.MOBILITY,
            unit = "in", timed = false, min = -10.0, max = 12.0, step = 0.5, start = 0.0,
            floor = -4.0, target = 4.0,
            summary = "Hamstring and low-back flexibility.",
            steps = listOf(
                "Sit on the floor with legs straight and feet flat against a box or wall.",
                "Reach forward slowly with both hands, palms down, and hold for 2 seconds.",
                "Measure relative to your toes: beyond toes is positive, short of toes is negative.",
                "Best of three; do a few easy reaches first to warm up."
            ),
            why = "Tight hamstrings limit hip hinge and pull you out of posture at address."
        ),
        FitnessTest(
            key = "rotation", name = "Seated Trunk Rotation (weaker side)", category = Category.MOBILITY,
            unit = "°", timed = false, min = 0.0, max = 90.0, step = 5.0, start = 40.0,
            floor = 25.0, target = 55.0,
            summary = "How far your upper body turns without your hips moving.",
            steps = listOf(
                "Sit tall on a chair with a club across your shoulders and hips square.",
                "Rotate as far as you can without letting your hips shift.",
                "Have a partner (or a phone video) estimate the angle your club turned.",
                "Test both sides and record the smaller one. Use the same method every time."
            ),
            why = "Rotation range lets you coil without compensating with your lower back."
        )
    )

    // ------------------------------------------------------------ exercises

    val exercises: List<Exercise> = listOf(
        // ---- Stability
        Exercise(
            "plank", "Front Plank", Category.STABILITY, "Core stability", "None",
            listOf(
                "Set your forearms on the floor with elbows under shoulders.",
                "Brace your abs and squeeze your glutes.",
                "Hold a straight line from head to heels; breathe steadily."
            ),
            "Ribs down, glutes tight",
            "Teaches your trunk to stay rigid while hips and shoulders move, so ground force reaches the club."
        ),
        Exercise(
            "side_plank", "Side Plank", Category.STABILITY, "Lateral core", "None",
            listOf(
                "Lie on your side with your elbow under your shoulder.",
                "Lift your hips into a straight line from head to feet.",
                "Keep your top shoulder stacked over the bottom one."
            ),
            "Push the floor away",
            "Builds the side-trunk strength that controls posture through impact."
        ),
        Exercise(
            "dead_bug", "Dead Bug", Category.STABILITY, "Core control", "None",
            listOf(
                "Lie on your back with arms up and knees bent over hips.",
                "Press your low back gently into the floor.",
                "Lower the opposite arm and leg slowly, then return. Alternate sides."
            ),
            "Back stays glued to the floor",
            "Trains your core to hold the spine still while your limbs move."
        ),
        Exercise(
            "bird_dog", "Bird Dog", Category.STABILITY, "Spine and hip stability", "None",
            listOf(
                "Start on hands and knees with a flat back.",
                "Reach one arm forward and the opposite leg back.",
                "Hold briefly without rotating your hips, then switch."
            ),
            "Pretend a glass of water sits on your lower back",
            "Teaches opposite-limb coordination without losing trunk position."
        ),
        Exercise(
            "pallof", "Pallof Press", Category.STABILITY, "Anti-rotation core", "Resistance band",
            listOf(
                "Anchor a band at chest height and stand sideways to it.",
                "Hold the band at your chest with both hands.",
                "Press straight out and resist the band's pull to rotate you.",
                "Hold a second, return slowly."
            ),
            "Stay square — don't let it turn you",
            "Resisting rotation is the skill that lets you rotate fast on purpose."
        ),
        Exercise(
            "sl_rdl", "Single-Leg Romanian Deadlift", Category.STABILITY, "Hip hinge and balance", "None or dumbbell",
            listOf(
                "Stand on one leg with a soft knee.",
                "Hinge at the hip, sending the other leg straight back.",
                "Keep your hips square to the floor.",
                "Drive through the standing heel to return."
            ),
            "Long spine, hips level",
            "Mimics the hip hinge and single-leg loading of the golf setup and downswing."
        ),
        Exercise(
            "split_squat", "Split Squat", Category.STABILITY, "Single-leg strength", "None or dumbbells",
            listOf(
                "Take a long staggered stance.",
                "Lower straight down until the back knee nearly touches the floor.",
                "Push through the front heel to stand."
            ),
            "Torso tall, front knee tracks the toes",
            "Builds leg strength and the lateral stability of your lead side."
        ),
        Exercise(
            "sl_balance_reach", "Single-Leg Balance Reach", Category.STABILITY, "Balance and control", "None",
            listOf(
                "Stand on one leg with a soft knee.",
                "Reach the free foot forward, to the side, then behind you in a slow arc.",
                "Touch lightly without putting weight on it."
            ),
            "Slow and quiet",
            "Challenges balance in every direction, like uneven lies."
        ),
        Exercise(
            "band_walk", "Lateral Band Walk", Category.STABILITY, "Glute strength", "Mini band",
            listOf(
                "Place a mini band above your knees and sit into a quarter squat.",
                "Step sideways, keeping the band tight.",
                "Do not let your knees cave in."
            ),
            "Push knees out into the band",
            "Strong glutes keep your pelvis stable as you load and unload."
        ),
        Exercise(
            "suitcase_carry", "Suitcase Carry", Category.STABILITY, "Lateral core and grip", "Dumbbell or kettlebell",
            listOf(
                "Hold a weight in one hand at your side.",
                "Stand tall and walk without leaning toward the weight.",
                "Switch hands for the next set."
            ),
            "Stay perfectly upright",
            "Resists side-bend under load, protecting your back."
        ),
        // ---- Speed and power
        Exercise(
            "med_ball_rot_throw", "Rotational Med-Ball Throw", Category.POWER, "Rotational power", "Medicine ball and wall",
            listOf(
                "Stand side-on to a sturdy wall holding a 6–10 lb ball at your hip.",
                "Rotate away slightly, then turn explosively and throw the ball into the wall.",
                "Catch it and reset. Switch sides after your reps."
            ),
            "Push the ground, turn, release",
            "Trains the ground-up chain that creates club head speed."
        ),
        Exercise(
            "broad_jump", "Broad Jump", Category.POWER, "Lower-body power", "None",
            listOf(
                "Stand tall with feet shoulder-width.",
                "Dip, swing your arms and jump forward as far as you can.",
                "Land softly on both feet and reset."
            ),
            "Jump out, stick the landing",
            "Teaches your legs to produce force fast."
        ),
        Exercise(
            "jump_squat", "Jump Squat", Category.POWER, "Leg speed", "None",
            listOf(
                "Squat to a comfortable depth.",
                "Explode upward to leave the ground.",
                "Land softly and go straight into the next rep."
            ),
            "Fast up, soft landing",
            "Builds rate of force development in your legs."
        ),
        Exercise(
            "skater_bound", "Lateral Skater Bound", Category.POWER, "Lateral power", "None",
            listOf(
                "Stand on one leg with a soft knee.",
                "Bound sideways to the opposite leg and stick the landing.",
                "Pause a half second, then bound back."
            ),
            "Stick the landing",
            "Mirrors the weight shift of the downswing and builds lateral stability."
        ),
        Exercise(
            "kb_swing", "Kettlebell Swing", Category.POWER, "Hip power", "Kettlebell",
            listOf(
                "Hinge back with a flat spine and the bell between your legs.",
                "Snap your hips forward to float the bell to chest height.",
                "Let it fall back and hinge again. Arms are just hooks."
            ),
            "Hips snap, arms relax",
            "Builds the hip drive that powers the downswing."
        ),
        Exercise(
            "med_ball_slam", "Med-Ball Slam", Category.POWER, "Full-body power", "Medicine ball",
            listOf(
                "Raise the ball overhead and rise onto your toes.",
                "Slam it down hard, hinging through your hips.",
                "Pick it up and repeat."
            ),
            "Throw it through the floor",
            "Teaches you to produce force with intent."
        ),
        Exercise(
            "overspeed", "Speed Swings", Category.POWER, "Swing speed", "Light training club or your own club",
            listOf(
                "Warm up thoroughly first.",
                "Swing a light club (or hold your club upside-down by the head end) with full intent.",
                "Aim to hear the 'whoosh' at the point of impact.",
                "Rest fully between sets; stop if form degrades."
            ),
            "Fast and balanced",
            "Light-club swings train your nervous system to move faster than usual."
        ),
        // ---- Mobility
        Exercise(
            "open_book", "Open Book Rotation", Category.MOBILITY, "Thoracic rotation", "None",
            listOf(
                "Lie on your side with knees stacked and bent.",
                "Open your top arm like a book toward the opposite side.",
                "Follow your hand with your eyes, then return."
            ),
            "Knees stay together",
            "Frees up your upper back so you can coil without bending your lower back."
        ),
        Exercise(
            "hip_9090", "90/90 Hip Switch", Category.MOBILITY, "Hip rotation", "None",
            listOf(
                "Sit with both knees bent at 90 degrees, one forward and one to the side.",
                "Rotate your knees across to the other side.",
                "Sit tall and keep your hands off the floor if you can."
            ),
            "Tall spine, slow switch",
            "Opens internal and external hip rotation that you need in the golf swing."
        ),
        Exercise(
            "t_spine_ext", "Thoracic Extension", Category.MOBILITY, "Upper-back extension", "Foam roller or rolled towel",
            listOf(
                "Lie with the roller across your upper back and support your head.",
                "Lean back over the roller and exhale.",
                "Move the roller up and down one segment at a time."
            ),
            "Keep ribs down",
            "Improves posture and the room you have to turn your shoulders."
        ),
        Exercise(
            "worlds_greatest", "World's Greatest Stretch", Category.MOBILITY, "Total-body mobility", "None",
            listOf(
                "Step into a deep lunge with both hands inside the front foot.",
                "Rotate your chest toward the front knee and reach to the ceiling.",
                "Return and switch sides."
            ),
            "Reach high, hips low",
            "Opens hips, spine and shoulders in one move."
        ),
        Exercise(
            "cat_cow", "Cat–Cow", Category.MOBILITY, "Spine mobility", "None",
            listOf(
                "Get on hands and knees.",
                "Round your back toward the ceiling, then drop your belly and look up.",
                "Move slowly with your breath."
            ),
            "Slow and smooth",
            "Gently wakes up the spine before training."
        ),
        Exercise(
            "hip_flexor", "Half-Kneeling Hip Flexor Stretch", Category.MOBILITY, "Hip extension", "None",
            listOf(
                "Kneel on one knee with the other foot forward.",
                "Squeeze the glute of the kneeling leg and tuck your pelvis.",
                "Shift forward slightly until you feel a stretch at the front of the hip."
            ),
            "Glute on, ribs down",
            "Desk-bound hips limit your hip turn and posture; this restores them."
        ),
        Exercise(
            "leg_swing", "Leg Swings", Category.MOBILITY, "Dynamic hip warm-up", "None",
            listOf(
                "Hold a wall for balance.",
                "Swing one leg forward and back in a controlled arc.",
                "Then swing it across your body and out to the side."
            ),
            "Tall, relaxed, controlled",
            "Dynamic warm-up for your hips and hamstrings."
        ),
        Exercise(
            "standing_rot", "Standing Trunk Rotations", Category.MOBILITY, "Rotational warm-up", "Club",
            listOf(
                "Hold a club across your shoulders with feet shoulder-width.",
                "Rotate smoothly to each side, turning through the hips and trunk.",
                "Increase the range gradually."
            ),
            "Smooth, not forced",
            "Primes the rotation pattern before your first swing."
        )
    )

    // ------------------------------------------------------------- workouts

    val workouts: List<Workout> = listOf(
        Workout(
            id = "stability", name = "Stability Foundation", badge = "STB",
            tagline = "Core, hips and single-leg control",
            notes = "Move slowly and own every rep. Stop a set when your form breaks.",
            category = Category.STABILITY, restSeconds = 30,
            items = listOf(
                WorkoutItem("plank", 3, seconds = 30),
                WorkoutItem("side_plank", 3, seconds = 20, perSide = true),
                WorkoutItem("dead_bug", 3, reps = 10),
                WorkoutItem("bird_dog", 2, reps = 8, perSide = true),
                WorkoutItem("pallof", 3, reps = 10, perSide = true),
                WorkoutItem("sl_rdl", 3, reps = 8, perSide = true),
                WorkoutItem("split_squat", 3, reps = 8, perSide = true),
                WorkoutItem("sl_balance_reach", 2, reps = 5, perSide = true)
            )
        ),
        Workout(
            id = "power", name = "Speed & Power", badge = "PWR",
            tagline = "Explosive legs, hips and rotation",
            notes = "Do the Pre-Round Warm-Up first. Move fast, rest fully — quality beats quantity.",
            category = Category.POWER, restSeconds = 45,
            items = listOf(
                WorkoutItem("jump_squat", 3, reps = 6),
                WorkoutItem("broad_jump", 3, reps = 5),
                WorkoutItem("skater_bound", 3, reps = 6, perSide = true),
                WorkoutItem("med_ball_rot_throw", 4, reps = 6, perSide = true),
                WorkoutItem("kb_swing", 3, reps = 10),
                WorkoutItem("med_ball_slam", 3, reps = 8),
                WorkoutItem("overspeed", 3, reps = 5)
            )
        ),
        Workout(
            id = "total", name = "Total Golf Circuit", badge = "TOT",
            tagline = "Stability and power in one session",
            notes = "A balanced mix when you only have one session to spare.",
            category = null, restSeconds = 30,
            items = listOf(
                WorkoutItem("plank", 2, seconds = 30),
                WorkoutItem("pallof", 2, reps = 10, perSide = true),
                WorkoutItem("sl_rdl", 2, reps = 8, perSide = true),
                WorkoutItem("kb_swing", 3, reps = 10),
                WorkoutItem("med_ball_rot_throw", 3, reps = 6, perSide = true),
                WorkoutItem("side_plank", 2, seconds = 20, perSide = true),
                WorkoutItem("skater_bound", 2, reps = 6, perSide = true),
                WorkoutItem("suitcase_carry", 2, seconds = 30, perSide = true)
            )
        ),
        Workout(
            id = "mobility", name = "Mobility Reset", badge = "MOB",
            tagline = "Hips, thoracic spine and hamstrings",
            notes = "Easy breathing. Never force a stretch into pain.",
            category = Category.MOBILITY, restSeconds = 10,
            items = listOf(
                WorkoutItem("open_book", 2, reps = 8, perSide = true),
                WorkoutItem("hip_9090", 2, reps = 6, perSide = true),
                WorkoutItem("t_spine_ext", 2, reps = 8),
                WorkoutItem("cat_cow", 1, reps = 10),
                WorkoutItem("worlds_greatest", 2, reps = 4, perSide = true),
                WorkoutItem("hip_flexor", 2, seconds = 30, perSide = true)
            )
        ),
        Workout(
            id = "warmup", name = "Pre-Round Warm-Up", badge = "WRM",
            tagline = "8 minutes before you tee off",
            notes = "Do this before a round or a hard session.",
            category = null, restSeconds = 5,
            items = listOf(
                WorkoutItem("leg_swing", 1, reps = 10, perSide = true),
                WorkoutItem("worlds_greatest", 1, reps = 3, perSide = true),
                WorkoutItem("standing_rot", 1, reps = 12),
                WorkoutItem("overspeed", 2, reps = 5)
            )
        )
    )

    private val exerciseById = exercises.associateBy { it.id }
    private val workoutById = workouts.associateBy { it.id }
    private val testByKey = tests.associateBy { it.key }

    init {
        require(exerciseById.size == exercises.size) { "duplicate exercise id" }
        require(workoutById.size == workouts.size) { "duplicate workout id" }
        require(testByKey.size == tests.size) { "duplicate test key" }
        workouts.forEach { w ->
            w.items.forEach { require(it.exerciseId in exerciseById) { "workout ${w.id} references unknown exercise ${it.exerciseId}" } }
        }
    }

    fun exercise(id: String): Exercise = exerciseById.getValue(id)
    fun workout(id: String): Workout = workoutById.getValue(id)
    fun test(key: String): FitnessTest = testByKey.getValue(key)
}

/** Weekly schedule: the user's training days rotate through this cycle in weekday order. */
object Plan {
    private val cycle = listOf("stability", "power", "total", "mobility")

    fun sessionFor(days: Set<Int>, date: LocalDate): Workout? {
        val sorted = days.sorted()
        val index = sorted.indexOf(date.dayOfWeek.value)
        return if (index < 0) null else Content.workout(cycle[index % cycle.size])
    }

    /** The next scheduled session on or after [from]. With no days configured it suggests Stability today. */
    fun next(days: Set<Int>, from: LocalDate): Pair<LocalDate, Workout> {
        if (days.isEmpty()) return from to Content.workout("stability")
        for (offset in 0..6) {
            val date = from.plusDays(offset.toLong())
            sessionFor(days, date)?.let { return date to it }
        }
        error("unreachable: days is non-empty so one weekday must match")
    }
}
