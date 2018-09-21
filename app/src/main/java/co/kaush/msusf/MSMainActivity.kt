package co.kaush.msusf

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MSMainActivity : MSActivity() {

    override fun inject(activity: MSActivity) {
        app.appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
         ms_mainScreen_title.text = ctx.resources.getString(R.string.app_name)
    }
}
