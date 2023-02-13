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

package io.telereso.kmp.core


import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

class InMemorySettingTest {

    private lateinit var settings: InMemorySetting

    @BeforeTest
    fun before() {
        settings = Settings.getInMemory() as InMemorySetting
    }

    @Test
    fun shouldClearAllSettings() {
        settings.putString("STRINGKEY", "CLEARME")
        settings.putInt("INTKEY", 8)
        settings.clear()
        settings.get<String>("STRINGKEY").shouldBeNull()
        settings.get<Int>("STRINGKEY").shouldBeNull()
    }

    @Test
    fun shouldReturnCorrectSizeSettings() {
        settings.putString("STRINGKEY", "CLEARME")
        settings.putInt("INTKEY", 8)
        settings.size.shouldBe(2)
    }

    @Test
    fun shouldReturnCorrectKeysSettings() {
        settings.putString("STRINGKEY", "CLEARME")
        settings.putInt("INTKEY", 8)
        settings.keys.also {
            it.shouldContain("INTKEY")
            it.shouldContain("STRINGKEY")
        }
    }

    @Test
    fun shouldRemoveCorrectValue() {
        settings.putString("STRINGKEY", "CLEARME")
        settings.putInt("INTKEY", 8)
        settings.remove("STRINGKEY")
        settings.get<String>("STRINGKEY").shouldBeNull()
    }

    @Test
    fun shouldPutEnGetString() {
        settings.putString("STRINGKEY", "SOMESTRING")
        settings.getString("STRINGKEY","DEFAULT").shouldBe("SOMESTRING")
        settings.getStringOrNull("4543354FF").shouldBeNull()

        // should get default value
        settings.getString("STRINGDEF","DEFAULT").shouldBe("DEFAULT")
    }

    @Test
    fun shouldPutEnGetInt() {
        settings.putInt("INTKEY", 666)
        settings.getInt("INTKEY",0).shouldBe(666)
        settings.getIntOrNull("FFJJGf").shouldBeNull()

        // should get default value
        settings.getInt("INTDEF",0).shouldBe(0)
    }

    @Test
    fun shouldPutEnGetLong() {
        settings.putLong("LONGKEY", 99L)
        settings.getLong("LONGKEY",0L).shouldBe(99L)
        settings.getLongOrNull("UT5554").shouldBeNull()

        // should get default value
        settings.getLong("INTDEF",0L).shouldBe(0L)
    }

    @Test
    fun shouldPutEnGetFloat() {
        settings.putFloat("FLOATKEY", 60F)
        settings.getFloat("FLOATKEY",0F).shouldBe(60F)
        settings.getFloatOrNull("TOEOEOE").shouldBeNull()

        // should get default value
        settings.getFloat("INTDEF",0F).shouldBe(0F)
    }

    @Test
    fun shouldPutEnGetDouble() {
        settings.putDouble("DOUBLEKEY", 100000.99)
        settings.getDouble("DOUBLEKEY",0.0).shouldBe(100000.99)
        settings.getDoubleOrNull("DFFDS").shouldBeNull()

        // should get default value
        settings.getDouble("INTDEF",100.0).shouldBe(100.0)
    }

    @Test
    fun shouldPutEnGetBoolean() {
        settings.putBoolean("BOOLEANKEY", false)
        settings.getBoolean("BOOLEANKEY",false).shouldBeFalse()
        settings.putBoolean("BOOLEANKEY", true)
        settings.getBoolean("BOOLEANKEY",false).shouldBeTrue()
        settings.getBooleanOrNull("FNFNFNF").shouldBeNull()

        // should get default value
        settings.getBoolean("INTDEF",true).shouldBeTrue()
    }

    @Test
    fun hasKeyShouldReturn() {
        settings.hasKey("LONGKEY").shouldBeFalse()
        settings.putBoolean("BAKABOOL", true)
        settings.hasKey("BAKABOOL").shouldBeTrue()
    }
}