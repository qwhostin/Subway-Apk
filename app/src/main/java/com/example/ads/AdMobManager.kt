package com.example.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

data class AdConsentState(
    val initialized: Boolean = false,
    val canRequestAds: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val rewardedReady: Boolean = false,
    val interstitialReady: Boolean = false,
    val statusNote: String = "Checking privacy preferences..."
)

/**
 * Google AdMob & User Messaging Platform (UMP) manager for RUNVANTA.
 * - Uses Google's official test ad unit IDs configured in strings.xml.
 * - Never initializes ads before UMP consent allows it.
 * - Never interrupts active gameplay or grants uncompleted rewards.
 * - Provides graceful offline/unavailable fallbacks so gameplay is never blocked.
 */
class AdMobManager(private val context: Context) {

    private val isMobileAdsInitialized = AtomicBoolean(false)
    private var consentInformation: ConsentInformation? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private val _adState = MutableStateFlow(AdConsentState())
    val adState: StateFlow<AdConsentState> = _adState.asStateFlow()

    fun requestConsentAndInitialize(activity: Activity) {
        val playServicesStatus = runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        }.getOrDefault(ConnectionResult.SERVICE_MISSING)

        if (playServicesStatus != ConnectionResult.SUCCESS) {
            _adState.value = _adState.value.copy(
                canRequestAds = false,
                statusNote = "Google Play Services unavailable (ad-free fallback active)"
            )
            return
        }

        runCatching {
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            val info = UserMessagingPlatform.getConsentInformation(activity)
            consentInformation = info

            info.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                        val privacyReq =
                            info.privacyOptionsRequirementStatus ==
                                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                        val canRequest = info.canRequestAds()
                        _adState.value = _adState.value.copy(
                            canRequestAds = canRequest,
                            privacyOptionsRequired = privacyReq,
                            statusNote = if (canRequest) "Ad consent configured" else "Ad-free fallback active"
                        )
                        if (canRequest) {
                            initializeMobileAds(activity)
                        }
                    }
                },
                { _ ->
                    // Graceful offline/emulator fallback: do not block game
                    val canRequest = info.canRequestAds()
                    _adState.value = _adState.value.copy(
                        canRequestAds = canRequest,
                        statusNote = "Offline / ad service unavailable (ad-free mode)"
                    )
                    if (canRequest) {
                        initializeMobileAds(activity)
                    }
                }
            )
        }.onFailure {
            _adState.value = _adState.value.copy(
                canRequestAds = false,
                statusNote = "Ad-free fallback active"
            )
        }
    }

    fun showPrivacyOptionsForm(activity: Activity) {
        runCatching {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
                val info = consentInformation ?: return@showPrivacyOptionsForm
                val canRequest = info.canRequestAds()
                _adState.value = _adState.value.copy(
                    canRequestAds = canRequest,
                    privacyOptionsRequired = info.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                )
                if (canRequest) initializeMobileAds(activity)
            }
        }
    }

    private fun initializeMobileAds(activity: Activity) {
        if (isMobileAdsInitialized.getAndSet(true)) return
        runCatching {
            val requestConfig = RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
                .build()
            MobileAds.setRequestConfiguration(requestConfig)
            MobileAds.initialize(context) {
                _adState.value = _adState.value.copy(initialized = true)
                preloadInterstitial()
                preloadRewarded()
            }
        }
    }

    fun preloadInterstitial() {
        if (!_adState.value.canRequestAds) return
        val unitId = context.getString(R.string.admob_interstitial_id)
        InterstitialAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _adState.value = _adState.value.copy(interstitialReady = true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _adState.value = _adState.value.copy(interstitialReady = false)
                }
            }
        )
    }

    fun showInterstitialIfReady(activity: Activity?, onFinished: () -> Unit) {
        val ad = interstitialAd
        if (activity == null || ad == null || !_adState.value.canRequestAds) {
            onFinished()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _adState.value = _adState.value.copy(interstitialReady = false)
                preloadInterstitial()
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                _adState.value = _adState.value.copy(interstitialReady = false)
                preloadInterstitial()
                onFinished()
            }
        }
        ad.show(activity)
    }

    fun preloadRewarded() {
        if (!_adState.value.canRequestAds) return
        val unitId = context.getString(R.string.admob_rewarded_id)
        RewardedAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _adState.value = _adState.value.copy(rewardedReady = true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _adState.value = _adState.value.copy(rewardedReady = false)
                }
            }
        )
    }

    /**
     * Grants reward ONLY inside OnUserEarnedRewardListener when the rewarded ad completes.
     */
    fun showRewardedAd(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onUnavailableOrCancelled: (String) -> Unit
    ) {
        val ad = rewardedAd
        if (activity == null || ad == null || !_adState.value.canRequestAds) {
            preloadRewarded()
            onUnavailableOrCancelled("Rewarded ad is currently unavailable. Try again when online.")
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _adState.value = _adState.value.copy(rewardedReady = false)
                preloadRewarded()
                if (!earned) {
                    onUnavailableOrCancelled("Watch the full ad to receive the reward.")
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                _adState.value = _adState.value.copy(rewardedReady = false)
                preloadRewarded()
                onUnavailableOrCancelled("Ad could not be displayed right now.")
            }
        }
        ad.show(activity) { _ ->
            earned = true
            onRewardEarned()
        }
    }
}

@Composable
fun AdMobBannerSlot(
    canRequestAds: Boolean,
    modifier: Modifier = Modifier
) {
    if (!canRequestAds) return

    val context = LocalContext.current
    var adLoaded by remember { mutableStateOf(false) }

    val adView = remember(context) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = context.getString(R.string.admob_banner_id)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adLoaded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    adLoaded = false
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    if (adLoaded) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(52.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(factory = { adView })
        }
    }
}
