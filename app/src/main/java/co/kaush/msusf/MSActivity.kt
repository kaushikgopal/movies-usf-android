package co.kaush.msusf

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import javax.inject.Inject

abstract class MSActivity : AppCompatActivity() {

    val app: MSApp by lazy { application as MSApp }

    @Inject lateinit var ctx: Context

    abstract fun inject(activity: MSActivity)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(this)
    }
}