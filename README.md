# KReact
This library provides a Redux-like implementation in pure Kotlin and coroutines.

# Include
## Gradle
```kotlin
dependecies {
    implementation("org.kreact.kreact:1.0")
}
```

## Maven
```xml
<dependency>
  <groupId>org.kreact</groupId>
  <artifactId>kreact</artifactId>
  <version>1.0</version>
</dependency>
```

# Use 
This library can be used in a variety of applications.

## Mobile
In order for the state to be preserved in mobile you need to integrate the viewmodel of your application with KReact.

### BaseViewModel
```kotlin
/**
 * Integration of the Android ViewModel allowing for seamless state
 * collection and action firing from the viewmodel itself. It allows the state to be preserved
 * during configuration changes or recomposition due to [viewModelScope] tied to ViewModels
 * lifecycle which is longer than any Android UI component such as an Activity or composable.
 *
 * This base class is to be extended by the apps view models.
 */
abstract class BaseViewModel<A : Action, S : State, E : SideEffect>(
    private val initialState: S,
    vararg reducerFunctions: ReducerFunctionType<A, S, E>
) : ViewModel() {
    private val actionDispatcher: ActionDispatcher<A, S, E>
    private val stateProvider: StateProvider<S, E>

    init {
        val (actionDispatcher, stateProvider) =
            StateProviderFactory.create(viewModelScope, initialState, *reducerFunctions)
        this.actionDispatcher = actionDispatcher
        this.stateProvider = stateProvider
    }

    suspend fun dispatch(action: A) {
        actionDispatcher.dispatch(action)
    }

    @Composable
    fun state(): S {
        return stateProvider.stateFlow.collectAsState(initialState).value
    }
}
```
You can now extend from this class:

### ExampleViewModel
```kotlin
/**
 * The ViewModel used in the app.
 *
 * @param initialState used to initialize the state
 * @param reducerFunctions which are used to transform an action into a state mutation. One or many
 * can be used depending on complexity and need.
 *
 * The generic nature of [StateProvider] and [ActionDispatcher] and their scoping to a specific
 * coroutine context (in this case viewModelScope) allows for many reducers to be
 * defined and thus also many viewmodels depending on complexity and need.
 */
class ExampleViewModel(
    initialState: CommonState,
    vararg reducerFunctions: CommonReducerType
) : BaseViewModel<CommonAction, CommonState, CommonSideEffect>(initialState, *reducerFunctions)
```

However, you will need a `ViewModelFactory`:

### ExampleViewModelFactory
```kotlin
/**
 * Since the viewmodel has constructor params this class is needed to instruct the Android
 * framework how to handle the creation of the non-standard ViewModel instances in an Android UI
 * component or composable.
 */
class ExampleViewModelFactory(
    private val initialState: CommonState,
    private vararg val reducerFunctions: CommonReducerType
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommonViewModel::class.java)) {
            return ExampleViewModel(initialState, *reducerFunctions) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```
