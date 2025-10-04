package mx.checklist.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mx.checklist.data.auth.AuthState
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
import mx.checklist.ui.screens.SimpleOptimizedHistoryScreen
import mx.checklist.ui.screens.SimpleOptimizedTemplatesScreen
import mx.checklist.ui.screens.SimpleOptimizedAdminScreen
import mx.checklist.ui.screens.TemplatesAdminScreen
import mx.checklist.ui.screens.AssignmentScreen
import mx.checklist.ui.screens.admin.AdminTemplateListScreen
import mx.checklist.ui.screens.admin.AdminTemplateFormScreen
import mx.checklist.ui.screens.admin.AdminItemFormScreen
import mx.checklist.ui.screens.admin.AdminSectionFormScreen
import mx.checklist.ui.vm.AuthViewModel
import mx.checklist.ui.vm.RunsViewModel
import mx.checklist.ui.vm.AdminViewModel
import mx.checklist.ui.vm.AssignmentViewModel
import mx.checklist.ui.vm.ChecklistStructureViewModel
import mx.checklist.ui.screens.ChecklistStructureScreen
import mx.checklist.ui.screens.SectionItemsScreen
import mx.checklist.ui.navigation.NavRoutes
import mx.checklist.config.AppConfig.ENABLE_PAGINATION_OPTIMIZATIONS

@Composable
fun AppNavHost(
    authVM: AuthViewModel,
    runsVM: RunsViewModel,
    adminVM: AdminViewModel,
    assignmentVM: AssignmentViewModel,
    checklistVM: ChecklistStructureViewModel,
    modifier: Modifier = Modifier
) {
    val nav = rememberNavController()
    
    val authState by authVM.state.collectAsStateWithLifecycle()
    val currentRoleCode = authState.authenticated?.roleCode
    val isAdmin = currentRoleCode in listOf("ADMIN", "MGR_PREV", "MGR_OPS")
    
    val startDestination = if (authState.authenticated != null) {
        NavRoutes.HOME
    } else {
        NavRoutes.LOGIN
    }
    
    LaunchedEffect(authState.authenticated?.roleCode) {
        AuthState.roleCode = authState.authenticated?.roleCode
    }
    
    Log.d("AppNavHost", "🏠 AppNavHost - AuthViewModel.roleCode: '$currentRoleCode', AuthState.roleCode: '${AuthState.roleCode}', isAdmin: $isAdmin")

    NavHost(
        navController = nav,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                vm = authVM,
                onLoggedIn = {
                    nav.navigate(NavRoutes.HOME) { popUpTo(0) }
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                vm = runsVM,
                authVM = authVM,
                onNuevaCorrida = { nav.navigate(NavRoutes.STORES) },
                onOpenHistory = { nav.navigate(NavRoutes.HISTORY) },
                onAdminAccess = if (isAdmin) {{ nav.navigate(NavRoutes.ADMIN_TEMPLATES) }} else null,
                onLogout = { nav.navigate(NavRoutes.LOGIN) { popUpTo(0) } }
            )
        }

        composable(NavRoutes.HISTORY) {
            if (ENABLE_PAGINATION_OPTIMIZATIONS) {
                SimpleOptimizedHistoryScreen(
                    runsVM = runsVM,
                    adminVM = adminVM,
                    onOpenRun = { runId, _, _ -> nav.navigate(NavRoutes.run(runId)) }
                )
            } else {
                HistoryScreen(
                    vm = runsVM,
                    adminVM = adminVM,
                    onOpenRun = { runId, _, _ -> nav.navigate(NavRoutes.run(runId)) }
                )
            }
        }

        composable(NavRoutes.STORES) {
            StoresScreen(
                vm = runsVM,
                onStoreSelected = { storeCode -> nav.navigate(NavRoutes.templates(storeCode)) },
                onAdminAccess = if (isAdmin) {{ nav.navigate(NavRoutes.ADMIN_TEMPLATES) }} else null
            )
        }

        composable(
            route = NavRoutes.TEMPLATES,
            arguments = listOf(navArgument("storeCode") { type = NavType.StringType })
        ) { backStack ->
            val storeCode = requireNotNull(backStack.arguments?.getString("storeCode"))
            if (ENABLE_PAGINATION_OPTIMIZATIONS) {
                SimpleOptimizedTemplatesScreen(
                    storeCode = storeCode,
                    runsVM = runsVM,
                    // ✅ CORREGIDO: Usar el runId que devuelve el callback en lugar de buscar el primer run
                    onRunCreated = { runId, _ ->
                        // runId ahora es el ID del run recién creado
                        nav.navigate(NavRoutes.run(runId))
                    }
                )
            } else {
                TemplatesScreen(
                    storeCode = storeCode,
                    vm = runsVM,
                    // ✅ CORREGIDO: Usar el runId que devuelve el callback
                    onRunCreated = { runId, _ ->
                        nav.navigate(NavRoutes.run(runId))
                    }
                )
            }
        }

        composable(
            route = NavRoutes.RUN,
            arguments = listOf(navArgument("runId") { type = NavType.LongType })
        ) { backStack ->
            val runId = requireNotNull(backStack.arguments?.getLong("runId"))

            // Cargar información del run para obtener el storeCode real
            LaunchedEffect(runId) {
                runsVM.loadRunInfo(runId)
            }

            val runInfo by runsVM.runInfoFlow().collectAsStateWithLifecycle()
            val storeCode = runInfo?.storeCode ?: "Cargando..."

            ItemsScreen(
                runId = runId,
                storeCode = storeCode,
                vm = runsVM,
                onSubmit = { nav.navigate(NavRoutes.HOME) { popUpTo(0) } }
            )
        }

        // === ADMIN ROUTES ===
        composable(NavRoutes.ADMIN_TEMPLATES) {
            if (!isAdmin) {
                nav.navigate(NavRoutes.HOME) { popUpTo(NavRoutes.ADMIN_TEMPLATES) { inclusive = true } }
                return@composable
            }
            if (ENABLE_PAGINATION_OPTIMIZATIONS) {
                SimpleOptimizedAdminScreen(
                    adminVM = adminVM,
                    runsVM = runsVM,
                    onCreateTemplate = { nav.navigate(NavRoutes.adminTemplateForm(null)) },
                    onEditTemplate = { templateId -> nav.navigate(NavRoutes.adminTemplateForm(templateId)) },
                    onViewTemplate = { templateId -> nav.navigate(NavRoutes.adminTemplateForm(templateId)) },
                    onAssignments = { nav.navigate(NavRoutes.ADMIN_ASSIGNMENTS) },
                    onTemplatesAdmin = { nav.navigate(NavRoutes.ADMIN_TEMPLATES_ADMIN) }
                )
            } else {
                AdminTemplateListScreen(
                    vm = adminVM,
                    onCreateTemplate = { nav.navigate(NavRoutes.adminTemplateForm(null)) },
                    onEditTemplate = { templateId -> nav.navigate(NavRoutes.adminTemplateForm(templateId)) },
                    onViewTemplate = { templateId -> nav.navigate(NavRoutes.adminTemplateForm(templateId)) }
                )
            }
        }

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
            if (!isAdmin) {
                nav.navigate(NavRoutes.HOME) { popUpTo(NavRoutes.ADMIN_TEMPLATE_FORM) { inclusive = true } }
                return@composable
            }
            val templateIdString = backStack.arguments?.getString("templateId") ?: "-1"
            val templateIdFromArg = templateIdString.toLongOrNull()?.takeIf { it != -1L }

            val onCreateItemFunc: (Long) -> Unit = { sectionId: Long ->
                // Navega al formulario de ítem, usando el templateId y el sectionId
                nav.navigate(NavRoutes.adminItemForm(templateId = templateIdFromArg ?: -1L, sectionId = sectionId, itemId = null))
            }
            val onEditSectionFunc: (Long, Long) -> Unit = { templateId: Long, sectionId: Long ->
                // Navega al formulario de sección para EDITAR sección
                nav.navigate(NavRoutes.adminSectionForm(templateId = templateId, sectionId = sectionId))
            }
            val onCreateSectionFunc: (Long) -> Unit = { templateId: Long ->
                // Navega al formulario de sección para CREAR nueva sección
                nav.navigate(NavRoutes.adminSectionForm(templateId = templateId, sectionId = null))
            }
            AdminTemplateFormScreen(
                vm = adminVM,
                templateId = templateIdFromArg,
                onBack = { nav.popBackStack() },
                onSaved = { adminVM.loadTemplates(); nav.popBackStack() },
                onCreateItem = onCreateItemFunc,
                onEditItem = onEditSectionFunc, // Este es para editar SECCIONES
                onCreateSection = onCreateSectionFunc
            )
        }

        // Formulario de sección admin - Solo para administradores
        composable(
            route = NavRoutes.ADMIN_SECTION_FORM,
            arguments = listOf(
                navArgument("templateId") { type = NavType.LongType },
                navArgument("sectionId") {
                    type = NavType.StringType 
                    nullable = false
                    defaultValue = "-1"
                }
            )
        ) { backStack ->
            if (!isAdmin) {
                nav.navigate(NavRoutes.HOME) { popUpTo(NavRoutes.ADMIN_SECTION_FORM) { inclusive = true } }
                return@composable
            }
            val templateId = requireNotNull(backStack.arguments?.getLong("templateId"))
            val sectionIdString = backStack.arguments?.getString("sectionId") ?: "-1"
            val sectionIdFromArg = sectionIdString.toLongOrNull()?.takeIf { it != -1L }

            val onCreateItemFunc: (Long) -> Unit = { currentSectionId: Long -> // currentSectionId es el ID de la sección actual
                 nav.navigate(NavRoutes.adminItemForm(templateId = templateId, sectionId = currentSectionId, itemId = null))
            }
            val onEditItemFunc: (Long, Long) -> Unit = { currentSectionId: Long, itemID: Long ->
                 nav.navigate(NavRoutes.adminItemForm(templateId = templateId, sectionId = currentSectionId, itemId = itemID))
            }
            
            AdminSectionFormScreen(
                vm = adminVM,
                templateId = templateId, // Cambiado de checklistId a templateId en la llamada
                sectionId = sectionIdFromArg,
                onBack = { nav.popBackStack() },
                onSaved = {
                    // Recargar el template y luego navegar de vuelta
                    adminVM.loadTemplate(templateId)
                    nav.popBackStack()
                },
                onCreateItem = onCreateItemFunc,
                onEditItem = onEditItemFunc
            )
        }
        
        // Formulario de item admin - Solo para administradores
        composable(
            route = NavRoutes.ADMIN_ITEM_FORM, // Ruta actualizada en NavRoutes.kt
            arguments = listOf(
                navArgument("templateId") { type = NavType.LongType },
                navArgument("sectionId") { type = NavType.LongType }, // Argumento sectionId añadido
                navArgument("itemId") {
                    type = NavType.StringType
                    nullable = false
                    defaultValue = "-1"
                }
            )
        ) { backStack ->
            if (!isAdmin) {
                nav.navigate(NavRoutes.HOME) { popUpTo(NavRoutes.ADMIN_ITEM_FORM) { inclusive = true } }
                return@composable
            }

            val templateId = requireNotNull(backStack.arguments?.getLong("templateId"))
            val sectionId = requireNotNull(backStack.arguments?.getLong("sectionId"))
            val itemIdString = backStack.arguments?.getString("itemId") ?: "-1"
            val itemId = itemIdString.toLongOrNull()?.takeIf { it != -1L }

            AdminItemFormScreen(
                vm = adminVM,
                templateId = templateId, // <-- Nuevo parámetro obligatorio
                itemId = itemId,
                sectionId = sectionId,
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() }
            )
        }

        composable(NavRoutes.ADMIN_ASSIGNMENTS) {
            if (!isAdmin) {
                nav.navigate(NavRoutes.HOME) { popUpTo(NavRoutes.ADMIN_ASSIGNMENTS); launchSingleTop = true }
                return@composable
            }
            AssignmentScreen(assignmentVM = assignmentVM, onBack = { nav.popBackStack() })
        }

        composable(NavRoutes.ADMIN_TEMPLATES_ADMIN) {
            if (!isAdmin) {
                nav.navigate(NavRoutes.HOME) { popUpTo(NavRoutes.ADMIN_TEMPLATES_ADMIN); launchSingleTop = true }
                return@composable
            }
            TemplatesAdminScreen(
                adminVM = adminVM,
                onCreateTemplate = { nav.navigate(NavRoutes.adminTemplateForm(null)) },
                onEditTemplate = { templateId -> nav.navigate(NavRoutes.adminTemplateForm(templateId)) },
                onViewTemplate = { templateId -> nav.navigate(NavRoutes.adminTemplateForm(templateId)) },
                onBack = { nav.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.CHECKLIST_STRUCTURE,
            arguments = listOf(navArgument("checklistId") { type = NavType.LongType })
        ) { backStack ->
            val checklistId = requireNotNull(backStack.arguments?.getLong("checklistId"))
            ChecklistStructureScreen(
                checklistId = checklistId,
                viewModel = checklistVM,
                navigateBack = { adminVM.loadTemplate(checklistId); nav.popBackStack() },
                onOpenSectionItems = { sectionId -> nav.navigate(NavRoutes.sectionItems(sectionId)) }
            )
        }

        composable(
            route = NavRoutes.SECTION_ITEMS,
            arguments = listOf(navArgument("sectionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getLong("sectionId") ?: -1L
            SectionItemsScreen(
                sectionId = sectionId,
                viewModel = checklistVM,
                navigateBack = { nav.navigateUp() }
            )
        }
    }
}