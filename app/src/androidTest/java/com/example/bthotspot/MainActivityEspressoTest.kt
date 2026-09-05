package com.example.bthotspot

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityEspressoTest {

    @Test
    fun automationToggleIsDisplayedOnLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.automationToggle)).check { view, noMatchException ->
                if (noMatchException != null) throw noMatchException
                check(isDisplayed().matches(view))
            }
        }
    }
}