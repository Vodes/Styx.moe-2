package moe.styx.web.components.user

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.setClassNames2
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.server.StreamResource
import com.vaadin.flow.theme.lumo.LumoUtility.*
import moe.styx.common.config.UnifiedConfig
import moe.styx.web.components.linkButton
import moe.styx.web.unorderedList
import org.vaadin.lineawesome.LineAwesomeIcon
import java.io.File

class UserDesktopDownloadButtons : KComposite() {
    val root = ui {
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
            val linuxTarZst = latest.walkTopDown().find { it.name.endsWith(".pkg.tar.zst") }
            val linuxJar = latest.walkTopDown().find { it.name.contains("linux", true) && it.name.endsWith(".jar") }
            verticalLayout {
                if (linuxJar != null || linuxDeb != null || linuxRpm != null || linuxTarZst != null) {
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
                        if (linuxTarZst != null) {
                            linkButton("", "PKG.TAR.ZST (Arch)", LineAwesomeIcon.LINUX.create()) {
                                element.setAttribute("download", true)
                                setHref(linuxTarZst.streamResource())
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
}

class UserAndroidDownloadButtons : KComposite() {
    val root = ui {
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
                verticalLayout(padding = false, spacing = false) {
                    htmlSpan("If you don't know what to pick here or don't care about filesize just go with <b>Universal</b>.")
                    htmlSpan("Chances are that if you have a modern device <b>ARM-64</b> will be what you want.")
                    htmlSpan("ARMv7 and X86 Builds are not available anymore because the demand for that is quite low and <b>Universal</b> will still work.")
                    htmlSpan("<b>TV-Boxes will need the universal ones!</b>")
                }
            }
        }
    }
}

class UserIosDownloadButtons : KComposite() {
    val root = ui {
        horizontalLayout {
            setClassNames2(Padding.Horizontal.SMALL)
            val androidBuildDir = UnifiedConfig.current.base.androidBuildDir()
            if (!File(androidBuildDir).exists() || File(androidBuildDir).listFiles().isNullOrEmpty()) {
                h3("Could not find builds on the server.")
                return@horizontalLayout
            }
            val latest = File(androidBuildDir).listFiles()!!.filter { it.isDirectory }.maxBy { it.name }
            val ipaFile = latest.walkTopDown().find { it.name.endsWith(".ipa") }
            verticalLayout {
                h3("iOS")
                unorderedList {
                    addClassNames(Display.FLEX, Gap.SMALL, ListStyleType.NONE, Margin.NONE, Padding.NONE)
                    if (ipaFile != null) {
                        linkButton("", "IPA (for sideloading)", LineAwesomeIcon.APPLE.create()) {
                            element.setAttribute("download", true)
                            setHref(ipaFile.streamResource())
                        }
                    }
                }
                verticalLayout(padding = false, spacing = false) {
                    htmlSpan("This is an unsigned ipa file for self-signing and sideloading purposes.")
                    htmlSpan("There are many ways to do so, notably <a href='https://altstore.io'>Altstore</a>, <a href='https://sideloadly.io'>Sideloadly</a> or <a href='https://sidestore.io'>Sidestore</a>.")
                    htmlSpan("I myself tested it out with <b>Sidestore</b> and would also probably recommend that as it only requires a PC at initial setup.")
                    htmlSpan("If you want this to be easier, you can sponsor me the 100€ yearly Apple App Dev license.")
                }
            }
        }
    }
}

private fun File.streamResource(): StreamResource {
    return StreamResource(this.name, this::inputStream)
}

@VaadinDsl
fun (@VaadinDsl HasComponents).userDesktopDownloadButtons(
    block: (@VaadinDsl UserDesktopDownloadButtons).() -> Unit = {}
) = init(
    UserDesktopDownloadButtons(), block
)

@VaadinDsl
fun (@VaadinDsl HasComponents).userAndroidDownloadButtons(
    block: (@VaadinDsl UserAndroidDownloadButtons).() -> Unit = {}
) = init(
    UserAndroidDownloadButtons(), block
)

@VaadinDsl
fun (@VaadinDsl HasComponents).userIOSDownloadButtons(
    block: (@VaadinDsl UserIosDownloadButtons).() -> Unit = {}
) = init(
    UserIosDownloadButtons(), block
)