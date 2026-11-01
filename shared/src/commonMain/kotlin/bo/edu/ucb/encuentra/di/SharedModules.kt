package bo.edu.ucb.encuentra.di

import bo.edu.ucb.encuentra.feature.alerts.di.alertsModule
import bo.edu.ucb.encuentra.feature.audit.di.auditModule
import bo.edu.ucb.encuentra.feature.claims.di.claimsModule
import bo.edu.ucb.encuentra.feature.dashboard.di.dashboardModule
import bo.edu.ucb.encuentra.feature.delivery.di.deliveryModule
import bo.edu.ucb.encuentra.feature.foundcapture.di.foundCaptureModule
import bo.edu.ucb.encuentra.feature.foundcatalog.di.foundCatalogModule
import bo.edu.ucb.encuentra.feature.foundform.di.foundFormModule
import bo.edu.ucb.encuentra.feature.home.di.homeModule
import bo.edu.ucb.encuentra.feature.inventory.di.inventoryModule
import bo.edu.ucb.encuentra.feature.login.di.loginModule
import bo.edu.ucb.encuentra.feature.matching.di.matchingModule
import bo.edu.ucb.encuentra.feature.myreports.di.myReportsModule
import bo.edu.ucb.encuentra.feature.profile.di.profileModule
import bo.edu.ucb.encuentra.feature.reportwizard.di.reportWizardModule
import bo.edu.ucb.encuentra.feature.verify.di.verifyModule
import org.koin.core.module.Module

// ⚠️ Archivo congelado desde F01: cada feature registra lo suyo en su propio XxxModule.kt.
fun sharedModules(): List<Module> = listOf(
    // A · Plataforma, datos y panel admin
    dashboardModule,
    claimsModule,
    // B · Acceso, seguridad y verificación
    loginModule,
    verifyModule,
    deliveryModule,
    auditModule,
    // C · Reporte de perdido y notificaciones
    homeModule,
    reportWizardModule,
    myReportsModule,
    foundCatalogModule,
    alertsModule,
    // D · Hallazgo con cámara e IA de visión
    foundCaptureModule,
    foundFormModule,
    inventoryModule,
    // E · Design System, matching y mapas
    matchingModule,
    profileModule
)
