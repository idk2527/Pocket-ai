package com.pocketai.app.presentation.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Receipts : Screen("receipts")
    object Scanner : Screen("scanner")
    object ModelDownload : Screen("model_download")

    object AddExpense : Screen("add_expense?expenseId={expenseId}") {
        const val EXPENSE_ID_ARG = "expenseId"
        val routeWithArg = "add_expense?expenseId={$EXPENSE_ID_ARG}"
        val navigationRoute = "add_expense"

        fun withId(id: Int): String {
            return routeWithArg.replace("{$EXPENSE_ID_ARG}", id.toString())
        }
    }

    object ExpenseDetail : Screen("expense_detail/{expenseId}") {
        const val EXPENSE_ID_ARG = "expenseId"
        val routeWithArg = "expense_detail/{$EXPENSE_ID_ARG}"

        fun withId(id: Int): String {
            return routeWithArg.replace("{$EXPENSE_ID_ARG}", id.toString())
        }
    }
    
    object Settings : Screen("settings")
    object Analytics : Screen("analytics")
    object Chat : Screen("chat")

    object DigitalReceipt : Screen("digital_receipt/{expenseId}") {
        const val EXPENSE_ID_ARG = "expenseId"
        val routeWithArg = "digital_receipt/{$EXPENSE_ID_ARG}"

        fun withId(id: Int): String {
            return routeWithArg.replace("{$EXPENSE_ID_ARG}", id.toString())
        }
    }
}