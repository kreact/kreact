# KReact
[![Build and Test](https://github.com/kreact/kreact/actions/workflows/build-and-test.yml/badge.svg?branch=main)](https://github.com/kreact/kreact/actions/workflows/build-and-test.yml)

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
This library can be used in a variety of applications in both clients and servers that use Kotlin.

## General
You are required to set up a set of components for any use case. These are:
* Actions -> `org.kreact.core.Action`
* Side effects -> `org.kreact.core.SideEffect`
* State -> `org.kreact.core.State`
* Reducer functions -> `org.kreact.core.Reducer.ReducerFunctionType`

The components can be as complex or as simple as you want them to be.

Below is a basic example for each:

### Action
```kotlin
sealed class MyAction: Action {
    object Start: MyAction()
    object Stop: MyAction()
}
```

### Side Effect
```kotlin
sealed class MySideEffect: SideEffect
```

### State
```kotlin
data class MyState(val property1: String? = null): State
```

### Reducer Function
```kotlin
typealias MyReducerType = suspend (MyAction, MyState) -> ReducerResult<MyState, MySideEffect>

object MyReducer : MyReducerType {
    override suspend fun invoke(
        action: MyAction,
        state: MyState
    ): ReducerResult<MyState, MySideEffect> {
        TODO()
    }
}
```

You then combine all those components to create a `StateProvider` and `ActionDispatcher` pair using 
`StateProviderFactory`:

```kotlin
val coroutineScope = CoroutineScope(Dispatchers.Default + Job())
val (actionDispatcher, stateProvider) =
            StateProviderFactory.create(coroutineScope, MyState(), MyReducer)
```

You can then use the action dispatcher like so:
```kotlin
    /**
     * Dispatches an action to the flow for processing by the reducer.
     *
     * @param action The action to dispatch.
     */
    suspend fun dispatch(action: A)

    /**
     * Dispatches an action to the channel for processing and waits for the result from the reducer.
     *
     * @param action The action to dispatch with suspension.
     */
    suspend fun dispatchAndAwait(action: A)
```

And once an action is dispatched you can then collect on the state and side effect flows:

```kotlin
/**
 * An abstract class that provides a flow of states and side effects.
 *
 * @param S The state to provide
 * @param E The possible side effects during state mutation
 *
 * @property stateFlow is a hot stream of [State] values emitted once and only once to its
 * collectors. It has a default initial state value.
 * @property sideEffectFlow is a hot stream of [SideEffect] values emitted once or more times to its
 * collectors, dependent on the replay value during collection, and has no initial value.
 */
abstract class StateProvider<S : State, E : SideEffect> {
    abstract val stateFlow: StateFlow<S>
    abstract val sideEffectFlow: SharedFlow<E>
}

stateFlow.collect {
    TODO()
}

sideEffectFlow.collect {
    TODO()
}
```

## Client - Android
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
    initialState: ExampleState,
    vararg reducerFunctions: ExampleReducerType
) : BaseViewModel<ExampleAction, ExampleState, ExampleSideEffect>(initialState, *reducerFunctions)
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
    private val initialState: ExampleState,
    private vararg val reducerFunctions: ExampleReducerType
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExampleViewModel::class.java)) {
            return ExampleViewModel(initialState, *reducerFunctions) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

### MainScreen
Now you can create your viewmodel and it will be seamlessly integrated with KReact.
```kotlin
@Preview
@Composable
fun MainScreen() {
    val viewModel: ExampleViewModel = viewModel(
        factory = ExampleViewModelFactory(ExampleState(), ExampleReducer)
    )
    val state = viewModel.state()
    if (state.showProgressBar) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary)
                .align(Alignment.BottomCenter)
        )
    }
    Button(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        onClick = {
            coroutineScope.launch {
                viewModel.dispatch(
                    TODO("Dispatch some relevant action here that potentially changes the state")
                )
            }
        }) {
        Text(text = "Send Action")
    }
}
```

## Server
To fully harness the power of coroutines use an HTTP server like Ktor that makes full use of coroutines. Other HTTP 
servers may be viable but are not tested. The library can be used for both stateful and stateless services.

### Stateful Example
I will not be providing a stateful example since it is trivial to set up and the General section above should send you
on the right path. Just ensure that the created `ActionDispatcher` and `StateProvider` pair are accessible to your 
service class during its lifecycle, including the coroutine scope needed by `StateProviderFactory`.

### Stateless Example
For the stateless example I will be using Ktor and utilize its `pipelineContext` to maintain structured concurrency.
What we want is that for each call made to a URL or endpoint, we initialize a new state, therefore allowing any call 
made to the service to utilize common state properties, condensing the data needed to process a request into a single 
source of truth.

### Setup State
```kotlin
data class ServiceState(
    val apiCall: ApplicationCall
): State {
    suspend fun <T : Any?> apiCallScope(scope: suspend ApplicationCall.() -> T?): T? {
        return withContext(Dispatchers.IO) {
            scope(apiCall)
        }
    }
}

```

### Setup Actions & Side Effects
```kotlin
sealed class ServiceAction : Action {
    sealed class AuthFlow : PluginServiceAction() {
        data class Start(
            val id1: String?,
            val id2: String?
        ) : AuthFlow() {
            companion object {
                const val endpoint = "/{id1}/auth/start"
            }
        }
    }
}
```
```kotlin
sealed class ServiceSideEffect : SideEffect
```

### Setup Reducer
```kotlin
private typealias ServiceReducerType =
        suspend (ServiceAction, ServiceState) -> ReducerResult<ServiceState, ServiceSideEffect>

object ServiceReducer : ServiceReducerType {

    override suspend fun invoke(
        action: ServiceAction,
        state: ServiceState
    ): ReducerResult<ServiceState, ServiceSideEffect> {
        return when (action) {
            is ServiceAction.AuthFlow.Start -> {
                if (action.id1.isNullOrEmpty()) {
                    state.apiCallScope {
                        respond(HttpStatusCode.BadRequest, "Missing ${action::id1.name}")
                    }
                    return ReducerResult.NoMutation()
                }
                if (action.id2.isNullOrEmpty()) {
                    state.apiCallScope {
                        respond(HttpStatusCode.BadRequest, "Missing ${action::id2.name}")
                    }
                    return ReducerResult.NoMutation()
                }

                state.apiCallScope {
                    respondRedirect(authorizationUrl.toString())
                }

                ReducerResult.NoMutation()
            }
        }
    }
}
```

### Create the State
Using the components created above we now are able to create our state using a function like the one below.
This function uses `pipelineContext.coroutineContext` to create a new coroutine for a call, maintaining structured 
concurrency. In addition, `pipelineContext.call` is injected into the service state to be used by the reducer later on
so it can set an HTTP status code or response body.
```kotlin
suspend fun initServiceState(
    pipelineContext: PipelineContext<Unit, ApplicationCall>,
    scope: suspend ActionDispatcher<ServiceAction, ServiceState, ServiceSideEffect>.() -> Unit
) {
    val pair = StateProviderFactory.create(
        CoroutineScope(pipelineContext.coroutineContext),
        ServiceState(pipelineContext.call),
        ServiceReducer
    )
    scope(pair.first)
}
```
Note: By providing an `ActionDispatcher` in the scope we allow further use of the function, in a declarative a fashion.

### Setup Ktor
Now that we can create a state on the fly we can finally integrate with Ktor.
```kotlin
embeddedServer(Netty, host = "0.0.0.0", port = port) {
    routing {
        get(ServiceAction.AuthFlow.Start.url) {
            initServiceState(this) {
                dispatchAndAwait(
                    ServiceAction.AuthFlow.Start(
                        call.request.queryParameters["moo"],
                        call.parameters["meow"],
                    )
                )
            }
        }
    }
}
```
Note: Coroutines are cheap and Ktor creates them on the fly for each request. `dispatchAndAwait` allows Ktor to await 
until a reducer handles the action and returns a `ReducerResult`. Usage of `dispatch` with Ktor will result in Ktor not 
knowing the status of a response and appearing to hang. Once an action is dispatched without suspension, the Job 
associated with this route completes without the reducer having a chance to properly populate `pipelineContext.call`. 
