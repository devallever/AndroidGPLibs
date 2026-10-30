package app.allever.android.lib.billing

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.allever.android.lib.billing.databinding.ItemPlanBinding
import com.android.billingclient.api.ProductDetails

/**
 * 订阅 Plan 列表 Adapter（单选）
 */
class PlanAdapter(
    private val plans: List<ProductDetails>,
    private val onItemClick: ((ProductDetails, Int) -> Unit)? = null
) : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {

    private var selectedPosition = 0

    fun getSelectedPlan(): ProductDetails? =
        if (plans.isNotEmpty() && selectedPosition in plans.indices) plans[selectedPosition] else null

    fun setSelected(position: Int) {
        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemPlanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(plans[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = plans.size

    /** 从 ProductDetails 提取订阅价格字符串（兼容 Billing 9.x） */
    private fun ProductDetails.getSubscriptionPrice(): String {
        return subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
            ?: oneTimePurchaseOfferDetails?.formattedPrice
            ?: ""
    }

    inner class PlanViewHolder(
        private val binding: ItemPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(details: ProductDetails, isSelected: Boolean) {
            val context = binding.root.context

            // 标题
            binding.tvPlanTitle.text = details.title

            // 价格
            val priceText = details.getSubscriptionPrice()
            val periodSuffix = getPeriodSuffix(details)
            binding.tvPlanPrice.text = context.getString(
                when (periodSuffix) {
                    Period.WEEKLY -> R.string.billing_plan_per_week
                    Period.YEARLY -> R.string.billing_plan_per_year
                    else -> R.string.billing_plan_per_month
                }, priceText
            )

            // 选中态背景
            binding.root.setBackgroundResource(
                if (isSelected) R.drawable.bg_plan_selected else R.drawable.bg_plan_unselected
            )

            // 勾选
            binding.viewCircle.setBackgroundResource(
                if (isSelected) R.drawable.bg_plan_check else R.drawable.bg_plan_uncheck
            )
            binding.ivCheck.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE

            // 点击事件
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    setSelected(pos)
                    onItemClick?.invoke(details, pos)
                }
            }
        }

        private fun getPeriodSuffix(details: ProductDetails): Period {
            val sku = details.productId.lowercase()
            return when {
                sku.contains("week") || sku.contains("weekly") -> Period.WEEKLY
                sku.contains("year") || sku.contains("yearly") -> Period.YEARLY
                else -> Period.MONTHLY
            }
        }
    }

    private enum class Period { WEEKLY, MONTHLY, YEARLY }
}
