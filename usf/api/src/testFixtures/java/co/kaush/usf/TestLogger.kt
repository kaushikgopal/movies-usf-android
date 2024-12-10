package co.kaush.usf

object TestLogger : UsfViewModelImpl.UsfVmLogger {
    override fun debug(message: String) = println(message)

    override fun warning(message: String) = println(message)

    override fun error(error: Throwable, message: String) = println(message)
}
