package f.cking.software.utils.navigation

class RouterImpl : Router {

    private var navigator: Navigator? = null

    fun attachNavigator(navigator: Navigator) {
        this.navigator = navigator
    }

    fun detachNavigator() {
        navigator = null
    }

    override fun navigate(command: NavigationCommand) {
        navigator?.handle(command)
    }
}