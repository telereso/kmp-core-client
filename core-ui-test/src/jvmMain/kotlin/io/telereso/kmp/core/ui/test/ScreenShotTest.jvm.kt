/*
 * MIT License
 *
 * Copyright (c) 2023 Telereso
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.telereso.kmp.core.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import io.telereso.kmp.core.ui.pages.DeviceInfo
import io.telereso.kmp.core.ui.pages.Simulator
import io.telereso.kmp.core.ui.pages.rememberSimulatorState
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

private val screenShotDir = System.getProperty("user.dir").plus("/telereso/screenShots/desktop")
private val screenShotDirPath = Path(screenShotDir)

private val screenShotDiffDir =
    System.getProperty("user.dir").plus("/build/telereso/screenShots/diff/desktop/")
private val screenShotDiffDirPath = Path(screenShotDiffDir)

private val screenShotReportDirPath = Path(
    System.getProperty("user.dir").plus("/build/telereso/reports/screenShotTest/desktop")
)


@OptIn(ExperimentalTestApi::class)
actual fun runScreenShotTest(
    device: DeviceInfo,
    block: @Composable (Modifier) -> Unit
) {
    val callerName = getCallerName()
    val className = callerName.split(".").let { p -> p[p.lastIndex - 1] }
    val testName = callerName.split(".").let { p -> p[p.lastIndex] }
    val screenShotName = device.fileName().plus("_").plus(callerName)
    var savingFile = false
    runSkikoComposeUiTest(
        size = Size(device.screenWidth.value, device.screenHeight.value)
    ) {
        setContent {
            if (savingFile) return@setContent

            val scope = rememberCoroutineScope()
            val state = rememberSimulatorState(device)
            state.captureScreenShot()
            var byteArrays by remember { mutableStateOf<Pair<ByteArray, ByteArray>?>(null) }

            LaunchedEffect(byteArrays) {
                byteArrays?.apply {
                    scope.launch {
                        val byteArray = verify(first, second, 1f)
                        if (byteArray != null) {
                            if (!SystemFileSystem.exists(screenShotDiffDirPath))
                                SystemFileSystem.createDirectories(screenShotDiffDirPath)

                            SystemFileSystem.sink(screenShotDiffDirPath.child("$screenShotName.png"))
                                .buffered()
                                .use { sink ->
                                    sink.write(byteArray)
                                }

                            val testPath = screenShotReportDirPath.child(className).child(testName)
                            if (!SystemFileSystem.exists(testPath))
                                SystemFileSystem.createDirectories(testPath)

                            val basePath = testPath.child("base.png")
                            val newPath = testPath.child("new.png")
                            val deferencePath = testPath.child("deference.png")

                            SystemFileSystem.sink(basePath)
                                .buffered()
                                .use { sink ->
                                    sink.write(first)
                                }

                            SystemFileSystem.sink(newPath)
                                .buffered()
                                .use { sink ->
                                    sink.write(second)
                                }

                            SystemFileSystem.sink(deferencePath)
                                .buffered()
                                .use { sink ->
                                    sink.write(byteArray)
                                }

                            val reportPath = testPath.child("index.html")
                            SystemFileSystem.sink(reportPath)
                                .buffered()
                                .use { sink ->
                                    sink.write(
                                        getReportTemplate(
                                            className = className,
                                            testName = testName
                                        ).toByteArray()
                                    )
                                }

                            throw AssertionError("Screenshot does not match the expected output for $testName on Device: ${device.name}")
                        }

                    }
                }
            }

            Simulator(
                state = state,
                content = block
            )

            val screenShotByteArray = state.screenShotByteArray
                ?: throw IllegalStateException("Screenshot byteArray can't be null, failed to capture screenshot")

            val baseScreenShot = getBaseScreenShot(screenShotName)
            if (baseScreenShot == null) {
                saveBaseScreenShot(screenShotName, screenShotByteArray)
            } else {
                savingFile = true
                byteArrays = baseScreenShot to screenShotByteArray
            }
        }
    }
}

private fun Path.child(name: String): Path {
    return Path(toString().plus("/$name"))
}

private fun getBaseScreenShot(testName: String): ByteArray? {
    val path = screenShotDirPath.child("$testName.png")
    return if (SystemFileSystem.exists(path)) SystemFileSystem.source(path).buffered()
        .readByteArray() else null
}

private fun saveBaseScreenShot(testName: String, byteArray: ByteArray) {
    if (!SystemFileSystem.exists(screenShotDirPath))
        SystemFileSystem.createDirectories(screenShotDirPath)

    SystemFileSystem
        .sink(screenShotDirPath.child("$testName.png"))
        .buffered()
        .use { sink ->
            sink.write(byteArray)
        }
}

private fun getCallerName(): String {
    //    val functionName = object {}.javaClass.enclosingMethod?.name ?: "screenshot.png"
    val stackTrace = Thread.currentThread().stackTrace
    return if (stackTrace.size > 4) {
        val caller = stackTrace[4]
        "${caller.className}.${caller.methodName}"
    } else {
        "unknown"
    }
}


fun getReportTemplate(
    className: String,
    testName: String
): String {
    val dollar = "$"
    return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ScreenShot Test Report</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
        }
        h1 {
            text-align: center;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 8px;
            text-align: left;
        }
        th {
            background-color: #f4f4f4;
        }
        .image-comparison-container {
            display: flex;
            flex-direction: column;
            align-items: center;
            margin-top: 20px;
        }
        .slider {
            width: 100%;
            max-width: 500px;
            margin-bottom: 10px;
        }
        .image-comparison {
            position: relative;
            width: 100%;
            max-width: 500px;
            height: auto;
        }
        .image-comparison img {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: auto;
        }
        .image-comparison .compare {
            clip-path: inset(0 50% 0 0);
        }
        .difference-section {
            text-align: center;
            margin-top: 40px;
            clear: both;
        }
        .difference-section img {
            width: 100%;
            max-width: 500px;
            height: auto;
        }
    </style>
</head>
<body>
<h1>Screen Unit Test Report</h1>

<!-- Failed Tests Section -->
<h2>Failed Tests</h2>
<table>
    <thead>
    <tr>
        <th>Class Name</th>
        <th>Test Name</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>$className</td>
        <td>$testName</td>
    </tr>
    </tbody>
</table>

<!-- Difference Image Section -->
<div class="difference-section">
    <h2>Difference Image</h2>
    <img src="deference.png" alt="Difference Image">
</div>

<hr>

<!-- Image Comparison Section -->
<h2>Image Comparison</h2>
<div class="image-comparison-container">
    <input type="range" class="slider" min="0" max="100" value="50">
    <div class="image-comparison">
        <img src="base.png" alt="Baseline Image">
        <img src="new.png" alt="Test Image" class="compare">
    </div>
</div>

<script>
    // JavaScript to handle the slider effect
    const slider = document.querySelector('.slider');
    const compareImage = document.querySelector('.compare');

    slider.addEventListener('input', (event) => {
        const value = event.target.value;
        compareImage.style.clipPath = `inset(0 $dollar{100 - value}% 0 0)`;
    });
</script>
</body>
</html>
""".trimIndent()
}


//
//fun verify(
//    baseScreenShotByteArray: ByteArray,
//    newScreenShotByteArray: ByteArray,
//    tolerance: Float
//): ByteArray? {
//    require(tolerance in 0.0..100.0) { "Tolerance must be between 0.0 and 100.0" }
//
//    val baseImage = ImageIO.read(ByteArrayInputStream(baseScreenShotByteArray))
//    val newImage = ImageIO.read(ByteArrayInputStream(newScreenShotByteArray))
//
//    require(baseImage.width == newImage.width && baseImage.height == newImage.height) {
//        "Images must have the same dimensions"
//    }
//
//    val width = baseImage.width
//    val height = baseImage.height
//    val normalizedTolerance = tolerance / 100f
//
//    var hasDifference = false
//    val diffImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
//
//    for (x in 0 until width) {
//        for (y in 0 until height) {
//            val basePixel = Color(baseImage.getRGB(x, y), true)
//            val newPixel = Color(newImage.getRGB(x, y), true)
//
//            if (basePixel.alpha == 0 && newPixel.alpha == 0) {
//                // Both pixels are transparent; keep as-is
//                diffImage.setRGB(x, y, basePixel.rgb)
//            } else {
//                val diff = colorDifference(basePixel, newPixel)
//
//                if (diff > normalizedTolerance) {
//                    hasDifference = true
//                    // Highlight based on presence
//                    when {
//                        basePixel.alpha == 0 -> {
//                            // New pixel (green)
//                            diffImage.setRGB(x, y, Color(0, 255, 0, 255).rgb)
//                        }
//                        newPixel.alpha == 0 -> {
//                            // Removed pixel (red)
//                            diffImage.setRGB(x, y, Color(255, 0, 0, 255).rgb)
//                        }
//                        else -> {
//                            // Significant difference, highlight in green
//                            diffImage.setRGB(x, y, Color(0, 255, 0, 255).rgb)
//                        }
//                    }
//                } else {
//                    // No significant difference; copy the base pixel
//                    diffImage.setRGB(x, y, baseImage.getRGB(x, y))
//                }
//            }
//        }
//    }
//
//    // If no differences were found, return null (images match within tolerance)
//    return if (hasDifference) {
//        val outputStream = ByteArrayOutputStream()
//        writeImage(diffImage, outputStream)
//        outputStream.toByteArray()
//    } else {
//        null
//    }
//}
//
//// Function to compute the color difference
//private fun colorDifference(color1: Color, color2: Color): Float {
//    val redDiff = Math.abs(color1.red - color2.red) / 255f
//    val greenDiff = Math.abs(color1.green - color2.green) / 255f
//    val blueDiff = Math.abs(color1.blue - color2.blue) / 255f
//    val alphaDiff = Math.abs(color1.alpha - color2.alpha) / 255f
//    return (redDiff + greenDiff + blueDiff + alphaDiff) / 4
//}
//
//// Function to write the image to a ByteArrayOutputStream with no lossy compression
//private fun writeImage(image: BufferedImage, outputStream: ByteArrayOutputStream) {
//    val writer = ImageIO.getImageWritersByFormatName("png").next()
//    val output = ImageIO.createImageOutputStream(outputStream)
//    writer.output = output
//
//    val param: ImageWriteParam = writer.defaultWriteParam
//    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
//    param.compressionQuality = 0.9f // Set to highest quality (no lossy compression)
//
//    writer.write(null, javax.imageio.IIOImage(image, null, null), param)
//    output.flush()
//    writer.dispose()
//}
