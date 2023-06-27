package co.kaush.msusf.annotations

@Target(AnnotationTarget.CLASS) // targeting classes
@Retention(AnnotationRetention.SOURCE) // valid in compile time and removed in binary output
@MustBeDocumented // allows annotation to be included in generated docs
annotation class ViewModel
