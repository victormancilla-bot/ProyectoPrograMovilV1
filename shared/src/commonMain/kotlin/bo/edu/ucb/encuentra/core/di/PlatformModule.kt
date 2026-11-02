package bo.edu.ucb.encuentra.core.di

import org.koin.core.module.Module

/** Lo que depende de la plataforma: la base de datos (Context en Android, carpeta Documents en iOS). */
expect fun platformModule(): Module
