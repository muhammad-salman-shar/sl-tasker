package com.neurasamu.build.sl_tasker.data.block

object BlockPresets {

    val SOCIAL: Set<String> = setOf(
        "com.facebook.katana",
        "com.facebook.lite",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.google.android.youtube",
        "com.snapchat.android",
        "com.twitter.android",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.discord",
        "com.reddit.frontpage",
        "com.linkedin.android",
        "com.pinterest",
        "com.tumblr",
        "com.vkontakte.android",
        "com.zhihu.android"
    )

    val AI_APPS: Set<String> = setOf(
        "com.openai.chatgpt",
        "com.anthropic.claude",
        "com.google.android.apps.bard",
        "ai.perplexity.app.android",
        "com.microsoft.bing",
        "com.smartwidgetlabs.chatgpt",
        "com.deepseek.chat",
        "com.perplexity.ai"
    )

    val GAMES: Set<String> = setOf(
        "com.supercell.clashofclans",
        "com.supercell.clashroyale",
        "com.mojang.minecraftpe",
        "com.innersloth.spacemafia",
        "com.mojang.minecraftedu",
        "com.tencent.ig",
        "com.pubg.newstate",
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "com.epicgames.fortnite",
        "com.roblox.client",
        "com.ea.game.pvz2_row",
        "com.gameloft.android.ANMP.GloftA9HM"
    )

    val ENTERTAINMENT: Set<String> = setOf(
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "tv.twitch.android.app",
        "com.disney.disneyplus",
        "com.hotstar.android",
        "com.zhiliaoapp.musically.go",
        "com.crunchyroll.crunchyroid",
        "com.plexapp.android"
    )

    fun byName(name: String): Set<String> = when (name.uppercase()) {
        "SOCIAL" -> SOCIAL
        "AI", "AI_APPS" -> AI_APPS
        "GAMES", "GAMING" -> GAMES
        "ENTERTAINMENT", "MEDIA" -> ENTERTAINMENT
        else -> emptySet()
    }

    fun installedFrom(
        categories: Set<String>,
        installedPackages: Set<String>
    ): Set<String> = categories.filter { it in installedPackages }.toSet()
}
