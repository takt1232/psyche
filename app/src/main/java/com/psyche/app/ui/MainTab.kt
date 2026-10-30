package com.psyche.app.ui

import androidx.annotation.DrawableRes
import com.psyche.app.R

enum class MainTab(val label: String, @DrawableRes val icon: Int) {
    HOME("Home", R.drawable.ic_home),
    ACTIVITY("Activity", R.drawable.ic_activity),
    BUDGETS("Budgets", R.drawable.ic_budget),
    ACCOUNTS("Accounts", R.drawable.ic_accounts),
}
