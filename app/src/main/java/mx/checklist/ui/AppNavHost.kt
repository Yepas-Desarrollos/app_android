package mx.checklist.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import mx.checklist.ui.screens.ItemsScreen
import mx.checklist.ui.screens.LoginScreen
import mx.checklist.ui.screens.HomeScreen
import mx.checklist.ui.screens.HistoryScreen
import mx.checklist.ui.screens.RunScreen
import mx.checklist.ui.screens.StoresScreen
import mx.checklist.ui.screens.TemplatesScreen
import mx.checklist.ui.screens.admin.AdminTemplateListScreen
import mx.checklist.ui.screens.admin.AdminTemplateFormScreen
import mx.checklist.ui.screens.admin.AdminItemFormScreen
import mx.checklist.ui.vm.AuthViewModel
import mx.checklist.ui.vm.RunsViewModel
import mx.checklist.ui.vm.AdminViewModel
import mx.checklist.ui.navigation.NavRoutes

@Composable
fun AppNavHost(
    authVM: AuthViewModel,
    runsVM: RunsViewModel,
    adminVM: AdminViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = NavRoutes.HOME
) {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // LOGIN
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                vm = authVM,
                onLoggedIn = {
                    nav.navigate(NavRoutes.HOME) { popUpTo(0) }
                }
            )
        }

        // HOME - Pantalla principal con opciones
        composable(NavRoutes.HOME) {
            HomeScreen(
                vm = runsVM,
                authVM = authVM,
                onNuevaCorrida = {
                    nav.navigate(NavRoutes.STORES)
                },
                onOpenHistory = {
                    nav.navigate(NavRoutes.HISTORY)
                },
                onAdminAccess = {
                    nav.navigate(NavRoutes.ADMIN_TEMPLATES)
                },
                onLogout = {
                    nav.navigate(NavRoutes.LOGIN) { popUpTo(0) }
                }
            )
        }

        // HISTORY - Borradores y enviadas
        composable(NavRoutes.HISTORY) {
            HistoryScreen(
                vm = runsVM,
                onOpenRun = { runId, storeCode, templateName ->
                    nav.navigate(NavRoutes.run(runId))
                }
            )
        }

        // STORES
        composable(NavRoutes.STORES) {
            StoresScreen(
                vm = runsVM,
                onStoreSelected = { storeCode: String ->
                    nav.navigate(NavRoutes.templates(storeCode))
                },
                onAdminAccess = {
                    nav.navigate(NavRoutes.ADMIN_TEMPLATES)
                }
            )
        }

        // TEMPLATES (storeCode como path param)
        composable(
            route = NavRoutes.TEMPLATES,
            arguments = listOf(
                navArgument("storeCode") { type = NavType.StringType }
            )
        ) { backStack ->
            val storeCode = requireNotNull(backStack.arguments?.getString("storeCode")) {
                "storeCode es requerido"
            }

            TemplatesScreen(
                storeCode = storeCode,
                vm = runsVM,
                onRunCreated = { runId: Long, sc: String ->
                    // Usa query param, consistente con NavRoutes.run(...)
                    nav.navigate(NavRoutes.run(runId))
                }
            )
        }

        // ITEMS (runId como path + storeCode como query param)
        composable(
            route = NavRoutes.RUN,
            arguments = listOf(
                navArgument("runId") { type = NavType.LongType }
            )
        ) { backStack ->
            val runId = requireNotNull(backStack.arguments?.getLong("runId")) {
                "runId es requerido"
            }

            ItemsScreen(
                runId = runId,
                storeCode = "AUTO", // Para mantener compatibilidad
                vm = runsVM,
                onSubmit = {
                    nav.navigate(NavRoutes.HOME) { popUpTo(0) }
                }
            )
        }

        // === ADMIN ROUTES ===
        
        // Lista de templates admin
        composable(NavRoutes.ADMIN_TEMPLATES) {
            AdminTemplateListScreen(
                vm = adminVM,
                onCreateTemplate = {
                    nav.navigate(NavRoutes.adminTemplateForm())
                },
                onEditTemplate = { templateId ->
                    nav.navigate(NavRoutes.adminTemplateForm(templateId))
                },
                onViewTemplate = { templateId ->
                    nav.navigate(NavRoutes.adminTemplateForm(templateId))
                }
            )
        }

        // Formulario de template admin
        composable(
            route = NavRoutes.ADMIN_TEMPLATE_FORM,
            arguments = listOf(
                navArgument("templateId") {
                    type = NavType.StringType
                    nullable = false
                    defaultValue = "-1"
                }
            )
        ) { backStack ->
            val templateIdString = backStack.arguments?.getString("templateId") ?: "-1"
            val templateId = templateIdString.toLongOrNull()?.takeIf { it != -1L }

            AdminTemplateFormScreen(
                vm = adminVM,
                templateId = templateId,
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
                onCreateItem = { templateId ->
                    nav.navigate(NavRoutes.adminItemForm(templateId))
                },
                onEditItem = { templateId, itemId ->
                    nav.navigate(NavRoutes.adminItemForm(templateId, itemId))
                }
            )
        }

        // Formulario de item admin
        composable(
            route = NavRoutes.ADMIN_ITEM_FORM,
            arguments = listOf(
                navArgument("templateId") { type = NavType.LongType },
                navArgument("itemId") {
                    type = NavType.StringType
                    nullable = false
                    defaultValue = "-1"
                }
            )
        ) { backStack ->
            val templateId = requireNotNull(backStack.arguments?.getLong("templateId")) {
                "templateId es requerido"
            }
            val itemIdString = backStack.arguments?.getString("itemId") ?: "-1"
            val itemId = itemIdString.toLongOrNull()?.takeIf { it != -1L }

            AdminItemFormScreen(
                vm = adminVM,
                templateId = templateId,
                itemId = itemId,
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() }
            )
        }
    }
}
