package co.kaush.msusf

import io.reactivex.observers.TestObserver

fun <T> TestObserver<T>.assertLastValue(predicate: (T) -> Boolean) {
    assertValueAt(valueCount() - 1) { predicate.invoke(it) }
}