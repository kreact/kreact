# KReact
[![Build and Test](https://github.com/kreact/kreact/actions/workflows/build-and-test.yml/badge.svg?branch=main)](https://github.com/kreact/kreact/actions/workflows/build-and-test.yml)

KReact is a Kotlin-based framework designed for managing application state in a reactive and structured manner. 
It employs a redux-like pattern, centralizing the state management logic. The key components of this library are:

* `Action`: Represents discrete operations or events that trigger state changes.
* `State`: The core of the application's data structure, which is immutable and reflects the current status of the 
application.
* `SideEffect`: Handles operations that are not directly related to the state mutation, such as logging or database 
interactions, ensuring a clear separation of concerns.
* `ActionDispatcher`: A conduit for dispatching actions either synchronously or asynchronously, facilitating state 
mutations.
* `Reducer`: The heart of the state mutation logic, where actions are processed to produce a new, immutable state.
* `StateProvider`: Offers a reactive stream of state and side effects, enabling components to respond dynamically to 
state changes.

This architecture promotes a clean, maintainable, and scalable codebase, ideal for complex applications requiring a 
structured approach to state management, while simplifying testing and improving the testing surface.

The design is based on these principles:
1. Reactive
2. Scalable
3. Modular
4. Minimal

This framework can be used in a variety of applications both in clients and in servers. Servers are a particularly
interesting use case especially with the rise of stateless services. All software has internal state one way or another
whether it exists in the form of constants or database connections. KReact helps centralize the data so that you can
focus on the logic and not worry about how to access things you need to process a request. See below for some concrete
examples.

## Use
### Gradle
```kotlin
dependecies {
    implementation("org.kreact.kreact:1.0.0")
}
```

### Maven
```xml
<dependency>
  <groupId>org.kreact</groupId>
  <artifactId>kreact</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Quick Start
You are required to set up a set of components for any use case. These are:
* Action: Represents an operation to change the state.
* State: Represents the application state.
* SideEffect: Represents a side effect operation after mutating the state like logging, database interactions etc.
* ActionDispatcher: Dispatches actions to change the state.
* Reducer: Processes actions to produce a new state.
* StateProvider: Provides a flow of states and side effects.

The components can be as complex or as simple as you want them to be.

Here is a simple example to get started quickly:

### Define your components
#### Step 1: Define State, Actions & Side Effects
```kotlin
// Define your application state
class AppState(val counter: Int = 0) : State

// Define actions that can change the state
class IncrementAction : Action
class DecrementAction : Action

// Define any side effects (e.g., logging)
class LogSideEffect(val message: String) : SideEffect
```
#### Step 2: Define Reducer Functions
```kotlin
// Reducer function to handle actions
val counterReducer: ReducerFunctionType<Action, AppState, SideEffect> = { action, state, dispatcher ->
    when (action) {
        is IncrementAction -> ReducerResult.Mutation(state.copy(counter = state.counter + 1))
        is DecrementAction -> ReducerResult.Mutation(state.copy(counter = state.counter - 1))
        else -> ReducerResult.NoMutation()
    }
}
```
#### Step 3: Initialize State Management
Using the components we defined we can now create our `ActionDispatcher` and `StateProvider`
```kotlin
// Coroutine scope for the reducer (usually a ViewModelScope or similar)
val scope = CoroutineScope(Dispatchers.Default)

// Initial state of the app
val initialState = AppState()

// Create the ActionDispatcher and StateProvider
val (actionDispatcher, stateProvider) = StateProviderFactory.create(
    scope,
    initialState,
    counterReducer
)
```
#### Step 4: Dispatch Actions and Observe State Changes
```kotlin
// Dispatch actions
scope.launch {
    actionDispatcher.dispatch(IncrementAction())
    actionDispatcher.dispatch(DecrementAction())
}

// Observe state changes
stateProvider.stateFlow.collect { state ->
    println("Current counter value: ${state.counter}")
}
```
#### Step 5: Observe Side Effects (Optional)
```kotlin
// Side effect example (e.g., logging)
stateProvider.sideEffectFlow.collect { sideEffect ->
    if (sideEffect is LogSideEffect) {
        println("Log: ${sideEffect.message}")
    }
}
```


## Concrete Examples
Below are some concrete examples and use cases. I am providing these while I create a better way to showcase examples
like a dedicated folder and projects.

### Client - Android
In order for the state to be preserved in mobile you need to integrate the viewmodel of your application with KReact.

#### Define a ViewModel with KReact
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

#### Extend the base ViewModel with KReact
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

#### Create a ViewModelFactory for the ViewModel
You will need a `ViewModelFactory` since the ViewModel we defined has constructor parameters.
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

#### Use the ViewModel in your composable
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

### Server
To fully harness the power of coroutines use an HTTP server like Ktor that makes full use of coroutines. Other HTTP 
servers may be viable but are not tested. The library can be used for both stateful and stateless services.

#### Stateful
I will not be providing a stateful example since it is trivial to set up and the `Quick Start` section above should
provide a generalized example. Just ensure that the created `ActionDispatcher` and `StateProvider` pair are accessible 
to your service class during its lifecycle, including the coroutine scope needed by `StateProviderFactory`.

#### Stateless
For the stateless example I will be using Ktor and utilize its `pipelineContext` to maintain structured concurrency.
What we want is that for each call made to a URL or endpoint, we initialize a new state, therefore allowing any call 
made to the service to utilize common state properties, condensing the data needed to process a request into a single 
source of truth.

#### Setup State
```kotlin
data class ServiceState(
    val apiCall: ApplicationCall
): State {
    suspend fun <T : Any?> apiCallScope(scope: suspend ApplicationCall.() -> T?): T? {
        return withContext(Dispatchers.IO) { // Dispatcher here can be whatever I just chose IO as an example
            scope(apiCall)
        }
    }
}

```

#### Setup Actions & Side Effects
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

#### Setup Reducer
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

#### Create the State
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

#### Setup Ktor
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
