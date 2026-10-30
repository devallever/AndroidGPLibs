package app.allever.android.lib.billing

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.allever.android.lib.billing.databinding.ActivityBillingBinding
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 订阅购买页面
 */
class BillingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBillingBinding
    private var planAdapter: PlanAdapter? = null

    private var isLoading = false
    private var isSubscribed = false

    // 当前选中的 plan，用于订阅按钮文案
    private var currentPriceText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadPlans()
    }

    private fun setupToolbar() {
        // 沉浸式 + 深色状态栏
        window.statusBarColor = getColor(R.color.billing_bg)
    }

    private fun setupRecyclerView() {
        binding.rvPlans.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener { finish() }

        binding.btnSubscribe.setOnClickListener {
            if (isLoading || isSubscribed) return@setOnClickListener
            val selected = planAdapter?.getSelectedPlan()
            if (selected == null) {
                Toast.makeText(this, "Please select a plan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            launchPurchase(selected)
        }

        binding.tvPrivacy.setOnClickListener {
            Toast.makeText(this, getString(R.string.billing_privacy_click), Toast.LENGTH_SHORT).show()
        }

        binding.tvTerms.setOnClickListener {
            Toast.makeText(this, getString(R.string.billing_terms_click), Toast.LENGTH_SHORT).show()
        }

        binding.tvRestore.setOnClickListener {
            restorePurchases()
        }
    }

    // ==================== 加载 Plan 列表 ====================

    private fun loadPlans() {
        showLoading(true)

        lifecycleScope.launch {
            // 确保 BillingManager 已初始化
            if (!BillingHelper.isConnected) {
                BillingManager.init(this@BillingActivity)
                // 等连接 + 查询完成（BillingManager.init 是异步的，这里简单延时等一下）
                kotlinx.coroutines.delay(1500)
            }

            // 检查当前订阅状态
            val hasActive = BillingManager.hasActiveSubscription()
            isSubscribed = hasActive

            // 从缓存获取 plan 列表
            val cached = BillingManager.getAllCachedProductDetails()

            if (cached.isNotEmpty()) {
                showPlans(cached)
            } else {
                // 缓存为空，主动查询一次
                val result = BillingHelper.querySubDetails(BillingManager.ALL_SUBS_SKUS)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val list = result.getOrNull() ?: emptyList()
                        if (list.isNotEmpty()) {
                            showPlans(list)
                        } else {
                            showRegionNotSupported()
                        }
                    } else {
                        showRegionNotSupported()
                    }
                }
            }

            // 如果已有订阅，更新按钮状态
            if (isSubscribed) {
                updateSubscribedState()
            }
        }
    }

    private fun showPlans(plans: List<ProductDetails>) {
        showLoading(false)
        binding.llLoading.visibility = View.GONE
        binding.tvNotSupported.visibility = View.GONE
        binding.rvPlans.visibility = View.VISIBLE

        planAdapter = PlanAdapter(plans) { details, _ ->
            currentPriceText = details.extractPrice()
            updateSubscribeButton()
        }
        binding.rvPlans.adapter = planAdapter

        // 默认第一个
        if (plans.isNotEmpty()) {
            currentPriceText = plans[0].extractPrice()
            updateSubscribeButton()
        }
    }

    private fun showRegionNotSupported() {
        showLoading(false)
        binding.rvPlans.visibility = View.GONE
        binding.tvNotSupported.visibility = View.VISIBLE
        binding.btnSubscribe.isEnabled = false
        binding.tvBtnText.text = getString(R.string.billing_region_not_supported)
    }

    private fun showLoading(show: Boolean) {
        isLoading = show
        binding.llLoading.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvPlans.visibility = if (show) View.GONE else View.VISIBLE
        binding.tvNotSupported.visibility = View.GONE
        binding.btnSubscribe.isEnabled = !show
    }

    // ==================== 订阅按钮状态 ====================

    private fun updateSubscribeButton() {
        if (isSubscribed) {
            updateSubscribedState()
        } else {
            binding.btnSubscribe.setBackgroundResource(R.drawable.bg_btn_subscribe)
            binding.tvBtnText.setTextColor(getColor(R.color.billing_btn_text))
            binding.tvBtnText.text = getString(R.string.billing_subscribe_format, currentPriceText)
            binding.progressBar.visibility = View.GONE
            binding.btnSubscribe.isEnabled = true
        }
    }

    private fun showButtonLoading(show: Boolean) {
        isLoading = show
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvBtnText.visibility = if (show) View.GONE else View.VISIBLE
        binding.btnSubscribe.isEnabled = !show
    }

    private fun updateSubscribedState() {
        isSubscribed = true
        binding.btnSubscribe.setBackgroundResource(R.drawable.bg_btn_subscribed)
        binding.tvBtnText.setTextColor(getColor(R.color.billing_btn_disabled_text))
        binding.tvBtnText.text = getString(R.string.billing_subscribed)
        binding.progressBar.visibility = View.GONE
        binding.tvBtnText.visibility = View.VISIBLE
        binding.btnSubscribe.isEnabled = false
    }

    // ==================== 发起购买 ====================

    private fun launchPurchase(productDetails: ProductDetails) {
        showButtonLoading(true)

        // 设置一次性监听
        var handled = false
        val listener: (PurchaseResult) -> Unit = listener@{ result ->
            if (handled) return@listener
            handled = true

            lifecycleScope.launch {
                when (result) {
                    is PurchaseResult.Success -> {
                        // 等待 ack 完成后刷新
                        kotlinx.coroutines.delay(500)
                        isSubscribed = true
                        updateSubscribedState()
                        Toast.makeText(
                            this@BillingActivity,
                            "Subscription activated!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is PurchaseResult.Error -> {
                        showButtonLoading(false)
                        Toast.makeText(
                            this@BillingActivity,
                            "Purchase failed: ${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    PurchaseResult.Canceled -> {
                        showButtonLoading(false)
                        Toast.makeText(
                            this@BillingActivity,
                            "Purchase canceled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        BillingManager.setGlobalPurchaseListener(listener)

        // 发起购买
        BillingHelper.launchSubscriptionPurchase(this, productDetails)

        // 2 分钟超时保护
        lifecycleScope.launch {
            kotlinx.coroutines.delay(120_000)
            if (!handled) {
                handled = true
                withContext(Dispatchers.Main) {
                    showButtonLoading(false)
                    BillingManager.setGlobalPurchaseListener(null)
                }
            }
        }
    }

    // ==================== 恢复购买 ====================

    private fun restorePurchases() {
        lifecycleScope.launch {
            val result = BillingManager.restorePurchases()
            withContext(Dispatchers.Main) {
                if (result) {
                    isSubscribed = true
                    updateSubscribedState()
                    Toast.makeText(
                        this@BillingActivity,
                        getString(R.string.billing_restore_success),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@BillingActivity,
                        getString(R.string.billing_restore_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        BillingManager.setGlobalPurchaseListener(null)
        super.onDestroy()
    }

    private fun ProductDetails.extractPrice(): String {
        return subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
            ?: oneTimePurchaseOfferDetails?.formattedPrice
            ?: ""
    }
}
