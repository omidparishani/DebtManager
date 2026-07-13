package com.debtmanager.app.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

data class ItemIconOption(val key: String, val label: String, val icon: ImageVector)

object ItemIcons {
    val all: List<ItemIconOption> = listOf(
        ItemIconOption("person", "شخص", Icons.Filled.Person),
        ItemIconOption("store", "فروشگاه", Icons.Filled.Store),
        ItemIconOption("account_balance", "بانک", Icons.Filled.AccountBalance),
        ItemIconOption("receipt", "چک", Icons.Filled.Receipt),
        ItemIconOption("home", "خانه", Icons.Filled.Home),
        ItemIconOption("car", "خودرو", Icons.Filled.DirectionsCar),
        ItemIconOption("health", "بیمه/سلامت", Icons.Filled.LocalHospital),
        ItemIconOption("school", "تحصیل", Icons.Filled.School),
        ItemIconOption("phone", "موبایل", Icons.Filled.PhoneAndroid),
        ItemIconOption("credit_card", "کارت", Icons.Filled.CreditCard),
        ItemIconOption("work", "کار", Icons.Filled.Work),
        ItemIconOption("family", "خانواده", Icons.Filled.FamilyRestroom),
        ItemIconOption("shopping", "خرید", Icons.Filled.ShoppingCart),
        ItemIconOption("restaurant", "رستوران", Icons.Filled.Restaurant),
        ItemIconOption("build", "تعمیرات", Icons.Filled.Build),
        ItemIconOption("money", "پول", Icons.Filled.AttachMoney),
        ItemIconOption("savings", "پس‌انداز", Icons.Filled.Savings),
        ItemIconOption("other", "سایر", Icons.Filled.MoreHoriz)
    )

    val profileAvatars: List<ItemIconOption> = listOf(
        ItemIconOption("avatar_person", "پروفایل", Icons.Filled.AccountCircle),
        ItemIconOption("avatar_face", "چهره", Icons.Filled.Face),
        ItemIconOption("avatar_star", "ستاره", Icons.Filled.Star),
        ItemIconOption("avatar_emoji", "لبخند", Icons.Filled.EmojiEmotions),
        ItemIconOption("avatar_business", "کسب‌وکار", Icons.Filled.BusinessCenter),
        ItemIconOption("avatar_brand", "امید", Icons.Filled.Favorite)
    )

    fun resolve(key: String): ImageVector {
        return all.find { it.key == key }?.icon
            ?: profileAvatars.find { it.key == key }?.icon
            ?: Icons.Filled.Circle
    }

    fun defaultForDebtCategory(category: String): String = when (category) {
        "PERSON" -> "person"
        "STORE" -> "store"
        else -> "other"
    }

    fun defaultForPaymentType(type: String): String = when (type) {
        "LOAN" -> "account_balance"
        "CHECK" -> "receipt"
        "DEBT" -> "person"
        "RECURRING" -> "autorenew"
        else -> "money"
    }
}
