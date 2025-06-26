package com.allever.android.lib.admob

import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.allever.android.lib.admob.databinding.ActivityAdBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AdActivity: AppCompatActivity() {
    private lateinit var mBinding: ActivityAdBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityAdBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        //Navigation Button (Back Button)
        mBinding.ivBack.setOnClickListener {
            finish()
        }

        init()
    }

    private var interClickCount = 0
    private var interShowCount = 0
    private var interFailCount = 0
    private var nativeShowCount = 0
    private var nativeClickCount = 0
    private var nativeFailCount = 0
    private var rewardClickCount = 0
    private var rewardShowCount = 0
    private var rewardFailCount = 0
    private var inBackGround = false
    private var interJob: Job? = null
    private var nativeJob: Job? = null
    private var rewardJob: Job? = null
    private var executeOnStop = false

    private var dismissDelay = 4
    private var nextLoadDelay = 15

    fun init() {

        mBinding.apply {
            dismissProgress.progress = dismissDelay
            tvDismissDelay.text = "dismiss delay: $dismissDelay"
            dismissProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    dismissDelay = progress
                    tvDismissDelay.text = "dismiss delay: $dismissDelay"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })

            nextLoadProgress.progress = nextLoadDelay
            tvNextLoadDelay.text = "next load delay: $nextLoadDelay"
            nextLoadProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    nextLoadDelay = progress
                    tvNextLoadDelay.text = "next load delay: $nextLoadDelay"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                }
            })
        }

        mBinding.btnLoadInter.setOnClickListener {
            loadDevInter()
            mBinding.btnLoadInter.isEnabled = false
            mBinding.btnLoadReward.isEnabled = false
        }

        mBinding.btnLoadReward.setOnClickListener {
            loadDevReward()
            mBinding.btnLoadInter.isEnabled = false
            mBinding.btnLoadReward.isEnabled = false
        }

        loadDevNative()
    }

    private fun loadDevInter() {
        mBinding.apply {
            interJob = lifecycleScope.launch {
                while (true) {
                    if (inBackGround) {
                        delay(5000)
                        log("already show inter ad")
                        continue
                    }
                    AdDevManager.loadDevInter(this@AdActivity, object : AdCallback {
                        override fun onAdFailLoad() {
                            interFailCount++
                            if (interFailCount >= 20) {
                                interJob?.cancel()
                                return
                            }
                        }
                        override fun onAdShow() {
                            interShowCount++
                            tvInterShowCount.text = "inter show count: $interShowCount"
                            launch {
                                delay(dismissDelay * 1000L)
                                AdDevManager.adActivity?.finish()
                                log("auto dismiss inter ad")
                            }
                        }

                        override fun onAdClick() {
                            interClickCount++
                            tvInterClickCount.text = "inter click count: $interClickCount"
                        }
                    })
                    delay(nextLoadDelay * 1000L)
                }
            }
        }
    }

    private fun loadDevReward() {
        mBinding.apply {
            lifecycleScope.launch {
                while (true) {
                    if (inBackGround) {
                        delay(5000)
                        log("already show reward ad")
                        continue
                    }
                    AdDevManager.loadDevReward(this@AdActivity, object : AdCallback {
                        override fun onAdFailLoad() {
                            rewardFailCount++
                            if (rewardFailCount >= 20) {
                                rewardJob?.cancel()
                                return
                            }
                        }
                        override fun onAdShow() {
                            rewardShowCount++
                            tvRewardShowCount.text = "reward show count: $rewardShowCount"
                            launch {
                                delay(dismissDelay * 1000L)
                                AdDevManager.adActivity?.finish()
                                log("auto dismiss reward ad")
//                                delay(5000)
//                                if (executeOnStop) {
//
//                                } else {
//                                    if (inBackGround) {
//                                        requireActivity().onBackPressed()
//                                    }
//                                }
                            }
                        }

                        override fun onAdClick() {
                            rewardClickCount++
                            tvRewardClickCount.text = "reward click count: $rewardClickCount"
                        }
                    })
                    delay(nextLoadDelay * 1000L)
                }
            }
        }
    }


    private fun loadDevNative() {
        mBinding.apply {
            lifecycleScope.launch {
                while (true) {
                    if (inBackGround) {
                        delay(5000)
                        log("already show native ad")
                        continue
                    }
                    AdDevManager.loadDevNative(nativeContainer, adCallback = object : AdCallback {
                        override fun onAdFailLoad() {
                            nativeFailCount++
                            if (nativeFailCount >= 20) {
                                nativeJob?.cancel()
                                return
                            }
                        }
                        override fun onAdShow() {
                            nativeShowCount++
                            tvBannerShowCount.text = "native show count: $nativeShowCount"
                            launch {
                                delay(15 * 1000)
                                AdDevManager.destroyDevNative(nativeContainer)
                                log("auto dismiss native ad")
                            }
                        }

                        override fun onAdClick() {
                            nativeClickCount++
                            tvBannerClickCount.text = "native click count: $nativeClickCount"
                        }
                    })
                    delay(30 * 1000)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        inBackGround = false
        executeOnStop = false
    }


    override fun onPause() {
        super.onPause()
        inBackGround = true
    }

    override fun onStop() {
        super.onStop()
        executeOnStop = true
    }

    override fun onBackPressed() {

    }

    private fun log(msg: String) {

        Log.d("AdActivity", msg)
    }

}