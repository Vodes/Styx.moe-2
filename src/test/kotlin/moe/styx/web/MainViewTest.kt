package moe.styx.web

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10.Routes

/**
 * Tests the UI. Uses the Browserless testing approach as provided by the
 * [Karibu Testing](https://github.com/mvysny/karibu-testing) library.
 */
class MainViewTest : DynaTest({
    lateinit var routes: Routes
    beforeGroup {
        // Route discovery involves classpath scanning and is an expensive operation.
        // Running the discovery process only once per test run speeds up the test runtime considerably.
        // Discover the routes once and cache the result.
        routes = Routes().autoDiscoverViews("moe.styx.web")
    }
    beforeEach {
        // MockVaadin.setup() registers all @Routes, prepares the Vaadin instances for us
        // (the UI, the VaadinSession, VaadinRequest, VaadinResponse, ...) and navigates to the root route.
        MockVaadin.setup(routes)
    }
    afterEach { MockVaadin.tearDown() }
})
