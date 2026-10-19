package com.example.photogether.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = startup(StartupMode.COLD)

    @Test
    fun warmStartup() = startup(StartupMode.WARM)

    private fun startup(mode: StartupMode) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = mode,
        compilationMode = CompilationMode.Partial(),
        setupBlock = {
            grantAppPermissions()
            pressHome()
        },
    ) {
        launchMainActivity()
    }

    companion object {
        const val TARGET_PACKAGE = "com.tgound.photogether"
    }
}
