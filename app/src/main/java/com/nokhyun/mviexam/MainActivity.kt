package com.nokhyun.mviexam

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), IView<MovieState, MovieSideEffect> {

    private val main by lazy { findViewById<ConstraintLayout>(R.id.main) }
    private val tvMovies by lazy { findViewById<TextView>(R.id.tvMovies) }
    private val btnAction by lazy { findViewById<Button>(R.id.btnAction) }
    private val mainViewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnAction.setOnClickListener {
            lifecycleScope.launch {
                mainViewModel.intents.send(MovieIntent.SearchMovie)
            }
        }

        mainViewModel.state.observe(this) {
            render(it)
        }

        mainViewModel.navigation.observe(this) {
            navigate(it)
        }
    }

    override fun render(state: MovieState) {
        with(state) {
            tvMovies.text = movies.toString()

            if (errorMessage != null) {
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun navigate(from: String) {
        // TODO navigateToActivity
    }

}

class MainViewModel(
    savedInstanceState: SavedStateHandle,
    private val movieRepository: MovieRepository = MovieRepositoryImpl()
) : ViewModel(), IModel<MovieState, MovieIntent, MovieSideEffect> {
    override val intents: Channel<MovieIntent> = Channel(Channel.UNLIMITED)
    override val sideEffect: Channel<MovieSideEffect> = Channel(Channel.UNLIMITED)

    private val _state: MutableLiveData<MovieState> = MutableLiveData<MovieState>()
    override val state: LiveData<MovieState> get() = _state

    private val _navigation: MutableLiveData<String> = MutableLiveData<String>()
    val navigation: LiveData<String> get() = _navigation

    init {
        intentConsumer()
    }

    private fun intentConsumer() {
        viewModelScope.launch {
            intents.consumeAsFlow().collect { movieIntent ->
                when (movieIntent) {
                    MovieIntent.SearchMovie -> fetchData()
                    MovieIntent.NavigateToActivity -> sideEffect.send(MovieSideEffect.NavigateToActivity)
                }
            }
        }
    }

    private fun fetchData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateState { it.copy(isLoading = true, errorMessage = null) }

                flow {
                    emit(movieRepository.getList())
                }.collect { movies ->
                    if (movies.isNotEmpty()) {
                        updateState {
                            it.copy(isLoading = false, movies = movies, errorMessage = null)
                        }
                    } else {
                        updateState {
                            it.copy(isLoading = false, errorMessage = "couldn't find the movie")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun updateState(handler: suspend (intent: MovieState) -> MovieState) {
        _state.postValue(handler(state.value ?: MovieState()))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val savedInstanceState = createSavedStateHandle()
                val movieRepository: MovieRepository = MovieRepositoryImpl()
                MainViewModel(savedInstanceState, movieRepository)
            }
        }
    }
}

interface MovieRepository {
    fun getList(): List<MovieEntity>
}

class MovieRepositoryImpl : MovieRepository {
    override fun getList(): List<MovieEntity> = mutableListOf<MovieEntity>().apply {
        repeat(5) {
            add(MovieEntity(name = "가$it"))
        }
    }
}