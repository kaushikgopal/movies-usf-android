package co.kaush.msusf.movies

import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import co.kaush.msusf.MSApp

class MSMainVm(app: MSApp) : AndroidViewModel(app) {

    

}

// -----------------------------------------------------------------------------------

class MSMainVmFactory(
        private val app: MSApp
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MSMainVm(app) as T
    }
}
