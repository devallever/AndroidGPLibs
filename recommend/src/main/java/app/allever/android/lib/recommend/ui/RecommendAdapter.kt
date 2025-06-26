package app.allever.android.lib.recommend.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.allever.android.lib.recommend.data.Recommend
import app.allever.android.lib.recommend.data.RecommendId
import app.allever.android.lib.recommend.databinding.ItemRecommendBinding
import app.allever.android.lib.recommend.util.RecommendUtils
import app.allever.android.lib.recommend.util.loadCircle

class RecommendAdapter(val data: MutableList<Recommend>) :
    RecyclerView.Adapter<RecommendAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRecommendBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemRecommendBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.binding.apply {
            ivIcon.loadCircle(RecommendId.getIconRes(item.id), 0)
            tvTitle.text = item.name
            tvDesc.text = item.desc
            if (RecommendUtils.isAppExist(RecommendUtils.context, item.pkg)) {
                btnInstall.text = "Open"
            } else {
                btnInstall.text = "Install"
            }
            btnInstall.setOnClickListener {
                if (RecommendUtils.isAppExist(RecommendUtils.context, item.pkg)) {
//                    AppManager.openApp(item.pkg)
                    //打开应用
                    RecommendUtils.openApp(item.pkg)
                } else {
//                    AppManager.installApp(item.pkg)
                    //跳转谷歌应用商店应用信息
                    RecommendUtils.jumpToAppStore(item.pkg)
                }
            }
        }
    }
}