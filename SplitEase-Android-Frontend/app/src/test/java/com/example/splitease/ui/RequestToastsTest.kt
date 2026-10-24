package com.example.splitease.ui

import com.example.splitease.testutil.Fixtures
import com.example.splitease.testutil.recordToasts
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class RequestToastsTest {

    @Test
    fun successShowsTheMessageAsIs() = runTest {
        val toasts = recordToasts()

        RequestToasts.success("Group \"Trip\" created")

        assertEquals(listOf("Group \"Trip\" created"), toasts)
    }

    @Test
    fun httpFailureShowsTheServersMessage() = runTest {
        val toasts = recordToasts()

        RequestToasts.failure("Couldn't add expense", Fixtures.httpException(400, "Split amounts add up to 90"))

        assertEquals(listOf("Couldn't add expense: Split amounts add up to 90"), toasts)
    }

    @Test
    fun httpFailureWithoutAMessageShowsTheStatusCode() = runTest {
        val toasts = recordToasts()

        RequestToasts.failure("Couldn't delete group", Fixtures.httpException(500))

        assertEquals(listOf("Couldn't delete group: server error (500)"), toasts)
    }

    @Test
    fun networkFailureAsksToCheckTheConnection() = runTest {
        val toasts = recordToasts()

        RequestToasts.failure("Couldn't load groups", IOException("Unable to resolve host"))

        assertEquals(listOf("Couldn't load groups: check your internet connection"), toasts)
    }

    @Test
    fun otherExceptionsShowTheirMessageOrJustTheAction() = runTest {
        val toasts = recordToasts()

        RequestToasts.failure("Couldn't record payment", IllegalStateException("boom"))
        RequestToasts.failure("Couldn't record payment", IllegalStateException())

        assertEquals(listOf("Couldn't record payment: boom", "Couldn't record payment"), toasts)
    }

    @Test
    fun unsuccessfulResponseShowsTheServersMessageOrCode() = runTest {
        val toasts = recordToasts()

        RequestToasts.failure("Couldn't create group", Fixtures.errorResponse<Unit>(400, "User not found: x@gmail.com"))
        RequestToasts.failure("Couldn't create group", Fixtures.errorResponse<Unit>(403))

        assertEquals(
            listOf("Couldn't create group: User not found: x@gmail.com", "Couldn't create group: server error (403)"),
            toasts
        )
    }

    @Test
    fun plainFailureMessage() = runTest {
        val toasts = recordToasts()

        RequestToasts.failure("Login failed: wrong email or password")

        assertEquals(listOf("Login failed: wrong email or password"), toasts)
    }
}
