package kekmech.ru.feature_app_settings_impl.presentation.screens.main.elm

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kekmech.ru.feature_app_settings_impl.presentation.screens.main.elm.AppSettingsEvent.Internal
import kekmech.ru.feature_app_settings_impl.presentation.screens.main.elm.AppSettingsEvent.Ui
import kekmech.ru.lib_app_info.AppVersionName

internal class AppSettingsReducerTest : BehaviorSpec({
    val reducer = AppSettingsReducer()
    val givenState = AppSettingsState(
        isFeatureToggleSnowFlakesEnabled = false,
        appVersionName = AppVersionName("v0.5.6"),
    )

    Given("The screen has just been opened") {
        Then("Nobody is signed in to BARS") {
            givenState.isBarsSignedIn.shouldBe(false)
        }
        When("Ui.Init") {
            val (state, effects, commands) = reducer.reduce(Ui.Init, givenState)
            Then("The settings and the BARS sign-in are subscribed") {
                state.shouldBe(givenState)
                effects.shouldBeEmpty()
                commands.shouldContainExactly(
                    AppSettingsCommand.LoadAppSettings,
                    AppSettingsCommand.ObserveBarsSignIn,
                )
            }
        }
    }

    Given("The BARS account is signed in") {
        val signedIn = reducer.reduce(Internal.BarsSignInChanged(isSignedIn = true), givenState).state
        Then("The BARS section is shown") {
            signedIn.isBarsSignedIn.shouldBe(true)
        }
        When("Ui.Action.LogoutBars") {
            val (state, effects, commands) = reducer.reduce(Ui.Action.LogoutBars, signedIn)
            Then("Only the logout command is sent, the section stays until the sign-out has happened") {
                state.shouldBe(signedIn)
                effects.shouldBeEmpty()
                commands.shouldContainExactly(AppSettingsCommand.LogoutBars)
            }
        }
        When("The account has gone") {
            val (state, _, commands) = reducer.reduce(Internal.BarsSignInChanged(isSignedIn = false), signedIn)
            Then("The BARS section is hidden") {
                state.isBarsSignedIn.shouldBe(false)
                commands.shouldBeEmpty()
            }
        }
    }
})
