package com.example.kaishelvesapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.kaishelvesapp.R
import com.example.kaishelvesapp.ui.components.BookCover
import com.example.kaishelvesapp.ui.components.GuestRestrictedAccessNotice
import com.example.kaishelvesapp.ui.theme.KaiShelvesAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CriticalComposeFlowsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun guestRestrictionNotice_showsAccountActionAndHandlesClicks() {
        var dismissCount = 0
        var createAccountCount = 0

        composeRule.setContent {
            KaiShelvesAppTheme {
                GuestRestrictedAccessNotice(
                    onDismiss = { dismissCount += 1 },
                    onCreateAccountAndSync = { createAccountCount += 1 }
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.guest_restricted_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.guest_restricted_body)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.create_account_and_sync))
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.cancel))
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, createAccountCount)
        assertEquals(1, dismissCount)
    }

    @Test
    fun bookCover_withoutImageShowsLocalizedPlaceholder() {
        composeRule.setContent {
            KaiShelvesAppTheme {
                BookCover(imageUrl = "", title = "Libro de prueba")
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.book_cover_placeholder)).assertIsDisplayed()
    }
}
