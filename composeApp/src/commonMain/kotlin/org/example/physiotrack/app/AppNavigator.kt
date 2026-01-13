package org.example.physiotrack.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class AppNavigator(initial: Route = Route.Welcome) {
    private val stack = mutableStateListOf(initial)

    val current: Route
        get() = stack.last()
    fun canPop(): Boolean = stack.size > 1
    fun push(route: Route) {
        stack.add(route)
    }

    fun pop() {
        if (stack.size > 1) stack.removeLast()
    }

    fun replace(route: Route) {
        stack[stack.lastIndex] = route
    }

    fun replaceAll(route: Route) {
        stack.clear()
        stack.add(route)
    }
}

@Composable
fun rememberAppNavigator(): AppNavigator = remember { AppNavigator(Route.Welcome) }
