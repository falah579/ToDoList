package com.falah.todolist.fragments.list

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.falah.todolist.R
import com.falah.todolist.SwipeToDelete
import com.falah.todolist.adapter.ListAdapter
import com.falah.todolist.data.model.ToDoData
import com.falah.todolist.databinding.FragmentListBinding
import com.falah.todolist.fragments.SharedViewModel
import com.falah.todolist.viewmodel.ToDoViewModel
import jp.wasabeef.recyclerview.animators.LandingAnimator
import kotlinx.android.synthetic.main.fragment_list.*
import kotlinx.android.synthetic.main.fragment_list.view.*


class ListFragment : Fragment() , SearchView.OnQueryTextListener{

    private val mSharedViewModel: SharedViewModel by viewModels()
    private val mTodoViewModel: ToDoViewModel by viewModels()
    private val listAdapter: ListAdapter by lazy { ListAdapter() }

    
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_list, container, false)

        _binding = FragmentListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.mSharedViewModel = mSharedViewModel


        setupRecyclerView()

        val rvTodo = view.rv_todo
        rvTodo.apply {
            layoutManager = StaggeredGridLayoutManager(2, GridLayoutManager.VERTICAL)
            adapter = listAdapter
        }
        mTodoViewModel.getAllData.observe(viewLifecycleOwner, Observer { data ->
           mSharedViewModel.checkIfDatabaseEmpty(data)
            listAdapter.setData(data)
        })
        


        view.fab.setOnClickListener {
            findNavController().navigate(R.id.action_listFragment_to_addFragment)
        }
        setHasOptionsMenu(true)

        return view
    }


    private fun swipToDelete(recyclerView: RecyclerView){
        val swipeToDeleteCallbacks = object : SwipeToDelete(){

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val deletedItem = listAdapter.dataList[viewHolder.adapterPosition]
                mTodoViewModel.deleteItem(deletedItem)
                listAdapter.notifyItemRemoved(viewHolder.adapterPosition)

                restoreDeletedData(viewHolder.itemView, deletedItem)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallbacks)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun setupRecyclerView() {
        val rvTodo = binding.rvTodo
        rvTodo.apply {
            layoutManager = StaggeredGridLayoutManager(2, GridLayoutManager.VERTICAL)
            adapter = listAdapter
            itemAnimator = LandingAnimator().apply {
                addDuration = 300
            }
        }
        swipToDelete(rvTodo)
    }

    private fun restoreDeletedData(view: View, deletedItem: ToDoData){
        val snackbar = Snackbar.make(view, "Deleted : '${deletedItem.title}'", Snackbar.LENGTH_LONG)
        snackbar.setAction("Undo"){
            mTodoViewModel.insertData(deletedItem)
        }
        snackbar.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun showEmptyDatabaseViews(emptyDatabase: Boolean) {
        if (emptyDatabase){
            img_no_data.visibility = View.VISIBLE
            tv_no_data.visibility = View.VISIBLE
        } else {
            img_no_data.visibility = View.INVISIBLE
            tv_no_data.visibility  = View.INVISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.list_fragment_menu, menu)

        val search = menu.findItem(R.id.menu_search)
        val searchView = search.actionView as? SearchView
        searchView?.isSubmitButtonEnabled = true
        searchView?.setOnQueryTextListener(this)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_delete_all ->confirmDelelteAllData()
            R.id.menu_priority_high -> mTodoViewModel.sortByHighPriority.observe(this, Observer {
                listAdapter.setData(it)
            })
            R.id.menu_priority_low -> mTodoViewModel.sortByLowPriority.observe(this, Observer {
                listAdapter.setData(it)
            })
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmDelelteAllData() {
        AlertDialog.Builder(requireContext())
                .setTitle("Delete Everything")
                .setMessage("Are you sure want to remove everything?")
                .setPositiveButton("Yes") {_,_ ->
                    mTodoViewModel.deleteAllData()
                    Toast.makeText(requireContext(), "Successfully Removed Everything",
                    Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .create()
                .show()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null){
            searchThroughDatabase(query)
        }
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query != null){
            searchThroughDatabase(query)
        }
        return true
    }

    private fun searchThroughDatabase(query: String) {
        val searchQuery = "%$query"

        mTodoViewModel.searchDatabase(searchQuery).observe(this, Observer { list ->
            list?.let {
                listAdapter.setData(it)
            }
        })
    }

}