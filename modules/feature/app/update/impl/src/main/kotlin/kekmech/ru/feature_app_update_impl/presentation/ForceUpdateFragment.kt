package kekmech.ru.feature_app_update_impl.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import kekmech.ru.ext_android.close
import kekmech.ru.ext_android.fragment.BottomSheetDialogFragment
import kekmech.ru.ext_android.notNullSerializableArg
import kekmech.ru.ext_android.viewbinding.viewBinding
import kekmech.ru.ext_android.withArguments
import kekmech.ru.ext_kotlin.fastLazy
import kekmech.ru.feature_app_update_impl.R
import kekmech.ru.feature_app_update_impl.databinding.FragmentForceUpdateBinding
import kekmech.ru.feature_app_update_impl.domain.models.ForceUpdateInfo

private const val ARG_INFO = "Arg.Info"

internal class ForceUpdateFragment : BottomSheetDialogFragment(R.layout.fragment_force_update) {

    private val viewBinding by viewBinding(FragmentForceUpdateBinding::bind)
    private val forceUpdateInfo by fastLazy { notNullSerializableArg<ForceUpdateInfo>(ARG_INFO) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewBinding.apply {
            buttonUpdateNow.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(forceUpdateInfo.updateUrl)))
            }
            buttonUpdateLater.setOnClickListener {
                close()
            }
            textDescription.text = forceUpdateInfo.shortDescription
        }
    }

    companion object {

        fun newInstance(forceUpdateInfo: ForceUpdateInfo) = ForceUpdateFragment()
            .withArguments(ARG_INFO to forceUpdateInfo)
    }
}
