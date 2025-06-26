package app.allever.android.lib.recommend.data

import app.allever.android.lib.recommend.R

class RecommendId {
    companion object {
        const val DEVICE_DETECTOR = 1
        const val JUST_NOTE = 2
        const val MEMORY_KITTIE = 3
        const val LINK_LINK_GAME = 4
        const val WATER_REMINDER = 5
        const val CARD_MAKER = 6
        const val QR_SCANNER = 7

        fun getIconRes(id: Int): Int {
            return when (id) {
                DEVICE_DETECTOR -> {
                    R.drawable.device_detector
                }

                JUST_NOTE -> {
                    R.drawable.just_note
                }
                MEMORY_KITTIE -> {
                    R.drawable.memory_kitties
                }
                LINK_LINK_GAME -> {
                    R.drawable.link_link_game
                }
                WATER_REMINDER -> {
                    R.drawable.water_reminder
                }
                CARD_MAKER -> {
                    R.drawable.card_maker
                }
                QR_SCANNER -> {
                    R.drawable.qr_scanner
                }

                else -> {
                    -1
                }
            }
        }
    }


}