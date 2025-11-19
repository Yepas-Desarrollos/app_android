package mx.checklist.ui.vm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mx.checklist.data.Repo
import mx.checklist.data.auth.Authenticated
import mx.checklist.data.api.dto.LoginReq
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthViewModelTest {

    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: Repo
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        repo = mockk()
        viewModel = AuthViewModel(repo)
    }

    @Test
    fun testLoginSuccess() = runTest(testDispatcher) {
        // Arrange
        val mockAuth = Authenticated(
            token = "fake_jwt_token_12345",
            roleCode = "USER",
            userId = 1L,
            email = "test@example.com",
            fullName = "Test User"
        )

        coEvery { repo.login("test@example.com", "password123") } returns mockAuth

        // Act
        viewModel.login("test@example.com", "password123") {}
        advanceUntilIdle()

        // Assert
        assertEquals(null, viewModel.error.value)
        assertEquals(false, viewModel.loading.value)
    }

    @Test
    fun testLoginFailure() = runTest(testDispatcher) {
        // Arrange
        val errorMessage = "Invalid credentials"
        coEvery { repo.login("test@example.com", "wrongpassword") } throws
            Exception(errorMessage)

        // Act
        viewModel.login("test@example.com", "wrongpassword") {}
        advanceUntilIdle()

        // Assert
        assertNotNull(viewModel.error.value)
        assertEquals(false, viewModel.loading.value)
    }

    @Test
    fun testLogout() = runTest(testDispatcher) {
        // Arrange
        coEvery { repo.logout() } returns Unit

        // Act
        viewModel.logout()
        advanceUntilIdle()

        // Assert
        assertEquals(null, viewModel.error.value)
        verify { repo.logout() }
    }

    @Test
    fun testLoadingStateChanges() = runTest(testDispatcher) {
        // Arrange
        val mockAuth = Authenticated(
            token = "fake_token",
            roleCode = "USER",
            userId = 1L,
            email = "test@example.com",
            fullName = "Test"
        )

        coEvery { repo.login("test@example.com", "password") } returns mockAuth

        // Act & Assert
        assertEquals(false, viewModel.loading.value)

        viewModel.login("test@example.com", "password") {}
        advanceUntilIdle()

        assertEquals(false, viewModel.loading.value)
    }

    @Test
    fun testClearError() {
        // Arrange - simular que hay un error
        viewModel.updateError("Test error")
        assertEquals("Test error", viewModel.error.value)

        // Act
        viewModel.clearError()

        // Assert
        assertEquals(null, viewModel.error.value)
    }
}

