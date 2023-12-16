package moe.styx.web.views.sub

import com.github.mvysny.karibudsl.v10.KComposite
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.OptionalParameter
import com.vaadin.flow.router.Route

@Route("download")
class DownloadableView : KComposite(), HasUrlParameter<String> {
    override fun setParameter(event: BeforeEvent?, @OptionalParameter parameter: String?) {
        TODO("Not yet implemented")
    }
}