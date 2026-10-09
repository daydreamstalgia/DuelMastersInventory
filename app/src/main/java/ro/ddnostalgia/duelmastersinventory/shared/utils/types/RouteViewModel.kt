package ro.ddnostalgia.duelmastersinventory.shared.utils.types

import androidx.lifecycle.SavedStateHandle
import ro.ddnostalgia.duelmastersinventory.nav.Routes

open class RouteViewModel<R: Routes>(
    protected val route: R,
    private val savedStateHandle: SavedStateHandle,
): BaseRouteViewModel(savedStateHandle)


open class BaseRouteViewModel(
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel(
) {
    protected fun routeArgInt(name: String): Int
            = checkNotNull(savedStateHandle[name])

    protected fun routeArgIntOrNull(name: String): Int?
            = savedStateHandle[name]

    protected fun routeArgString(name: String): String
            = checkNotNull(savedStateHandle[name])
}