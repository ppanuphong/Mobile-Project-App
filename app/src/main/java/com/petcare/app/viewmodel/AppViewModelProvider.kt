package com.petcare.app.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.petcare.app.PetCareApp
import com.petcare.app.di.AppContainer

/** Factory กลางสำหรับสร้าง ViewModel ทุกตัวพร้อม repository จาก [AppContainer] */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            SessionViewModel(
                container().authRepository,
                container().petRepository,
                container().appointmentRepository,
                container().reminderScheduler,
            )
        }
        initializer { LoginViewModel(container().authRepository) }
        initializer { RegisterViewModel(container().authRepository) }
        initializer {
            HomeViewModel(container().petRepository, container().appointmentRepository, container().sessionManager)
        }
        initializer { PetViewModel(container().petRepository, container().appointmentRepository) }
        initializer { PetFormViewModel(createSavedStateHandle(), container().petRepository) }
        initializer {
            PetDetailViewModel(createSavedStateHandle(), container().petRepository, container().appointmentRepository)
        }
        initializer { AppointmentViewModel(container().petRepository, container().appointmentRepository) }
        initializer {
            AppointmentFormViewModel(createSavedStateHandle(), container().petRepository, container().appointmentRepository)
        }
        initializer {
            NotificationViewModel(container().petRepository, container().appointmentRepository, container().sessionManager)
        }
    }
}

private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as PetCareApp).container
