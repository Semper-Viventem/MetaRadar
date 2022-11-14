package f.cking.software.common.navigation

class NavRouter {

    private var navigator: Navigator? = null

    fun attachNavigator(navigator: Navigator) {
        this.navigator = navigator
    }

    fun detachNavigator() {
        navigator = null
    }

    fun navigate(command: NavigationCommand) {
        navigator?.handle(command)
    }
}