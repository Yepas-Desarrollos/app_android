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
import mx.checklist.ui.vm.AuthViewModel
import mx.checklist.ui.vm.RunsViewModel
import mx.checklist.ui.vm.AdminViewModel
import mx.checklist.ui.vm.AssignmentViewModel
import mx.checklist.ui.navigation.NavRoutes
import mx.checklist.config.AppConfig.ENABLE_PAGINATION_OPTIMIZATIONS

@Composable
fun AppNavHost(
    authVM: AuthViewModel,
    runsVM: RunsViewModel,
    adminVM: AdminViewModel,
    assignmentVM: AssignmentViewModel,
    modifier: Modifier = Modifier
) {
    val nav = rememberNavController()
    
    // SOLUCIÓN: Usar el estado reactivo del AuthViewModel
    val authState by authVM.state.collectAsStateWithLifecycle()
    val currentRoleCode = authState.authenticated?.roleCode
    val isAdmin = currentRoleCode in listOf("ADMIN", "MGR_PREV", "MGR_OPS")
    
    // Determinar destino inicial basado en autenticación
    val startDestination = if (authState.authenticated != null) {
        Log.d("AppNavHost", "🔑 Usuario autenticado encontrado, iniciando en HOME")
        NavRoutes.HOME
    } else {
        Log.d("AppNavHost", "🚪 No hay autenticación, iniciando en LOGIN")
        NavRoutes.LOGIN
    }
    
    // Forzar recomposición cuando cambie el estado de auth
    LaunchedEffect(authState.authenticated?.roleCode) {
        Log.d("AppNavHost", "🔄 AuthState cambió - roleCode: ${authState.authenticated?.roleCode}")
        // Sincronizar AuthState global con AuthViewModel
        AuthState.roleCode = authState.authenticated?.roleCode
        Log.d("AppNavHost", "🔄 AuthState.roleCode sincronizado: ${AuthState.roleCode}")
    }
    
    // Log de diagnóstico para AppNavHost
    Log.d("AppNavHost", "🏠 AppNavHost - AuthViewModel.roleCode: '$currentRoleCode', AuthState.roleCode: '${AuthState.roleCode}', isAdmin: $isAdmin")

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
                onAdminAccess = if (isAdmin) {
                    Log.d("AppNavHost", "✅ onAdminAccess configurado - usuario ES admin"); 
                    { nav.navigate(NavRoutes.ADMIN_TEMPLATES) }
                } else {
                    Log.d("AppNavHost", "❌ onAdminAccess = null - usuario NO es admin"); 
                    null
                },
                onLogout = {
                    nav.navigate(NavRoutes.LOGIN) { popUpTo(0) }
                }
            )
        }

        // HISTORY - Borradores y enviadas (OPTIMIZADO)
        composable(NavRoutes.HISTORY) {
            if (ENABLE_PAGINATION_OPTIMIZATIONS) {
                // Usar versión optimizada con datos reales
                SimpleOptimizedHistoryScreen(
                    runsVM = runsVM,
                    adminVM = adminVM,
                    onOpenRun = { runId, storeCode, templateName ->
                        nav.navigate(NavRoutes.run(runId))
                    }
                )
            } else {
                // Fallback a versión original
                HistoryScreen(
                    vm = runsVM,
                    adminVM = adminVM,
                    onOpenRun = { runId, storeCode, templateName ->
                        nav.navigate(NavRoutes.run(runId))
                    }
                )
            }
        }

        // STORES
        composable(NavRoutes.STORES) {
            StoresScreen(
                vm = runsVM,
                onStoreSelected = { storeCode: String ->
                    nav.navigate(NavRoutes.templates(storeCode))
                },
                onAdminAccess = if (isAdmin) {
                    { nav.navigate(NavRoutes.ADMIN_TEMPLATES) }
                } else null
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

            if (ENABLE_PAGINATION_OPTIMIZATIONS) {
                // Usar versión optimizada con datos reales
                SimpleOptimizedTemplatesScreen(
                    storeCode = storeCode,
                    runsVM = runsVM,
                    onRunCreated = { runId: Long, sc: String ->
                        nav.navigate(NavRoutes.run(runId))
                    }
                )
            } else {
                // Fallback a versión original
                TemplatesScreen(
                    storeCode = storeCode,
                    vm = runsVM,
                    onRunCreated = { runId: Long, sc: String ->
                        nav.navigate(NavRoutes.run(runId))
                    }
                )
            }
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
        
        // Lista de templates admin (OPTIMIZADO) - Solo para administradores
        composable(NavRoutes.ADMIN_TEMPLATES) {
            if (!isAdmin) {
                // Redirigir a HOME si no es admin
                nav.navigate(NavRoutes.HOME) { 
                    popUpTo(NavRoutes.ADMIN_TEMPLATES) { inclusive = true }
                }
                return@composable
            }
            
            if (ENABLE_PAGINATION_OPTIMIZATIONS) {
                // Usar versión optimizada con datos reales
                SimpleOptimizedAdminScreen(
                    adminVM = adminVM,
                    runsVM = runsVM,
                    onCreateTemplate = {
                        nav.navigate(NavRoutes.adminTemplateForm())
                    },
                    onEditTemplate = { templateId: Long ->
                        nav.navigate(NavRoutes.adminTemplateForm(templateId))
                    },
                    onViewTemplate = { templateId: Long ->
                        nav.navigate(NavRoutes.adminTemplateForm(templateId))
                    },
                    onAssignments = {
                        nav.navigate(NavRoutes.ADMIN_ASSIGNMENTS)
                    },
                    onTemplatesAdmin = {
                        nav.navigate(NavRoutes.ADMIN_TEMPLATES_ADMIN)
                    }
                )
            } else {
                // Fallback a versión original
                AdminTemplateListScreen(
                    vm = adminVM,
                    onCreateTemplate = {
                        nav.navigate(NavRoutes.adminTemplateForm())
                    },
                    onEditTemplate = { templateId: Long ->
                        nav.navigate(NavRoutes.adminTemplateForm(templateId))
                    },
                    onViewTemplate = { templateId: Long ->
                        nav.navigate(NavRoutes.adminTemplateForm(templateId))
                    }
                )
            }
        }

        // Formulario de template admin - Solo para administradores
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
                // Redirigir a HOME si no es admin
                nav.navigate(NavRoutes.HOME) { 
                    popUpTo(NavRoutes.ADMIN_TEMPLATE_FORM) { inclusive = true }
                }
                return@composable
            }
            val templateIdString = backStack.arguments?.getString("templateId") ?: "-1"
            val templateId = templateIdString.toLongOrNull()?.takeIf { it != -1L }

            AdminTemplateFormScreen(
                vm = adminVM,
                templateId = templateId,
                onBack = { nav.popBackStack() },
                onSaved = { 
                    // Refrescar la lista después de guardar
                    adminVM.loadTemplates()
                    nav.popBackStack() 
                },
                onCreateItem = { templateId ->
                    nav.navigate(NavRoutes.adminItemForm(templateId))
                },
                onEditItem = { templateId, itemId ->
                    nav.navigate(NavRoutes.adminItemForm(templateId, itemId))
                }
            )
        }

        // Formulario de item admin - Solo para administradores
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
            if (!isAdmin) {
                // Redirigir a HOME si no es admin
                nav.navigate(NavRoutes.HOME) { 
                    popUpTo(NavRoutes.ADMIN_ITEM_FORM) { inclusive = true }
                }
                return@composable
            }
            
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
                onSaved = { 
                    // Refrescar la lista después de guardar
                    adminVM.loadTemplates()
                    nav.popBackStack() 
                }
            )
        }

        // Pantalla de asignaciones - Solo para administradores
        composable(NavRoutes.ADMIN_ASSIGNMENTS) {
            if (!isAdmin) {
                // Redirigir a HOME si no es admin; limpiar backstack hasta esta ruta
                nav.navigate(NavRoutes.HOME) {
                    popUpTo(NavRoutes.ADMIN_ASSIGNMENTS)
                    launchSingleTop = true
                }
                return@composable
            }
            
            AssignmentScreen(
                assignmentVM = assignmentVM,
                onBack = { nav.popBackStack() }
            )
        }

        // Pantalla de Templates Admin - Solo para administradores
        composable(NavRoutes.ADMIN_TEMPLATES_ADMIN) {
            if (!isAdmin) {
                // Redirigir a HOME si no es admin; limpiar backstack hasta esta ruta
                nav.navigate(NavRoutes.HOME) {
                    popUpTo(NavRoutes.ADMIN_TEMPLATES_ADMIN)
                    launchSingleTop = true
                }
                return@composable
            }
            
            TemplatesAdminScreen(
                adminVM = adminVM,
                onCreateTemplate = {
                    nav.navigate(NavRoutes.adminTemplateForm())
                },
                onEditTemplate = { templateId: Long ->
                    nav.navigate(NavRoutes.adminTemplateForm(templateId))
                },
                onViewTemplate = { templateId: Long ->
                    nav.navigate(NavRoutes.adminTemplateForm(templateId))
                },
                onBack = { nav.popBackStack() }
            )
        }
    }
}
