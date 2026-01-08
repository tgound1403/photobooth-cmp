package com.example.cameraxapp.ui.state

/**
 * Common UI state sealed class for handling loading, success, and error states
 * @param T The type of data in success state
 */
sealed class UiState<out T> {
    /**
     * Loading state - indicates that data is being fetched or processed
     */
    object Loading : UiState<Nothing>()
    
    /**
     * Success state - contains the successful result data
     * @param data The successful result data
     */
    data class Success<T>(val data: T) : UiState<T>()
    
    /**
     * Error state - contains error message
     * @param message Error message to display
     * @param throwable Optional throwable for detailed error information
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UiState<Nothing>()
    
    /**
     * Empty state - indicates no data available
     */
    object Empty : UiState<Nothing>()
    
    /**
     * Check if state is loading
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Check if state is success
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Check if state is error
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Check if state is empty
     */
    val isEmpty: Boolean
        get() = this is Empty
    
    /**
     * Get data if state is success, null otherwise
     */
    fun getDataOrNull(): T? {
        return if (this is Success) data else null
    }
}
