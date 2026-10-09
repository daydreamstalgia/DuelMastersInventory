package ro.ddnostalgia.duelmastersinventory.nav

import androidx.navigation.NavType

sealed class Routes(
    val route: String,
    val args: Map<String, Pair<NavType<*>, Boolean>> = mapOf()
) {
    data object CardList
        : Routes("cards")

    data object CardView
        : Routes("card/{id}?print={printId}", mapOf(
            "id" to Pair(NavType.IntType, false),
            "printId" to Pair(NavType.IntType, true),
        )) {

        fun createRoute(id: Int, print: Int? = null): String {
            return if (print != null) {
                "card/$id?print=$print"
            } else {
                "card/$id"
            }
        }

        const val cardIdArg = "id"
        const val printIdArg = "printId"
    }

    data object TransactionsList
        : Routes("transactions")

    data object TransactionView
        : Routes("transaction/{id}", mapOf(
        "id" to Pair(NavType.IntType, false),
    )) {

        fun createRoute(id: Int): String = "transaction/${id}"

        const val transactionIdArg = "id"
    }

    data object TransactedCardsEdit
        : Routes("transaction/{id}/edit_cards", mapOf(
        "id" to Pair(NavType.IntType, false),
    )) {

        fun createRoute(id: Int): String = "transaction/${id}/edit_cards"

        const val transactionIdArg = "id"
    }

    data object CardScan
        : Routes("transaction/{id}/scan", mapOf(
        "id" to Pair(NavType.IntType, false),
    )) {

        fun createRoute(id: Int): String = "transaction/${id}/scan"

        const val transactionIdArg = "id"
    }


    data object  TransactionEdit
        : Routes("transaction/{id}/edit", mapOf(
            "id" to Pair(NavType.IntType, false)
        )) {
        fun createRoute(id: Int): String = "transaction/${id}/edit"

        const val transactionIdArg = "id"
        }

    data object  TransactionCreate : Routes("transaction/create")


    data object Spreadsheets: Routes("spreadsheets")

    data object  SpreadsheetConfigCreate
        : Routes("spreadsheets/config/create?email={email}", mapOf(
        "email" to Pair(NavType.StringType, false)
    )) {
        fun createRoute(email: String): String
                = "spreadsheets/config/create?email=${email}"
        const val emailIdArg = "email"
    }

    data object SpreadsheetConfigView
        : Routes("spreadsheets/config/{id}", mapOf(
            "id" to Pair(NavType.IntType, false)
        )) {
        fun createRoute(id: Int): String = "spreadsheets/config/${id}"
        const val spreadsheetConfigIdArg = "id"
        }

    data object SpreadsheetConfigEdit
        : Routes("spreadsheets/config/{id}/edit?email={email}", mapOf(
        "id" to Pair(NavType.IntType, false),
        "email" to Pair(NavType.StringType, false)
        )) {
        fun createRoute(id: Int, email: String): String
            = "spreadsheets/config/${id}/edit?email=${email}"
        const val spreadsheetConfigIdArg = "id"
        const val emailIdArg = "email"
        }

    data object ActorsList
        : Routes("actors")

    data object ActorView
        : Routes("actor/{id}", mapOf(
        "id" to Pair(NavType.IntType, false),
    )) {

        fun createRoute(id: Int): String = "actor/${id}"

        const val actorIdArg = "id"
    }

    data object ActorEdit
        : Routes("actor/{id}/edit", mapOf(
            "id" to Pair(NavType.IntType, false)
        )) {
        fun createRoute(id: Int): String = "actor/${id}/edit"

        const val actorIdArg = "id"
        }

    data object ActorCreate : Routes("actor/create")

    data object Statistics: Routes("statistics")
    data object Sets: Routes("sets")
    data object Settings: Routes("settings")
    data object Onboarding: Routes("onboarding")
}

