package kekmech.ru.feature_app_update_impl.presentation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kekmech.ru.ext_android.addSystemVerticalPadding
import kekmech.ru.ext_android.close
import kekmech.ru.ext_android.notNullSerializableArg
import kekmech.ru.ext_android.openLinkExternal
import kekmech.ru.ext_android.viewbinding.viewBinding
import kekmech.ru.ext_android.withArguments
import kekmech.ru.ext_kotlin.fastLazy
import kekmech.ru.feature_app_update_impl.R
import kekmech.ru.feature_app_update_impl.databinding.FragmentBlockingUpdateBinding
import kekmech.ru.feature_app_update_impl.domain.models.ForceUpdateInfo
import kekmech.ru.res_strings.R.string as Strings

private const val ARG_UPDATE_INFO = "Arg.UpdateInfo"
private const val LINK_GITHUB = "https://github.com/team-ph4r0s/mpei-next/issues"

class BlockingUpdateFragment : Fragment(R.layout.fragment_blocking_update) {

    private val viewBinding by viewBinding(FragmentBlockingUpdateBinding::bind)
    private val updateInfo: ForceUpdateInfo by fastLazy { notNullSerializableArg(ARG_UPDATE_INFO) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(viewBinding) {
            buttonExit.setOnClickListener {
                close()
            }
            buttonUpdateNow.setOnClickListener {
                requireContext().openLinkExternal(updateInfo.updateUrl)
            }
            header.setText(Strings.blocking_update_title)
            fullText.setText(Strings.blocking_update_description)
            link.text = LINK_GITHUB
            link.setOnClickListener {
                requireContext().openLinkExternal(LINK_GITHUB)
            }
        }
        view.addSystemVerticalPadding()
    }

    companion object {

        fun newInstance(forceUpdateInfo: ForceUpdateInfo) =
            BlockingUpdateFragment().withArguments(ARG_UPDATE_INFO to forceUpdateInfo)
    }
}
