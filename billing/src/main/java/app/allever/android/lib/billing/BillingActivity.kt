package app.allever.android.lib.billing

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.allever.android.lib.billing.databinding.ActivityBillingBinding
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupToolbar() {
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
                Toast.makeText(this, getString(R.string.billing_tip_select_plan), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            launchPurchase(selected)
        }

        binding.tvPrivacy.setOnClickListener {
            Toast.makeText(this, getString(R.string.billing_tip_privacy_click), Toast.LENGTH_SHORT).show()
        }

        binding.tvTerms.setOnClickListener {
            Toast.makeText(this, getString(R.string.billing_tip_terms_click), Toast.LENGTH_SHORT).show()
        }

        binding.tvRestore.setOnClickListener {
            restorePurchases()
        }
    }

    // ==================== 加载 Plan 列表 ====================

    private fun loadPlans() {
        showLoading(true)

        lifecycleScope.launch {
            if (!BillingHelper.isConnected) {
                BillingManager.init(this@BillingActivity)
                kotlinx.coroutines.delay(1500)
            }

            // 先刷新订阅状态，拿到 activePurchase
            val hasActive = BillingManager.refreshSubscriptionStatus()
            isSubscribed = hasActive

            if (isSubscribed) {
                // 已订阅 → 显示生效卡片，隐藏 Plans
                withContext(Dispatchers.Main) {
                    showSubscribedCard(BillingManager.getActivePurchase())
                }
            } else {
                // 未订阅 → 显示 Plans
                val cached = BillingManager.getAllCachedProductDetails()
                if (cached.isNotEmpty()) {
                    withContext(Dispatchers.Main) { showPlans(cached) }
                } else {
                    val result = BillingHelper.querySubDetails(BillingManager.ALL_SUBS_SKUS)
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            val list = result.getOrNull() ?: emptyList()
                            if (list.isNotEmpty()) showPlans(list)
                            else showRegionNotSupported()
                        } else {
                            showRegionNotSupported()
                        }
                    }
                }
            }
        }
    }

    // ==================== 已订阅：显示生效卡片 ====================

    private fun showSubscribedCard(purchase: Purchase?) {
        isLoading = false
        isSubscribed = true

        // 隐藏 Plans 相关
        binding.llLoading.visibility = View.GONE
        binding.rvPlans.visibility = View.GONE
        binding.tvNotSupported.visibility = View.GONE
        binding.tvPlansTitle.visibility = View.GONE

        // 显示生效卡片
        binding.llSubscribedCard.visibility = View.VISIBLE

        // 填充数据
        if (purchase != null) {
            val sku = purchase.products.firstOrNull() ?: ""

            // 当前身份（根据 SKU 动态展示）
            binding.tvIdentityValue.text = when {
                sku.contains("weekly", true) -> getString(R.string.billing_identity_weekly)
                sku.contains("monthly", true) -> getString(R.string.billing_identity_monthly)
                sku.contains("yearly", true) -> getString(R.string.billing_identity_yearly)
                else -> getString(R.string.billing_identity_default)
            }

            // 订阅状态
            when {
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED -> {
                    binding.tvSubscriptionStatus.text = getString(R.string.billing_active_subscription_active)
                    binding.tvSubscriptionStatus.setTextColor(getColor(R.color.billing_status_active))
                }
                purchase.purchaseState == Purchase.PurchaseState.PENDING -> {
                    binding.tvSubscriptionStatus.text = getString(R.string.billing_active_subscription_pending)
                    binding.tvSubscriptionStatus.setTextColor(Color.parseColor("#F59E0B")) // amber
                }
                else -> {
                    binding.tvSubscriptionStatus.text = getString(R.string.billing_active_subscription_expired)
                    binding.tvSubscriptionStatus.setTextColor(getColor(R.color.billing_status_expired))
                }
            }

            // 自动续订
            binding.tvRenewStatus.text = if (purchase.isAutoRenewing) {
                getString(R.string.billing_active_renew_on)
            } else {
                getString(R.string.billing_active_renew_off)
            }
        } else {
            // 没有具体 purchase，显示通用生效状态
            binding.tvSubscriptionStatus.text = getString(R.string.billing_active_subscription_active)
            binding.tvSubscriptionStatus.setTextColor(getColor(R.color.billing_status_active))
            binding.tvRenewStatus.text = getString(R.string.billing_active_renew_on)
        }

        // 底部按钮变灰
        updateSubscribedState()
    }

    // ==================== 未订阅：显示 Plans ====================

    private fun showPlans(plans: List<ProductDetails>) {
        isLoading = false
        binding.llLoading.visibility = View.GONE
        binding.tvNotSupported.visibility = View.GONE
        binding.llSubscribedCard.visibility = View.GONE
        binding.tvPlansTitle.visibility = View.VISIBLE
        binding.rvPlans.visibility = View.VISIBLE

        planAdapter = PlanAdapter(plans) { details, _ ->
            currentPriceText = details.extractPrice()
            updateSubscribeButton()
        }
        binding.rvPlans.adapter = planAdapter

        if (plans.isNotEmpty()) {
            currentPriceText = plans[0].extractPrice()
            updateSubscribeButton()
        }
    }

    private fun showRegionNotSupported() {
        isLoading = false
        binding.llLoading.visibility = View.GONE
        binding.rvPlans.visibility = View.GONE
        binding.tvPlansTitle.visibility = View.GONE
        binding.llSubscribedCard.visibility = View.GONE
        binding.tvNotSupported.visibility = View.VISIBLE
        // 按钮变灰不可点击，不写长文案
        binding.btnSubscribe.setBackgroundResource(R.drawable.bg_btn_subscribed)
        binding.tvBtnText.setTextColor(getColor(R.color.billing_btn_disabled_text))
        binding.tvBtnText.text = getString(R.string.billing_subscribe_format, "—")
        binding.progressBar.visibility = View.GONE
        binding.btnSubscribe.isEnabled = false
    }

    private fun showLoading(show: Boolean) {
        isLoading = show
        binding.llLoading.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvPlans.visibility = if (show) View.GONE else View.VISIBLE
        binding.tvPlansTitle.visibility = if (show) View.GONE else View.VISIBLE
        binding.llSubscribedCard.visibility = View.GONE
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

        var handled = false
        val listener: (PurchaseResult) -> Unit = listener@{ result ->
            if (handled) return@listener
            handled = true

            lifecycleScope.launch {
                when (result) {
                    is PurchaseResult.Success -> {
                        kotlinx.coroutines.delay(500)
                        // 刷新 activePurchase
                        BillingManager.refreshSubscriptionStatus()
                        withContext(Dispatchers.Main) {
                            showSubscribedCard(BillingManager.getActivePurchase() ?: result.purchase)
                            Toast.makeText(
                                this@BillingActivity,
                                getString(R.string.billing_tip_subscribe_success),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    is PurchaseResult.Error -> {
                        withContext(Dispatchers.Main) {
                            showButtonLoading(false)
                            Toast.makeText(
                                this@BillingActivity,
                                getString(R.string.billing_tip_subscribe_failed, result.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    PurchaseResult.Canceled -> {
                        withContext(Dispatchers.Main) {
                            showButtonLoading(false)
                            Toast.makeText(
                                this@BillingActivity,
                                getString(R.string.billing_tip_subscribe_canceled),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        BillingManager.setGlobalPurchaseListener(listener)
        BillingHelper.launchSubscriptionPurchase(this, productDetails)

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
                    showSubscribedCard(BillingManager.getActivePurchase())
                    Toast.makeText(
                        this@BillingActivity,
                        getString(R.string.billing_tip_restore_success),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@BillingActivity,
                        getString(R.string.billing_tip_restore_failed),
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
