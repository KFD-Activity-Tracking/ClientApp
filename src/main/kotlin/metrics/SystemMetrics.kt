package metrics

import java.io.File

object SystemMetrics {

    fun readCpuPercent(): Double {
        val (idle1, total1) = parseProcStat()
        Thread.sleep(200)
        val (idle2, total2) = parseProcStat()
        val totalDelta = total2 - total1
        val idleDelta = idle2 - idle1
        return if (totalDelta > 0) (1.0 - idleDelta.toDouble() / totalDelta) * 100.0 else 0.0
    }

    fun readRamPercent(): Double {
        val lines = runCatching { File("/proc/meminfo").readLines() }.getOrNull() ?: return 0.0
        val total = lines.firstOrNull { it.startsWith("MemTotal:") }
            ?.split("\\s+".toRegex())?.getOrNull(1)?.toLongOrNull() ?: return 0.0
        val available = lines.firstOrNull { it.startsWith("MemAvailable:") }
            ?.split("\\s+".toRegex())?.getOrNull(1)?.toLongOrNull() ?: return 0.0
        return (total - available).toDouble() / total * 100.0
    }

    fun readGpuPercent(): Double {
        runCatching {
            return ProcessBuilder("nvidia-smi", "--query-gpu=utilization.gpu", "--format=csv,noheader,nounits")
                .start().inputStream.bufferedReader().readText().trim().toDouble()
        }
        runCatching {
            return File("/sys/class/drm/card0/device/gpu_busy_percent").readText().trim().toDouble()
        }
        return 0.0
    }

    private fun parseProcStat(): Pair<Long, Long> {
        val parts = runCatching {
            File("/proc/stat").readLines().first().split("\\s+".toRegex()).drop(1).map { it.toLong() }
        }.getOrNull() ?: return Pair(0L, 1L)
        val idle = parts.getOrElse(3) { 0L }
        val total = parts.sum()
        return Pair(idle, total)
    }
}

class SystemMetricsCollector {
    private var cpuSum = 0.0
    private var ramSum = 0.0
    private var gpuSum = 0.0
    private var count = 0

    fun sample() {
        cpuSum += SystemMetrics.readCpuPercent()
        ramSum += SystemMetrics.readRamPercent()
        gpuSum += SystemMetrics.readGpuPercent()
        count++
    }

    fun averages(): Triple<Double, Double, Double> =
        if (count == 0) Triple(0.0, 0.0, 0.0)
        else Triple(cpuSum / count, ramSum / count, gpuSum / count)
}
