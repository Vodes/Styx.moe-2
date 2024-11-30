package moe.styx.web.views

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setClassNames2
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.AnchorTarget
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.StreamResource
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.theme.lumo.LumoUtility.*
import moe.styx.common.config.UnifiedConfig
import moe.styx.common.data.User
import moe.styx.web.checkAuth
import moe.styx.web.components.authProgress
import moe.styx.web.components.linkButton
import moe.styx.web.components.noAccess
import moe.styx.web.components.user.deviceListView
import moe.styx.web.createComponent
import moe.styx.web.layout.MainLayout
import moe.styx.web.unorderedList
import org.vaadin.lineawesome.LineAwesomeIcon
import java.io.File

@PageTitle("Styx - User")
@Route("user", layout = MainLayout::class)
class UserView : KComposite() {

    val root = ui {
        verticalLayout(false) {
            setClassNames2(Margin.NONE, Padding.NONE)
            authProgress()
        }.also { layout ->
            checkAuth(UI.getCurrent(), VaadinRequest.getCurrent(), 0, layout, notLoggedIn = { noAccess() }) {
                init(userSettings(it))
            }
        }
    }

    private fun userSettings(user: User) = createComponent {
        verticalLayout(false) {
            setSizeFull()
            h2("Welcome, ${user.name}!") { addClassNames(Padding.XSMALL) }
            accordion {
                setWidthFull()
                panel("Downloads") {
                    isOpened = true
                    setClassNames2(Padding.XSMALL)
                    content {
                        add(desktopDownloadButtons())
                        add(androidDownloadButtons())
                    }
                }
                panel("Devices") {
                    setClassNames2(Padding.XSMALL)
                    content { deviceListView(user) }
                }
            }
            linkButton(
                "${UnifiedConfig.current.base.apiBaseURL()}/discord/logout",
                "Logout",
                LineAwesomeIcon.USER_ALT_SLASH_SOLID.create(),
                target = AnchorTarget.DEFAULT
            )
        }
    }

    private fun desktopDownloadButtons() = createComponent {
        horizontalLayout {
            setClassNames2(Padding.Horizontal.SMALL)
            val buildDir = UnifiedConfig.current.base.buildDir()
            if (!File(buildDir).exists() || File(buildDir).listFiles().isNullOrEmpty()) {
                h3("Could not find builds on the server.")
                return@horizontalLayout
            }
            val latest = File(buildDir).listFiles()!!.filter { it.isDirectory }.maxBy { it.name }
            val winMsi = latest.walkTopDown().find { it.name.endsWith(".msi") }
            val linuxDeb = latest.walkTopDown().find { it.name.endsWith(".deb") }
            val linuxRpm = latest.walkTopDown().find { it.name.endsWith(".rpm") }
            val linuxJar = latest.walkTopDown().find { it.name.contains("linux", true) && it.name.endsWith(".jar") }
            verticalLayout {
                if (linuxJar != null || linuxDeb != null || linuxRpm != null) {
                    h3("Linux")
                    unorderedList {
                        addClassNames(Display.FLEX, Gap.SMALL, ListStyleType.NONE, Margin.NONE, Padding.NONE)
                        if (linuxDeb != null) {
                            linkButton("", "DEB", LineAwesomeIcon.UBUNTU.create()) {
                                element.setAttribute("download", true)
                                setHref(linuxDeb.streamResource())
                            }
                        }
                        if (linuxRpm != null) {
                            linkButton("", "RPM", LineAwesomeIcon.FEDORA.create()) {
                                element.setAttribute("download", true)
                                setHref(linuxRpm.streamResource())
                            }
                        }
                        if (linuxJar != null) {
                            linkButton("", "Jar", LineAwesomeIcon.JAVA.create()) {
                                element.setAttribute("download", true)
                                setHref(linuxJar.streamResource())
                            }
                        }
                    }
                }
                if (winMsi != null) {
                    h3("Windows")
                    linkButton("", "Windows Installer", LineAwesomeIcon.WINDOWS.create()) {
                        element.setAttribute("download", true)
                        setHref(winMsi.streamResource())
                    }
                }
            }
        }
    }

    private fun androidDownloadButtons() = createComponent {
        horizontalLayout {
            setClassNames2(Padding.Horizontal.SMALL)
            val androidBuildDir = UnifiedConfig.current.base.androidBuildDir()
            if (!File(androidBuildDir).exists() || File(androidBuildDir).listFiles().isNullOrEmpty()) {
                h3("Could not find builds on the server.")
                return@horizontalLayout
            }
            val latest = File(androidBuildDir).listFiles()!!.filter { it.isDirectory }.maxBy { it.name }
            val universalAPK = latest.walkTopDown().find { it.name.endsWith("-universal-release.apk") }
            val arm64APK = latest.walkTopDown().find { it.name.endsWith("-arm64-v8a-release.apk") }
            verticalLayout {
                h3("Android")
                unorderedList {
                    addClassNames(Display.FLEX, Gap.SMALL, ListStyleType.NONE, Margin.NONE, Padding.NONE)
                    if (universalAPK != null) {
                        linkButton("", "Universal APK", LineAwesomeIcon.ANDROID.create()) {
                            element.setAttribute("download", true)
                            setHref(universalAPK.streamResource())
                        }
                    }
                    if (arm64APK != null) {
                        linkButton("", "ARM-64 APK") {
                            element.setAttribute("download", true)
                            setHref(arm64APK.streamResource())
                        }
                    }
                }
                htmlSpan("If you don't know what to pick here or don't care about filesize just go with <b>Universal</b>.")
                htmlSpan("Chances are that if you have a modern device <b>ARM-64</b> will be what you want.")
                htmlSpan("ARMv7 and X86 Builds are not available anymore because the demand for that is quite low and <b>Universal</b> will still work.")
            }
        }
    }

    private fun File.streamResource(): StreamResource {
        return StreamResource(this.name, this::inputStream)
    }
}