package app.allever.android.lib.recommend.data

import android.content.Context
import app.allever.android.lib.recommend.util.RecommendUtils
import app.allever.android.lib.recommend.util.removeItemIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object RecommendHelper {

    val list = mutableListOf<Recommend>()

    fun init(context: Context) {
        RecommendUtils.context = context.applicationContext
        initRecommend()
    }

    fun initRecommend() {
        CoroutineScope(Dispatchers.IO).launch {
            val response = RecommendUtils.readFile2String(RecommendUtils.context, "recommend.json")
            val recommendResponse =
                RecommendUtils.fromJson(response, RecommendResponse::class.java)
            list.clear()
            list.addAll(recommendResponse?.data ?: emptyList())
            list.removeItemIf {
                it.pkg == RecommendUtils.context.packageName
            }
            list.map {
                try {
                    it.isInstall = RecommendUtils.context.packageManager.getPackageInfo(it.pkg, 0) != null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                RecommendUtils.log(RecommendUtils.toJson(it))
            }
        }
    }

    fun getRecommendApp(): Recommend {
        return list.random()
    }
}
