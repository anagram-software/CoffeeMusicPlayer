package com.udeshcoffee.android.utils

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.*


/**
 * Created by Udathari on 9/24/2017.
 */
class ObservableList<T> {

    val list: ArrayList<T> = ArrayList()
    private val subject: BehaviorSubject<List<T>> = BehaviorSubject.create()

    val observable: Observable<List<T>>
        get() { return subject }

    fun add(position: Int? = null, t: T) {
        if (position == null)
            list.add(t)
        else
            list.add(position, t)
        subject.onNext(list)
    }

    fun addAll(list: List<T>){
        this.list.addAll(list)
        subject.onNext(this.list)
    }

    fun addAll(position: Int, list: List<T>){
        if (this.list.isEmpty()) {
            this.list.addAll(list)
        } else {
            this.list.addAll(position, list)
        }
        subject.onNext(this.list)
    }

    fun clearAndAddAll(list: List<T>){
        this.clear()
        this.list.addAll(list)
        subject.onNext(this.list)
    }

    fun clear() {
        list.clear()
        subject.onNext(list)
    }

    fun move(fromPosition: Int, toPosition: Int) {
        val item = list.getOrNull(fromPosition)
        item?.let {
            list.removeAt(fromPosition)
            list.add(toPosition, it)
            subject.onNext(list)
        }
    }

    fun remove(position: Int) {
        list.removeAt(position)
        subject.onNext(list)
    }

    val size: Int
        get() = list.size

    fun isEmpty(): Boolean = list.isEmpty()

    fun isNotEmpty(): Boolean = list.isNotEmpty()

    fun getOrNull(position: Int): T? = list.getOrNull(position)
}