package com.arranlomas.kontent.commons

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Created by arran on 4/12/2017.
 */
open class KontentInteractor<S : KontentContract.ViewState, E : KontentContract.Intent> : KontentContract.Interactor<S, E> {

    val intentsSubject: PublishSubject<E> = PublishSubject.create()
    val stateSubject: PublishSubject<S> = PublishSubject.create()

    lateinit var processStream: (E) -> Observable<S>
    lateinit var processor: (Observable<E>) -> Observable<S>

    override fun attachView(intents: Observable<E>): Observable<S> {
        intents.subscribe(intentsSubject)
        processor.invoke(intents)
                .subscribe(stateSubject)
        return stateSubject
    }

}