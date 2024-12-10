package co.kaush.usf.fixtures

import co.kaush.usf.UsfViewModelImpl

object TestLogger : UsfViewModelImpl.UsfVmLogger {
    override fun debug(message: String) = println(message)

    override fun warning(message: String) = println(message)

    override fun error(error: Throwable, message: String) = println(message)
}
