package kekmech.ru.feature_app_update_impl.presentation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

internal class AppMarketTest : BehaviorSpec({
    val appUri = "market://details?id=ru.mpei.hub"

    Given("The app was installed from a market") {
        When("The targets are built") {
            val targets = marketTargets("ru.mpei.hub", installer = "ru.vk.store", fallbackUrl = "")
            Then("The market that installed it goes first, then any handler of the standard link") {
                targets.shouldContainExactly(
                    MarketTarget(appUri, packageName = "ru.vk.store"),
                    MarketTarget(appUri),
                )
            }
        }
    }

    Given("The app was installed from a file") {
        When("The targets are built") {
            val targets = marketTargets("ru.mpei.hub", installer = null, fallbackUrl = "")
            Then("Only the standard link is left") {
                targets.shouldContainExactly(MarketTarget(appUri))
            }
        }
        When("The installer is blank") {
            marketTargets("ru.mpei.hub", installer = " ", fallbackUrl = "").shouldContainExactly(MarketTarget(appUri))
        }
    }

    Given("The settings of the app give a link") {
        When("The targets are built") {
            val targets = marketTargets("ru.mpei.hub", installer = "com.android.vending", fallbackUrl = " https://example.org/app ")
            Then("The link is the last resort") {
                targets.last().shouldBe(MarketTarget("https://example.org/app"))
                targets.size.shouldBe(3)
            }
        }
    }

    Given("The package of the debug build") {
        Then("The page is looked for by the package of the running app") {
            marketTargets("ru.mpei.hub.dev", installer = null, fallbackUrl = "").single().uri
                .shouldBe("market://details?id=ru.mpei.hub.dev")
        }
    }
})
