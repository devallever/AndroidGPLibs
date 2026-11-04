package app.allever.android.lib.recommend.data

import app.allever.android.lib.recommend.R

class RecommendId {
    companion object {
        const val DEVICE_DETECTOR = "app.allever.android.device.detector"
        const val JUST_NOTE = "app.allever.android.note.just"
        const val MEMORY_KITTIE = "app.allever.android.game.memory.kitties"
        const val LINK_LINK_GAME = "app.allever.android.game.linklinkgame"
        const val WATER_REMINDER = "app.allever.android.water.reminder"
        const val CARD_MAKER = "com.allever.android.card.text.pic.text"
        const val QR_SCANNER = "app.allever.android.ai.qr.scanner"

        const val GIF_MEMES = "app.funny.tech.gif.memes"

        const val FAKE_CALL = "app.funnyappptech.android.fake.call"

        fun getIconRes(pkg: String): Int {
            return when (pkg) {
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
                GIF_MEMES -> {
                    R.drawable.gif_memes
                }
                FAKE_CALL -> {
                    R.drawable.fakecall
                }

                else -> {
                    -1
                }
            }
        }
    }


}