package com.arranlomas.kontent.commons.objects

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.observers.DisposableObserver
import io.reactivex.subjects.BehaviorSubject

interface KontentContract {

    interface View<I : KontentIntent, A : KontentAction, R : KontentResult, S : KontentViewState> {
        fun render(state: S)
        var viewModel: KontentContract.ViewModel<I, A, R, S>
        val subscriptions: CompositeDisposable
        var onErrorAction: ((Throwable) -> Unit)?

        val intentSubscriber: DisposableObserver<S>

        fun setup(viewModel: KontentContract.ViewModel<I, A, R, S>, onErrorAction: ((Throwable) -> Unit)? = null) {
            this.viewModel = viewModel
            this.onErrorAction = onErrorAction
        }

        fun <T : I> attachIntents(intents: Observable<I>, initialIntent: Class<T>) {
            viewModel.attachView(intents, initialIntent)
                    .subscribeWith(intentSubscriber)
                    .addDisposable()
        }

        fun attachIntents(intents: Observable<I>) {
            viewModel.attachView(intents)
                    .subscribeWith(intentSubscriber)
                    .addDisposable()
        }

        private fun Disposable.addDisposable() {
            subscriptions.add(this)
        }
    }

    abstract class BaseSubscriber<A>(val onErrorAction: ((Throwable) -> Unit)?, val showLoading: Boolean = true) : DisposableObserver<A>() {
        override fun onError(e: Throwable) {
            onErrorAction?.invoke(e) ?: e.printStackTrace()
        }

        override fun onStart() {
        }

        override fun onComplete() {
        }
    }

    interface ViewModel<I : KontentIntent, A : KontentAction, R : KontentResult, S : KontentViewState> {
        val stateSubject: BehaviorSubject<S>

        var intentFilter: ObservableTransformer<I, I>?

        val intentToAction: (I) -> A
        val actionProcessor: ObservableTransformer<A, R>
        val defaultState: S
        val reducer: BiFunction<S, R, S>
        val postProcessor: (Function1<S, S>)?

        fun <T : I> attachView(intents: Observable<I>, initialIntent: Class<T>): Observable<S> {
            return attachViewMethod(intents,
                    stateSubject,
                    intentToAction,
                    actionProcessor,
                    reducer,
                    { getLastState() },
                    postProcessor,
                    intentFilter)
        }

        fun attachView(intents: Observable<I>): Observable<S> {
            return attachViewMethod(intents,
                    stateSubject,
                    intentToAction,
                    actionProcessor,
                    reducer,
                    { getLastState() },
                    postProcessor,
                    null)
        }

        fun getLastState(): S = stateSubject.value ?: defaultState
    }
}

fun <I, T : I> getInitialIntentFilter(clazz: Class<T>): ObservableTransformer<I, I> {
    var emittedCount = 0
    return ObservableTransformer {
        it.filter { (clazz.isInstance(it) && emittedCount < 1) || !clazz.isInstance(it) }
                .map {
                    if (clazz.isInstance(it)) emittedCount++
                    it
                }
    }
}

internal fun <I, A, R, S> attachViewMethod(intents: Observable<I>,
        subscriber: BehaviorSubject<S>,
        intentToAction: (I) -> A,
        actionProcessor: ObservableTransformer<A, R>,
        reducer: BiFunction<S, R, S>,
        getLastState: () -> S,
        postProcessor: (Function1<S, S>)?,
        intentFilter: ObservableTransformer<I, I>?): Observable<S> {
    val obs = intentFilter.let { intents.compose(intentFilter) } ?: intents
    obs.applyMvi(intentToAction, actionProcessor, reducer, getLastState, postProcessor)
            .filterOnComplete()
            .subscribe(subscriber)
    return subscriber
}

internal fun <I, A, R, S> Observable<I>.applyMvi(intentToAction: (I) -> A,
        actionProcessor: ObservableTransformer<A, R>,
        reducer: BiFunction<S, R, S>,
        getLastState: () -> S,
        postProcessor: (Function1<S, S>)? = null
): Observable<S> {
    return this.map { intent -> intentToAction.invoke(intent) }
            .compose(actionProcessor)
            .scan(getLastState.invoke(), reducer)
            .map { postProcessor?.invoke(it) ?: it }
}

internal fun <S> Observable<S>.filterOnComplete(): Observable<S> {
    return this.materialize()
            .filter { !it.isOnComplete }
            .dematerialize<S>()
}