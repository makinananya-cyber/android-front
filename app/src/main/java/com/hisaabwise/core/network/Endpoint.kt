package com.hisaabwise.core.network

/** Every path the app calls. Port of iOS `Endpoint`. Ids are sanitised into one path segment. */
object Endpoint {
    // Auth
    const val LOGIN = "/v1/auth/login"
    const val REFRESH = "/v1/auth/refresh"
    const val LOGOUT = "/v1/auth/logout"
    const val REGISTER = "/v1/auth/register"
    const val FORGOT_PASSWORD_QUESTIONS = "/v1/auth/forgot-password/questions"
    const val FORGOT_PASSWORD_VERIFY = "/v1/auth/forgot-password/verify"
    const val RESET_PASSWORD = "/v1/auth/reset-password"

    // Me
    const val ME = "/v1/me"
    const val CURRENCY = "/v1/me/currency"
    const val LANGUAGE = "/v1/me/language"
    const val PASSWORD = "/v1/me/password"
    const val PASSWORD_CHECK = "/v1/me/password/check"
    const val GOAL = "/v1/me/goal"
    const val WANTS_SHARE = "/v1/me/budget/wants"
    const val DELETE_ACCOUNT = "/v1/me"
    const val EXPORT = "/v1/me/export"

    // Screens
    const val SCREEN_HOME = "/v1/screens/home"
    const val SCREEN_EXPENSES = "/v1/screens/expenses"
    const val SCREEN_LEARN = "/v1/screens/learn"
    const val SCREEN_REPORTS = "/v1/screens/reports"
    const val SCREEN_ACCOUNT = "/v1/screens/account"
    fun screenReportsMonth(monthKey: String) = "$SCREEN_REPORTS/${segment(monthKey)}"
    const val BUDGET = "/v1/budget"

    // Expenses
    const val EXPENSES = "/v1/expenses"
    fun expense(id: String) = "$EXPENSES/${segment(id)}"
    fun fixedCost(categoryId: String) = "/v1/expenses/fixed/${segment(categoryId)}"
    fun billLines(categoryId: String) = "/v1/expenses/lines/${segment(categoryId)}"

    // Learn
    const val LEARN_PROGRESS = "/v1/learn/progress"
    fun lessonCompletion(lessonId: String) = "/v1/learn/lessons/${segment(lessonId)}/complete"
    const val CURRICULUM_PDF = "/v1/content/curriculum/pdf"

    // Events
    const val EVENTS = "/v1/events"

    private fun segment(id: String) = id.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
}
