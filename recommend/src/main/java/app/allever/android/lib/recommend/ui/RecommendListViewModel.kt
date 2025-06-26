package app.allever.android.lib.recommend.ui

import androidx.lifecycle.ViewModel
import app.allever.android.lib.recommend.data.RecommendHelper

class RecommendListViewModel: ViewModel() {
    val adapter = RecommendAdapter(RecommendHelper.list)

}