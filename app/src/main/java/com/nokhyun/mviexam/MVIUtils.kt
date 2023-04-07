package com.nokhyun.mviexam

import androidx.lifecycle.LiveData
import kotlinx.coroutines.channels.Channel

/** Activity, Fragment 사용 */
interface IIntent
interface ISideEffect
interface IState

/** 화면단 처리 */
interface IView<S : IState, SE : ISideEffect> {
    fun render(state: S)
    fun navigate(from: String)
}

/** Intent(사용자 동작)에 따른 View 를 변경하기 위한 값들 */
interface IModel<S : IState, I : IIntent, SE : ISideEffect> {
    val intents: Channel<I>
    val sideEffect: Channel<SE>
    val state: LiveData<S>
}

/** 사용자 액션 */
sealed interface MovieIntent : IIntent {
    object SearchMovie : MovieIntent
    object NavigateToActivity : MovieIntent
}

/** 액션 후 동작 */
sealed interface MovieSideEffect : ISideEffect {
    object NavigateToActivity : MovieSideEffect
}

/** 결과 값에 대한 상태 */
data class MovieState(
    val movies: List<MovieEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : IState

interface Entity
data class MovieEntity(val name: String) : Entity