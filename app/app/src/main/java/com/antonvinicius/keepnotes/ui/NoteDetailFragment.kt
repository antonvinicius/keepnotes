package com.antonvinicius.keepnotes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.antonvinicius.keepnotes.R
import com.antonvinicius.keepnotes.databinding.FragmentNoteDetailBinding
import com.antonvinicius.keepnotes.repository.NoteRepository
import com.antonvinicius.keepnotes.retrofit.ApiResult
import com.antonvinicius.keepnotes.retrofit.RetrofitInstance
import com.antonvinicius.keepnotes.retrofit.WebClient
import com.antonvinicius.keepnotes.ui.viewmodel.NoteDetailViewModel
import com.antonvinicius.keepnotes.ui.viewmodel.NoteDetailViewModelFactory
import com.antonvinicius.keepnotes.util.setTitle
import com.antonvinicius.keepnotes.util.showErrorMessage
import com.antonvinicius.keepnotes.util.showSuccessMessage
import kotlinx.coroutines.launch

class NoteDetailFragment : Fragment(), MenuProvider {
    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    private val noteId by lazy {
        arguments?.getString(NOTE_ID_KEY)
    }

    private val viewModel by viewModels<NoteDetailViewModel>(factoryProducer = {
        NoteDetailViewModelFactory(
            NoteRepository(WebClient(RetrofitInstance())), noteId
        )
    })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.remove.setOnClickListener {
            lifecycleScope.launch {
                viewModel.noteRemove()?.observe(viewLifecycleOwner) { result ->
                    when (result) {
                        is ApiResult.Error -> {
                            showErrorMessage(result.error)
                        }

                        is ApiResult.Loading -> {}
                        is ApiResult.Success -> {
                            showSuccessMessage()
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        viewModel.noteLiveData.observe(viewLifecycleOwner) { result ->
            result.data?.let {
                setTitle(it.title)
                binding.title.text = it.title
                binding.content.text = it.content
            }

            when (result) {
                is ApiResult.Error -> {
                    showErrorMessage(result.error)
                }

                is ApiResult.Loading -> {}

                is ApiResult.Success -> {
                    showSuccessMessage()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.noteFindById()
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.note_detail_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.update_action -> {
                findNavController().navigate(
                    R.id.action_NoteDetailFragment_to_NoteCreateOrEditFragment, bundleOf(
                        NOTE_ID_KEY to noteId
                    )
                )
            }

            else -> {
                findNavController().navigateUp()
            }
        }

        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val NOTE_ID_KEY = "noteId"
    }
}